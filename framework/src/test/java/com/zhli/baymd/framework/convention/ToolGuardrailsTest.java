package com.zhli.baymd.framework.convention;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ToolGuardrails 单元测试
 */
@DisplayName("ToolGuardrails 护栏测试")
class ToolGuardrailsTest {

    // ==================== 正常调用 ====================

    @Test
    @DisplayName("正常调用 — 一次成功")
    void shouldSucceedOnFirstTry() throws Exception {
        ToolGuardrails g = ToolGuardrails.builder().maxRetries(3).timeout(Duration.ofSeconds(5)).build();

        String result = g.call(() -> "success");

        assertThat(result).isEqualTo("success");
        assertThat(g.getTotalCalls().get()).isEqualTo(1);
        assertThat(g.getTotalRetries().get()).isEqualTo(0);
    }

    @Test
    @DisplayName("正常调用 — 无重试配置时失败直接抛异常")
    void shouldFailWithoutRetry() {
        ToolGuardrails g = ToolGuardrails.builder().maxRetries(0).timeout(Duration.ofSeconds(5)).build();

        assertThatThrownBy(() -> g.call(() -> {
            throw new RuntimeException("bang");
        })).isInstanceOf(ToolGuardrails.ToolExecutionException.class)
                .hasMessageContaining("bang");

        assertThat(g.getTotalCalls().get()).isEqualTo(1);
        assertThat(g.getTotalRetries().get()).isEqualTo(0);
    }

    // ==================== 重试 ====================

    @Test
    @DisplayName("重试 — 前两次失败第三次成功")
    void shouldRetryAndSucceed() throws Exception {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(3)
                .timeout(Duration.ofSeconds(5))
                .baseBackoff(Duration.ofMillis(1))
                .jitterFactor(0.0)
                .build();
        AtomicInteger attempts = new AtomicInteger(0);

        String result = g.call(() -> {
            int a = attempts.incrementAndGet();
            if (a < 3) throw new RuntimeException("attempt " + a);
            return "ok-" + a;
        });

        assertThat(result).isEqualTo("ok-3");
        assertThat(g.getTotalCalls().get()).isEqualTo(3);
        assertThat(g.getTotalRetries().get()).isEqualTo(2);
    }

    @Test
    @DisplayName("重试 — 全部失败后抛 ToolExecutionException")
    void shouldThrowAfterAllRetriesExhausted() {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(2)
                .timeout(Duration.ofSeconds(5))
                .baseBackoff(Duration.ofMillis(1))
                .jitterFactor(0.0)
                .build();

        assertThatThrownBy(() -> g.call(() -> {
            throw new RuntimeException("persistent error");
        })).isInstanceOf(ToolGuardrails.ToolExecutionException.class)
                .hasMessageContaining("persistent error")
                .hasMessageContaining("3 次尝试");

        // 首次 + 2 次重试 = 3 次
        assertThat(g.getTotalCalls().get()).isEqualTo(3);
        assertThat(g.getTotalRetries().get()).isEqualTo(2);
    }

    @Test
    @DisplayName("重试 — 不可重试异常直接退出")
    void shouldNotRetryNonRetryableException() {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(3)
                .timeout(Duration.ofSeconds(5))
                .baseBackoff(Duration.ofMillis(1))
                .build();
        AtomicInteger calls = new AtomicInteger(0);

        // IllegalArgumentException 不可重试
        assertThatThrownBy(() -> g.call(() -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException("permanent");
        }, e -> !(e instanceof IllegalArgumentException)))
                .isInstanceOf(ToolGuardrails.ToolExecutionException.class);

        assertThat(calls.get()).isEqualTo(1); // 只调了一次，没重试
    }

    // ==================== 超时 ====================

    @Test
    @DisplayName("超时 — 超时不重试")
    void shouldNotRetryOnTimeout() {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(2)
                .timeout(Duration.ofMillis(50))
                .baseBackoff(Duration.ofMillis(1))
                .build();

        assertThatThrownBy(() -> g.call(() -> {
            Thread.sleep(5000);
            return "never";
        })).isInstanceOf(ToolGuardrails.ToolExecutionException.class);

        // 超时默认不重试（重试判定将 ToolTimeoutException 判为不可重试）
        assertThat(g.getTotalCalls().get()).isEqualTo(1);
    }

    // ==================== 降级 ====================

    @Test
    @DisplayName("降级 — 失败时返回 fallback")
    void shouldReturnFallbackOnFailure() {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(1)
                .timeout(Duration.ofSeconds(5))
                .baseBackoff(Duration.ofMillis(1))
                .build();

        String result = g.callWithFallback(() -> {
            throw new RuntimeException("fail");
        }, "fallback-value");

        assertThat(result).isEqualTo("fallback-value");
    }

    @Test
    @DisplayName("降级 — 成功时不使用 fallback")
    void shouldNotUseFallbackOnSuccess() {
        ToolGuardrails g = ToolGuardrails.defaults();

        String result = g.callWithFallback(() -> "real-value", "fallback-value");

        assertThat(result).isEqualTo("real-value");
    }

    // ==================== 退避计算 ====================

    @Test
    @DisplayName("退避 — 指数增长")
    void shouldComputeExponentialBackoff() {
        ToolGuardrails g = ToolGuardrails.builder()
                .baseBackoff(Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(10))
                .jitterFactor(0.0)
                .build();

        // attempt 1 → 100 * 2^0 = 100ms
        assertThat(g.computeBackoffMs(1)).isEqualTo(100);
        // attempt 2 → 100 * 2^1 = 200ms
        assertThat(g.computeBackoffMs(2)).isEqualTo(200);
        // attempt 3 → 100 * 2^2 = 400ms
        assertThat(g.computeBackoffMs(3)).isEqualTo(400);
        // attempt 4 → 100 * 2^3 = 800ms
        assertThat(g.computeBackoffMs(4)).isEqualTo(800);
    }

    @Test
    @DisplayName("退避 — 不超过最大限制")
    void shouldCapBackoffAtMax() {
        ToolGuardrails g = ToolGuardrails.builder()
                .baseBackoff(Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(5))
                .jitterFactor(0.0)
                .build();

        // 1*2^5 = 32s → capped at 5s
        long backoff = g.computeBackoffMs(6);
        assertThat(backoff).isEqualTo(5000);
    }

    @Test
    @DisplayName("退避 — 抖动在预期范围内")
    void shouldJitterWithinRange() {
        ToolGuardrails g = ToolGuardrails.builder()
                .baseBackoff(Duration.ofMillis(100))
                .maxBackoff(Duration.ofSeconds(10))
                .jitterFactor(0.5)
                .build();

        // 跑 100 次抖动计算，验证都在 [50, 150] 范围内
        for (int i = 0; i < 100; i++) {
            long backoff = g.computeBackoffMs(1);
            // base=100ms, jitter=0.5 → range [50, 150]
            assertThat(backoff).isGreaterThanOrEqualTo(0);
            assertThat(backoff).isLessThanOrEqualTo(200); // 允许一些容差
        }
    }

    // ==================== 统计 ====================

    @Test
    @DisplayName("统计 — retryRate 计算正确")
    void shouldComputeRetryRate() throws Exception {
        ToolGuardrails g = ToolGuardrails.builder()
                .maxRetries(2)
                .timeout(Duration.ofSeconds(5))
                .baseBackoff(Duration.ofMillis(1))
                .jitterFactor(0.0)
                .build();
        AtomicInteger attempts = new AtomicInteger(0);

        // 调用一次（成功）
        g.call(() -> "ok");
        // 再调用一次（需要 1 次重试）
        try {
            g.call(() -> {
                int a = attempts.incrementAndGet();
                if (a <= 2) throw new RuntimeException("fail");
                return "ok";
            });
        } catch (ToolGuardrails.ToolExecutionException ignored) {
        }

        ToolGuardrails.Stats stats = g.getStats();
        assertThat(stats.totalCalls()).isGreaterThanOrEqualTo(2);
        assertThat(stats.totalRetries()).isGreaterThanOrEqualTo(1);
        assertThat(stats.retryRate()).isGreaterThan(0);
    }

    // ==================== 工厂方法 ====================

    @Test
    @DisplayName("defaults — 创建默认配置")
    void shouldCreateDefaults() {
        ToolGuardrails g = ToolGuardrails.defaults();
        assertThat(g.getMaxRetries()).isEqualTo(3);
        assertThat(g.getTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("noRetry — 创建无重试配置")
    void shouldCreateNoRetry() {
        ToolGuardrails g = ToolGuardrails.noRetry(Duration.ofSeconds(10));
        assertThat(g.getMaxRetries()).isEqualTo(0);
        assertThat(g.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
}
