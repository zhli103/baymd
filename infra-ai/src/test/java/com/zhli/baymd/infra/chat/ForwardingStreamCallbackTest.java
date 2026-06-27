package com.zhli.baymd.infra.chat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ForwardingStreamCallback 装饰器")
class ForwardingStreamCallbackTest {

    private final List<String> received = new ArrayList<>();
    private final TestCallback delegate = new TestCallback(received);

    // ==================== 透传 ====================

    @Test
    @DisplayName("onContent → 透传到 delegate")
    void forwardContent() {
        var cb = createCallback(delegate);
        cb.onContent("hello");
        assertThat(received).containsExactly("content:hello");
    }

    @Test
    @DisplayName("onThinking → 透传到 delegate")
    void forwardThinking() {
        var cb = createCallback(delegate);
        cb.onThinking("hmm...");
        assertThat(received).containsExactly("thinking:hmm...");
    }

    // ==================== onFirstContent ====================

    @Test
    @DisplayName("首个内容到达 → 触发 onFirstContent")
    void firstContentFiresHook() {
        var triggered = new AtomicBoolean(false);
        var cb = new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFirstContent() { triggered.set(true); }
            @Override
            protected void onFinish(boolean s, Throwable e) {}
        };

        cb.onContent("a");
        assertThat(triggered).isTrue();
    }

    @Test
    @DisplayName("多次 onContent → onFirstContent 只触发一次")
    void firstContentOnlyOnce() {
        var count = new int[]{0};
        var cb = new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFirstContent() { count[0]++; }
            @Override
            protected void onFinish(boolean s, Throwable e) {}
        };

        cb.onContent("a");
        cb.onContent("b");
        cb.onContent("c");
        assertThat(count[0]).isEqualTo(1);
        assertThat(received).containsExactly("content:a", "content:b", "content:c");
    }

    // ==================== onFinish ====================

    @Test
    @DisplayName("onComplete → delegate.onComplete + onFinish(success)")
    void completeTriggersFinish() {
        var success = new AtomicBoolean(false);
        var cb = new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFinish(boolean s, Throwable e) {
                success.set(s);
            }
        };

        cb.onComplete();
        assertThat(success).isTrue();
        assertThat(received).contains("complete");
    }

    @Test
    @DisplayName("onError → delegate.onError + onFinish(fail)")
    void errorTriggersFinish() {
        var success = new boolean[]{true};
        var error = new Throwable[]{null};
        var cb = new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFinish(boolean s, Throwable e) {
                success[0] = s;
                error[0] = e;
            }
        };

        var ex = new RuntimeException("test error");
        cb.onError(ex);
        assertThat(success[0]).isFalse();
        assertThat(error[0]).isEqualTo(ex);
        assertThat(received).contains("error");
    }

    @Test
    @DisplayName("onComplete 后 onError → onFinish 只触发一次")
    void finishOnlyOnce() {
        var count = new int[]{0};
        var cb = new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFinish(boolean s, Throwable e) { count[0]++; }
        };

        cb.onComplete();
        cb.onError(new RuntimeException());
        assertThat(count[0]).isEqualTo(1);
    }

    // ==================== helper ====================

    private static ForwardingStreamCallback createCallback(StreamCallback delegate) {
        return new ForwardingStreamCallback(delegate) {
            @Override
            protected void onFinish(boolean success, Throwable error) {}
        };
    }

    private static class TestCallback implements StreamCallback {
        private final List<String> log;
        TestCallback(List<String> log) { this.log = log; }
        public void onContent(String c) { log.add("content:" + c); }
        public void onThinking(String c) { log.add("thinking:" + c); }
        public void onComplete() { log.add("complete"); }
        public void onError(Throwable e) { log.add("error"); }
    }
}
