package com.zhli.baymd.rag.testbase;

import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.framework.convention.ChatRequest;
import com.zhli.baymd.infra.chat.LLMService;
import com.zhli.baymd.infra.chat.StreamCallback;
import com.zhli.baymd.infra.chat.StreamCancellationHandle;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Mock LLM 服务 — 用于测试 RAG 管道和 Agent 循环，不依赖真实 LLM API。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * // 方式1：预设响应队列（按顺序消费）
 * MockLLMService mock = new MockLLMService();
 * mock.enqueueText("这是第一个回答");
 * mock.enqueueText("这是第二个回答");
 *
 * // 方式2：模式匹配（按 prompt 内容选择响应）
 * mock.addPattern(q -> q.contains("天气") ? "今天晴天" : null);
 *
 * // 方式3：工具调用（用于测试 ReAct 循环）
 * mock.enqueueToolCall("get_weather", "{\"city\":\"北京\"}");
 *
 * // 验证收到的请求
 * assertThat(mock.getSyncRequests()).hasSize(2);
 * assertThat(mock.getLastRequest().getMessages().get(0).getContent()).contains("天气");
 * }</pre>
 */
@Slf4j
public class MockLLMService implements LLMService {

    // ==================== 响应预设 ====================

    /** 预设文本响应队列（FIFO） */
    private final Queue<MockResponse> responseQueue = new ConcurrentLinkedQueue<>();

    /** 模式匹配器：对输入的 prompt/request 判断返回什么 */
    private final List<Function<ChatRequest, String>> patterns = new CopyOnWriteArrayList<>();

    /** 当队列和模式都不匹配时的默认响应 */
    @Setter
    private String defaultResponse = "这是 Mock 模型的默认回答。";

    // ==================== 请求记录 ====================

    @Getter
    private final List<ChatRequest> syncRequests = Collections.synchronizedList(new ArrayList<>());

    @Getter
    private final List<StreamRequestRecord> streamRequests = new CopyOnWriteArrayList<>();

    /** 所有流式回调的取消句柄，可通过 cancelAll() 统一取消 */
    @Getter
    private final List<MockCancellationHandle> activeHandles = new CopyOnWriteArrayList<>();

    // ==================== 延迟模拟 ====================

    /** 每次 chat/streamChat 调用的模拟延迟（毫秒） */
    @Setter
    private long simulatedDelayMs = 0;

    /** 流式输出时每个 chunk 之间的间隔（毫秒） */
    @Setter
    private long chunkDelayMs = 0;

    /** 流式输出时每个 chunk 的大小（字符数） */
    @Setter
    private int chunkSize = 5;

    // ==================== 错误注入 ====================

    private final AtomicInteger errorOnCallIndex = new AtomicInteger(-1);
    private final AtomicInteger callCounter = new AtomicInteger(0);
    private RuntimeException injectedError;

    /**
     * 在第 N 次调用时抛出异常（0-based）
     */
    public void injectErrorOnCall(int callIndex, RuntimeException error) {
        errorOnCallIndex.set(callIndex);
        this.injectedError = error;
    }

    // ==================== 预设方法 ====================

    /**
     * 往响应队列末尾添加一条纯文本响应
     */
    public MockLLMService enqueueText(String text) {
        responseQueue.offer(MockResponse.text(text));
        return this;
    }

    /**
     * 往响应队列末尾添加一条工具调用响应
     *
     * @param toolName  工具名称
     * @param arguments 工具参数 JSON 字符串
     */
    public MockLLMService enqueueToolCall(String toolName, String arguments) {
        responseQueue.offer(MockResponse.toolCall(toolName, arguments));
        return this;
    }

    /**
     * 往响应队列末尾添加一条"先思考再回答"的响应（用于测试 ReAct 中间步骤）
     */
    public MockLLMService enqueueThinkThenToolCall(String thought, String toolName, String arguments) {
        responseQueue.offer(MockResponse.thinkThenToolCall(thought, toolName, arguments));
        return this;
    }

    /**
     * 添加一个模式匹配器。当队列为空时，按注册顺序依次尝试匹配，
     * 第一个返回非 null 的作为响应。
     */
    public MockLLMService addPattern(Function<ChatRequest, String> pattern) {
        patterns.add(pattern);
        return this;
    }

    /**
     * 清空所有预设和记录
     */
    public void reset() {
        responseQueue.clear();
        patterns.clear();
        syncRequests.clear();
        streamRequests.clear();
        activeHandles.clear();
        callCounter.set(0);
        errorOnCallIndex.set(-1);
        injectedError = null;
        simulatedDelayMs = 0;
        chunkDelayMs = 0;
        defaultResponse = "这是 Mock 模型的默认回答。";
    }

    /**
     * 取消所有活跃的流式调用
     */
    public void cancelAll() {
        activeHandles.forEach(MockCancellationHandle::cancel);
    }

    // ==================== LLMService 实现 ====================

    @Override
    public String chat(ChatRequest request) {
        syncRequests.add(request);
        maybeInjectError();
        simulateDelay();

        String response = resolveResponse(request);

        log.debug("[MockLLM] 同步调用 → 响应长度={}", response != null ? response.length() : 0);
        return response;
    }

    @Override
    public String chat(ChatRequest request, String modelId) {
        // 记录 modelId，但 mock 忽略它
        syncRequests.add(request);
        maybeInjectError();
        simulateDelay();
        return resolveResponse(request);
    }

    @Override
    public StreamCancellationHandle streamChat(ChatRequest request, StreamCallback callback) {
        streamRequests.add(new StreamRequestRecord(request, callback));
        maybeInjectError();

        MockCancellationHandle handle = new MockCancellationHandle();
        activeHandles.add(handle);

        String fullResponse = resolveResponse(request);

        // 检查是否是工具调用格式 — 流式也支持
        MockResponse queued = responseQueue.peek();
        if (queued != null && queued.getType() == MockResponseType.TOOL_CALL) {
            // 工具调用在流式模式下仍然是"一次性"返回
            responseQueue.poll(); // 消费
            simulateStreaming(fullResponse, callback, handle);
        } else if (queued != null && queued.getType() == MockResponseType.THINK_THEN_TOOL_CALL) {
            responseQueue.poll();
            // 先流式输出思考内容
            if (queued.getThinkingContent() != null) {
                simulateStreaming(queued.getThinkingContent(), callback, handle, true);
            }
            // 然后以工具调用格式结束
            if (!handle.isCancelled()) {
                simulateDelay();
                callback.onContent(queued.toToolCallJson());
                callback.onComplete();
            }
        } else {
            simulateStreaming(fullResponse, callback, handle);
        }

        return handle;
    }

    // ==================== 内部方法 ====================

    private String resolveResponse(ChatRequest request) {
        // 优先消费预设队列
        MockResponse queued = responseQueue.poll();
        if (queued != null) {
            if (queued.getType() == MockResponseType.TEXT) {
                return queued.getContent();
            } else {
                // 工具调用以 JSON 格式返回
                return queued.toToolCallJson();
            }
        }

        // 其次尝试模式匹配
        for (Function<ChatRequest, String> pattern : patterns) {
            String matched = pattern.apply(request);
            if (matched != null) {
                return matched;
            }
        }

        // 最后使用默认响应
        return defaultResponse;
    }

    private void simulateStreaming(String fullResponse, StreamCallback callback, MockCancellationHandle handle) {
        simulateStreaming(fullResponse, callback, handle, false);
    }

    private void simulateStreaming(String fullResponse, StreamCallback callback,
                                    MockCancellationHandle handle, boolean isThinking) {
        if (fullResponse == null || fullResponse.isEmpty()) {
            if (!handle.isCancelled()) {
                callback.onComplete();
            }
            return;
        }

        int pos = 0;
        while (pos < fullResponse.length() && !handle.isCancelled()) {
            int end = Math.min(pos + chunkSize, fullResponse.length());
            String chunk = fullResponse.substring(pos, end);
            if (isThinking) {
                callback.onThinking(chunk);
            } else {
                callback.onContent(chunk);
            }
            pos = end;
            if (pos < fullResponse.length() && chunkDelayMs > 0) {
                try {
                    Thread.sleep(chunkDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        if (!handle.isCancelled()) {
            callback.onComplete();
        }
    }

    private void simulateDelay() {
        if (simulatedDelayMs > 0) {
            try {
                Thread.sleep(simulatedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void maybeInjectError() {
        int current = callCounter.getAndIncrement();
        if (current == errorOnCallIndex.get() && injectedError != null) {
            throw injectedError;
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取最后一次同步调用的请求
     */
    public ChatRequest getLastRequest() {
        if (syncRequests.isEmpty()) return null;
        return syncRequests.get(syncRequests.size() - 1);
    }

    /**
     * 查找最后一条用户消息的内容
     */
    public String getLastUserMessage() {
        ChatRequest req = getLastRequest();
        if (req == null) return null;
        return req.getMessages().stream()
                .filter(m -> m.getRole() == ChatMessage.Role.USER)
                .reduce((first, second) -> second) // 最后一条 USER
                .map(ChatMessage::getContent)
                .orElse(null);
    }

    // ==================== 内部类型 ====================

    public record StreamRequestRecord(ChatRequest request, StreamCallback callback) {}

    /**
     * 流式取消句柄的 mock 实现
     */
    public static class MockCancellationHandle implements StreamCancellationHandle {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public void cancel() {
            cancelled.set(true);
        }

        public boolean isCancelled() {
            return cancelled.get();
        }
    }

    /**
     * 响应类型枚举
     */
    public enum MockResponseType {
        TEXT,           // 纯文本
        TOOL_CALL,      // 工具调用
        THINK_THEN_TOOL_CALL, // 先思考再调工具
        ERROR           // 错误
    }
}
