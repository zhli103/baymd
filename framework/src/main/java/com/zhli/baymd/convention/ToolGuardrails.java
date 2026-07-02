package com.zhli.baymd.framework.convention;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * 工具调用护栏 — 为任意工具执行添加重试、超时、断路器。
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>指数退避 + 随机抖动重试</li>
 *   <li>单次调用超时</li>
 *   <li>可重试异常判定（区分瞬时错误和永久错误）</li>
 *   <li>调用计数追踪</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * ToolGuardrails guardrails = ToolGuardrails.builder()
 *         .maxRetries(3)
 *         .timeout(Duration.ofSeconds(30))
 *         .build();
 *
 * String result = guardrails.call(() -> remoteService.fetchData());
 * // 或异步：
 * CompletableFuture<String> future = guardrails.callAsync(() -> remoteService.fetchData());
 * }</pre>
 */
@Slf4j
public class ToolGuardrails {

    /** 最大重试次数（不含首次调用） */
    @Getter
    private final int maxRetries;

    /** 单次调用超时 */
    @Getter
    private final Duration timeout;

    /** 基础退避时间 */
    private final Duration baseBackoff;

    /** 最大退避时间（避免无限增长） */
    private final Duration maxBackoff;

    /** 抖动因子 (0.0 ~ 1.0)，0 表示无抖动 */
    private final double jitterFactor;

    /** 调用计数器 */
    @Getter
    private final AtomicInteger totalCalls = new AtomicInteger(0);

    /** 重试计数器 */
    @Getter
    private final AtomicInteger totalRetries = new AtomicInteger(0);

    // ==================== 构造器 ====================

    @Builder
    public ToolGuardrails(int maxRetries, Duration timeout, Duration baseBackoff,
                          Duration maxBackoff, Double jitterFactor) {
        this.maxRetries = Math.max(0, maxRetries);
        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
        this.baseBackoff = baseBackoff != null ? baseBackoff : Duration.ofMillis(100);
        this.maxBackoff = maxBackoff != null ? maxBackoff : Duration.ofSeconds(10);
        this.jitterFactor = jitterFactor != null ? Math.min(1.0, Math.max(0.0, jitterFactor)) : 0.3;
    }

    /**
     * 创建默认护栏（重试 3 次，超时 30s）
     */
    public static ToolGuardrails defaults() {
        return builder().maxRetries(3).timeout(Duration.ofSeconds(30)).build();
    }

    /**
     * 创建无重试护栏（仅超时保护）
     */
    public static ToolGuardrails noRetry(Duration timeout) {
        return builder().maxRetries(0).timeout(timeout).build();
    }

    // ==================== 执行方法 ====================

    /**
     * 同步调用，带重试和超时
     *
     * @param callable 要执行的操作
     * @param <T>      返回值类型
     * @return 执行结果
     * @throws ToolExecutionException 所有重试耗尽后抛出
     */
    public <T> T call(Callable<T> callable) throws ToolExecutionException {
        // 默认：超时不重试，其他异常重试
        return call(callable, e -> !(e instanceof ToolTimeoutException));
    }

    /**
     * 同步调用，可指定哪些异常触发重试
     *
     * @param callable     要执行的操作
     * @param retryOn      重试判定条件（返回 true = 重试）
     * @param <T>          返回值类型
     * @return 执行结果
     * @throws ToolExecutionException 所有重试耗尽后抛出
     */
    public <T> T call(Callable<T> callable, Predicate<Exception> retryOn) throws ToolExecutionException {
        Exception lastException = null;
        int attempt = 0;

        while (attempt <= maxRetries) {
            totalCalls.incrementAndGet();
            try {
                if (attempt > 0) {
                    totalRetries.incrementAndGet();
                    long delayMs = computeBackoffMs(attempt);
                    log.info("工具调用重试, attempt={}/{}, delay={}ms", attempt, maxRetries, delayMs);
                    Thread.sleep(delayMs);
                }

                // 超时执行
                if (Duration.ZERO.equals(timeout) || timeout.toMillis() <= 0) {
                    return callable.call();
                }

                CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return callable.call();
                    } catch (Exception e) {
                        throw new CallableWrappedException(e);
                    }
                });

                return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);

            } catch (TimeoutException e) {
                lastException = e;
                log.warn("工具调用超时, attempt={}/{}, timeout={}ms",
                        attempt, maxRetries, timeout.toMillis());
                if (!retryOn.test(new ToolTimeoutException("调用超时: " + timeout.toMillis() + "ms"))) {
                    break;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ToolExecutionException("工具调用被中断", e, attempt);
            } catch (Exception e) {
                // 解包异常链：ExecutionException → CallableWrappedException → 原始异常
                //         或直接 RuntimeException → 原始异常
                Throwable root = unwrapRootCause(e);
                lastException = root instanceof Exception ex ? ex
                        : new RuntimeException(root != null ? root.getMessage() : e.getMessage());

                log.warn("工具调用失败, attempt={}/{}, error={}",
                        attempt, maxRetries,
                        lastException.getMessage() != null
                                ? lastException.getMessage()
                                : lastException.getClass().getSimpleName());

                if (!retryOn.test(lastException)) {
                    break; // 不可重试异常，直接退出
                }
            }
            attempt++;
        }

        String msg = lastException != null ? lastException.getMessage() : "未知错误";
        throw new ToolExecutionException(
                String.format("工具调用在 %d 次尝试后失败: %s", attempt, msg),
                lastException, attempt);
    }

    /**
     * 解包异常链：ExecutionException → RuntimeException/CallableWrappedException → 原始异常
     */
    private static Throwable unwrapRootCause(Throwable e) {
        Throwable current = e;
        // 剥离 ExecutionException 和 RuntimeException 包装层
        while (current != null && current != current.getCause()) {
            if (current instanceof java.util.concurrent.ExecutionException
                    || current instanceof CallableWrappedException
                    || (current instanceof RuntimeException
                        && current.getMessage() != null
                        && current.getMessage().startsWith("com.zhli.baymd"))) {
                current = current.getCause();
            } else if (current instanceof RuntimeException && current.getCause() != null
                    && current.getCause() != current) {
                // 一层 RuntimeException 包装（来自 CompletableFuture.supplyAsync）
                current = current.getCause();
            } else {
                break;
            }
        }
        return current != null ? current : e;
    }

    /**
     * 异步调用（返回 CompletableFuture，内部执行重试）
     */
    public <T> CompletableFuture<T> callAsync(Callable<T> callable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return call(callable);
            } catch (ToolExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 带降级值的调用 — 失败时返回 fallback 而非抛异常
     */
    public <T> T callWithFallback(Callable<T> callable, T fallback) {
        return callWithFallback(callable, e -> true, fallback);
    }

    /**
     * 带降级值的调用（可指定重试条件）
     */
    public <T> T callWithFallback(Callable<T> callable, Predicate<Exception> retryOn, T fallback) {
        try {
            return call(callable, retryOn);
        } catch (ToolExecutionException e) {
            log.warn("工具调用失败，使用降级值: {}", e.getMessage());
            return fallback;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 计算第 N 次重试的退避时间（指数退避 + 抖动）
     */
    long computeBackoffMs(int attemptNumber) {
        // 指数增长：baseBackoff * 2^(attempt-1)
        long exponential = baseBackoff.toMillis() * (1L << (attemptNumber - 1));
        long capped = Math.min(exponential, maxBackoff.toMillis());

        if (jitterFactor <= 0) {
            return capped;
        }

        // 随机抖动：[capped * (1 - jitter), capped * (1 + jitter)]
        long jitterRange = (long) (capped * jitterFactor);
        long jitter = (long) (Math.random() * 2 * jitterRange) - jitterRange;
        return Math.max(0, capped + jitter);
    }

    /**
     * 获取统计快照
     */
    public Stats getStats() {
        return new Stats(totalCalls.get(), totalRetries.get(), maxRetries, timeout);
    }

    // ==================== 内部类型 ====================

    public record Stats(int totalCalls, int totalRetries, int maxRetries, Duration timeout) {
        public double retryRate() {
            return totalCalls > 0 ? (double) totalRetries / totalCalls : 0.0;
        }

        @Override
        public String toString() {
            return String.format("ToolGuardrails.Stats{calls=%d, retries=%d, rate=%.2f%%, timeout=%dms}",
                    totalCalls, totalRetries, retryRate() * 100, timeout.toMillis());
        }
    }

    /**
     * 工具执行异常（包含尝试次数）
     */
    public static class ToolExecutionException extends RuntimeException {
        @Getter
        private final int attempts;

        public ToolExecutionException(String message, Throwable cause, int attempts) {
            super(message, cause);
            this.attempts = attempts;
        }
    }

    /**
     * 工具超时异常
     */
    public static class ToolTimeoutException extends Exception {
        public ToolTimeoutException(String message) {
            super(message);
        }
    }

    /**
     * 内部包装异常 — 用于在 CompletableFuture 中传递 callable 的原始异常
     */
    static class CallableWrappedException extends RuntimeException {
        CallableWrappedException(Exception cause) {
            super(cause);
        }
    }
}
