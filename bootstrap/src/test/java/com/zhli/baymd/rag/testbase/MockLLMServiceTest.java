package com.zhli.baymd.rag.testbase;

import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.framework.convention.ChatRequest;
import com.zhli.baymd.infra.chat.StreamCallback;
import com.zhli.baymd.infra.chat.StreamCancellationHandle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MockLLMService 自身功能的单元测试 — 验证 mock 的各种预设和记录能力。
 */
@DisplayName("MockLLMService 单元测试")
class MockLLMServiceTest {

    private final MockLLMService mock = new MockLLMService();

    @AfterEach
    void tearDown() {
        mock.reset();
    }

    // ==================== 同步调用 ====================

    @Test
    @DisplayName("同步调用 — 预设文本响应")
    void shouldReturnPresetTextResponse() {
        mock.enqueueText("这是预设回答");

        String result = mock.chat("随便问什么");

        assertThat(result).isEqualTo("这是预设回答");
        assertThat(mock.getSyncRequests()).hasSize(1);
    }

    @Test
    @DisplayName("同步调用 — 多轮预设依次消费")
    void shouldConsumeQueueInOrder() {
        mock.enqueueText("第一轮");
        mock.enqueueText("第二轮");
        mock.enqueueText("第三轮");

        assertThat(mock.chat("问1")).isEqualTo("第一轮");
        assertThat(mock.chat("问2")).isEqualTo("第二轮");
        assertThat(mock.chat("问3")).isEqualTo("第三轮");
        assertThat(mock.getSyncRequests()).hasSize(3);
    }

    @Test
    @DisplayName("同步调用 — 队列空时返回默认响应")
    void shouldReturnDefaultWhenQueueEmpty() {
        mock.setDefaultResponse("默认兜底回答");

        String result = mock.chat("随便问");

        assertThat(result).isEqualTo("默认兜底回答");
    }

    @Test
    @DisplayName("同步调用 — 模式匹配")
    void shouldMatchByPattern() {
        mock.addPattern(req -> {
            String msg = req.getMessages().get(req.getMessages().size() - 1).getContent();
            return msg.contains("天气") ? "今天晴天" : null;
        });
        mock.addPattern(req -> "万能兜底");

        assertThat(mock.chat("今天天气怎么样")).isEqualTo("今天晴天");
        assertThat(mock.chat("你好")).isEqualTo("万能兜底");
    }

    @Test
    @DisplayName("同步调用 — 记录所有请求")
    void shouldRecordAllSyncRequests() {
        mock.enqueueText("A");
        mock.enqueueText("B");

        mock.chat(TestFixtures.simpleRequest("问题1"));
        mock.chat(TestFixtures.simpleRequest("问题2"));

        assertThat(mock.getSyncRequests()).hasSize(2);
        assertThat(mock.getLastUserMessage()).isEqualTo("问题2");
    }

    // ==================== 流式调用 ====================

    @Test
    @DisplayName("流式调用 — 分段推送内容")
    void shouldStreamContentInChunks() throws Exception {
        mock.setChunkSize(3); // 每块3个字符
        mock.enqueueText("ABCDEFGHI"); // 9个字符 → 3块

        CountDownLatch completeLatch = new CountDownLatch(1);
        StringBuilder received = new StringBuilder();
        AtomicInteger chunkCount = new AtomicInteger();

        mock.streamChat(
                ChatRequest.builder().messages(List.of(ChatMessage.user("hi"))).build(),
                new StreamCallback() {
                    @Override
                    public void onContent(String content) {
                        received.append(content);
                        chunkCount.incrementAndGet();
                    }

                    @Override
                    public void onComplete() {
                        completeLatch.countDown();
                    }

                    @Override
                    public void onError(Throwable error) {
                    }
                }
        );

        assertThat(completeLatch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(received.toString()).isEqualTo("ABCDEFGHI");
        assertThat(chunkCount.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("流式调用 — 支持取消")
    void shouldSupportCancellation() throws Exception {
        mock.setChunkSize(1);
        mock.setChunkDelayMs(20);
        mock.enqueueText("这是一段很长的文本需要被中途取消掉");

        CountDownLatch completeLatch = new CountDownLatch(1);
        AtomicReference<String> partialContent = new AtomicReference<>("");

        // 在后台线程启动流式调用，避免主线程被阻塞
        Thread streamThread = new Thread(() -> {
            mock.streamChat(
                    ChatRequest.builder().messages(List.of(ChatMessage.user("hi"))).build(),
                    new StreamCallback() {
                        @Override
                        public void onContent(String content) {
                            partialContent.updateAndGet(s -> s + content);
                        }

                        @Override
                        public void onComplete() {
                            completeLatch.countDown();
                        }

                        @Override
                        public void onError(Throwable error) {
                        }
                    }
            );
        });
        streamThread.start();

        // 等一小段时间让部分 chunk 发出，然后取消所有活跃的流
        Thread.sleep(50);
        mock.cancelAll();
        streamThread.join(2000);

        // 应该只收到部分内容（被取消时未完成全部推送）
        String received = partialContent.get();
        assertThat(received.length()).isLessThan("这是一段很长的文本需要被中途取消掉".length());
    }

    // ==================== 工具调用 ====================

    @Test
    @DisplayName("同步调用 — 工具调用格式响应")
    void shouldReturnToolCallJson() {
        mock.enqueueToolCall("get_weather", "{\"city\":\"北京\"}");

        String result = mock.chat("今天天气怎么样");

        assertThat(result).contains("tool_calls");
        assertThat(result).contains("get_weather");
        assertThat(result).contains("北京");
    }

    @Test
    @DisplayName("同步调用 — 先思考再工具调用")
    void shouldReturnThinkThenToolCall() {
        mock.enqueueThinkThenToolCall("用户想知道天气", "get_weather", "{\"city\":\"上海\"}");

        String result = mock.chat("上海天气");

        assertThat(result).contains("tool_calls");
        assertThat(result).contains("get_weather");
    }

    // ==================== 错误注入 ====================

    @Test
    @DisplayName("错误注入 — 在第N次调用时抛出异常")
    void shouldInjectErrorOnSpecificCall() {
        mock.injectErrorOnCall(1, new RuntimeException("模拟网络错误"));

        mock.enqueueText("第一次OK");

        assertThat(mock.chat("第一次")).isEqualTo("第一次OK");

        assertThatThrownBy(() -> mock.chat("第二次"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("模拟网络错误");
    }

    // ==================== 工具方法 ====================

    @Test
    @DisplayName("getLastUserMessage — 返回最后一条用户消息")
    void shouldReturnLastUserMessage() {
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system("系统提示"),
                        ChatMessage.user("第一个问题"),
                        ChatMessage.assistant("第一个回答"),
                        ChatMessage.user("第二个问题")
                ))
                .build();

        mock.enqueueText("回答");
        mock.chat(req);

        assertThat(mock.getLastUserMessage()).isEqualTo("第二个问题");
    }

    @Test
    @DisplayName("reset — 清空所有状态")
    void shouldResetAllState() {
        mock.enqueueText("test");
        mock.chat("hello");
        mock.setSimulatedDelayMs(1000);

        mock.reset();

        assertThat(mock.getSyncRequests()).isEmpty();
        assertThat(mock.getStreamRequests()).isEmpty();
        // 重置后应该恢复默认
        String result = mock.chat("hello again");
        assertThat(result).isEqualTo("这是 Mock 模型的默认回答。");
    }
}
