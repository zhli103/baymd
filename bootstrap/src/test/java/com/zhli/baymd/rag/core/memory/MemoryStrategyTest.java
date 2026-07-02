package com.zhli.baymd.rag.core.memory;

import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.rag.config.MemoryProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("记忆策略测试")
class MemoryStrategyTest {

    // ==================== NoMemory ====================

    @Test
    @DisplayName("无记忆 — 始终返回空列表")
    void shouldReturnEmptyForNoMemory() {
        MemoryStrategy strategy = new MemoryStrategies.NoMemory();
        List<ChatMessage> result = strategy.loadHistory("conv1", "user1", null);
        assertThat(result).isEmpty();
        assertThat(strategy.getName()).isEqualTo("none");
        assertThat(strategy.isEnabled()).isTrue();
    }

    // ==================== SlidingWindow ====================

    @Test
    @DisplayName("滑动窗口 — 返回最近N条消息")
    void shouldReturnRecentMessages() {
        FakeStore store = new FakeStore();
        for (int i = 1; i <= 20; i++) {
            store.messages.add(ChatMessage.user("msg" + i));
        }

        MemoryProperties props = new MemoryProperties();
        props.setHistoryKeepTurns(3); // 3 轮 = 6 条
        MemoryStrategy strategy = new MemoryStrategies.SlidingWindow(store, props);

        List<ChatMessage> result = strategy.loadHistory("c", "u", null);
        assertThat(result).hasSize(6);
        assertThat(result.get(0).getContent()).isEqualTo("msg15");
        assertThat(result.get(5).getContent()).isEqualTo("msg20");
    }

    // ==================== SummaryCompression ====================

    @Test
    @DisplayName("摘要压缩 — 未超阈值时等同滑动窗口")
    void shouldUseWindowWhenUnderThreshold() {
        FakeStore store = new FakeStore();
        for (int i = 1; i <= 10; i++) store.messages.add(ChatMessage.user("m" + i));

        MemoryProperties props = new MemoryProperties();
        props.setHistoryKeepTurns(3);
        props.setSummaryStartTurns(9);

        FakeSummaryService noSummary = new FakeSummaryService(null);
        MemoryStrategy strategy = new MemoryStrategies.SummaryCompression(
                store, noSummary, props);

        List<ChatMessage> result = strategy.loadHistory("c", "u", null);
        assertThat(result).hasSize(6);
    }

    @Test
    @DisplayName("摘要压缩 — 超阈值时包含摘要")
    void shouldIncludeSummaryWhenOverThreshold() {
        FakeStore store = new FakeStore();
        for (int i = 1; i <= 30; i++) store.messages.add(ChatMessage.user("m" + i));

        MemoryProperties props = new MemoryProperties();
        props.setHistoryKeepTurns(3);
        props.setSummaryStartTurns(5);     // 10条 > 5轮×2 → 触发摘要

        // summary 返回一个 system 消息
        FakeSummaryService summarySvc = new FakeSummaryService(
                ChatMessage.system("摘要内容"));

        MemoryStrategy strategy = new MemoryStrategies.SummaryCompression(
                store, summarySvc, props);

        List<ChatMessage> result = strategy.loadHistory("c", "u", null);
        assertThat(result.get(0).getRole()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(result.get(0).getContent()).contains("摘要内容");
    }

    @Test
    @DisplayName("摘要压缩 — 空历史返回空列表")
    void shouldReturnEmptyForEmptyHistory() {
        FakeStore store = new FakeStore();
        MemoryProperties props = new MemoryProperties();
        MemoryStrategy strategy = new MemoryStrategies.SummaryCompression(
                store, new FakeSummaryService(null), props);

        List<ChatMessage> result = strategy.loadHistory("c", "u", null);
        assertThat(result).isEmpty();
    }

    // ==================== Fake 实现 ====================

    static class FakeStore implements ConversationMemoryStore {
        final java.util.ArrayList<ChatMessage> messages = new java.util.ArrayList<>();

        @Override public List<ChatMessage> loadHistory(String convId, String userId) {
            return List.copyOf(messages);
        }
        @Override public String append(String convId, String userId, ChatMessage msg) {
            messages.add(msg); return String.valueOf(messages.size());
        }
        @Override public void refreshCache(String convId, String userId) {}
    }

    static class FakeSummaryService implements ConversationMemorySummaryService {
        private final ChatMessage summary;
        FakeSummaryService(ChatMessage summary) { this.summary = summary; }
        @Override
        public void compressIfNeeded(String convId, String userId, ChatMessage msg) {}
        @Override
        public ChatMessage loadLatestSummary(String convId, String userId) { return summary; }
        @Override
        public ChatMessage decorateIfNeeded(ChatMessage summary) { return summary; }
    }
}
