package com.zhli.baymd.rag.core.memory;

import cn.hutool.core.collection.CollUtil;
import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.rag.config.MemoryProperties;
import com.zhli.baymd.rag.core.memory.evolution.ProfileGenerationService;
import com.zhli.baymd.rag.core.memory.retrieve.MemoryInjector;
import com.zhli.baymd.rag.core.memory.retrieve.MemoryRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 三种记忆策略实现。
 */
@Slf4j
public final class MemoryStrategies {

    private MemoryStrategies() {}

    // ==================== 无记忆 ====================

    @RequiredArgsConstructor
    public static class NoMemory implements MemoryStrategy {
        @Override public String getName() { return "none"; }
        @Override public boolean isEnabled() { return true; }
        @Override
        public List<ChatMessage> loadHistory(String conversationId, String userId, ChatMessage currentMessage) {
            log.debug("无记忆策略: 返回空历史");
            return List.of();
        }
    }

    // ==================== 滑动窗口 ====================

    @RequiredArgsConstructor
    public static class SlidingWindow implements MemoryStrategy {
        private final ConversationMemoryStore store;
        private final MemoryProperties props;

        @Override public String getName() { return "sliding_window"; }
        @Override public boolean isEnabled() { return true; }
        @Override
        public List<ChatMessage> loadHistory(String conversationId, String userId, ChatMessage currentMessage) {
            int turns = props.getHistoryKeepTurns();
            List<ChatMessage> all = store.loadHistory(conversationId, userId);
            if (CollUtil.isEmpty(all)) return List.of();
            int fromIndex = Math.max(0, all.size() - turns * 2);
            List<ChatMessage> window = new ArrayList<>(all.subList(fromIndex, all.size()));
            log.debug("滑动窗口记忆: {} 条消息 (总{}条, 保留{}轮)", window.size(), all.size(), turns);
            return window;
        }
    }

    // ==================== 摘要压缩 ====================

    @RequiredArgsConstructor
    public static class SummaryCompression implements MemoryStrategy {
        private final ConversationMemoryStore store;
        private final ConversationMemorySummaryService summaryService;
        private final MemoryProperties props;

        @Override public String getName() { return "summary_compression"; }
        @Override public boolean isEnabled() { return props.getSummaryEnabled(); }
        @Override
        public List<ChatMessage> loadHistory(String conversationId, String userId, ChatMessage currentMessage) {
            int keepTurns = props.getHistoryKeepTurns();
            int summaryStart = props.getSummaryStartTurns();
            int maxMessages = keepTurns * 2;

            List<ChatMessage> all = store.loadHistory(conversationId, userId);
            if (CollUtil.isEmpty(all)) return List.of();

            List<ChatMessage> result = new ArrayList<>();

            if (all.size() > summaryStart * 2) {
                ChatMessage summary = summaryService.loadLatestSummary(conversationId, userId);
                if (summary != null) {
                    result.add(summary);
                }
                int fromIndex = Math.max(0, all.size() - maxMessages);
                result.addAll(all.subList(fromIndex, all.size()));
                log.debug("摘要压缩记忆: 摘要 + {} 条最近 (总{}条)", all.size() - fromIndex, all.size());
            } else {
                int fromIndex = Math.max(0, all.size() - maxMessages);
                result.addAll(all.subList(fromIndex, all.size()));
                log.debug("摘要压缩记忆: {} 条 (未触发摘要, 总{}条)", result.size(), all.size());
            }
            return result;
        }
    }

    // ==================== 语义记忆 ====================

    /** 语义记忆：从历史对话中提取结构化事实，用向量检索召回相关记忆注入 Prompt。 */
    @RequiredArgsConstructor
    public static class SemanticMemory implements MemoryStrategy {
        private final MemoryRetrievalService retrievalService;
        private final MemoryInjector injector;
        private final ProfileGenerationService profileService;

        @Override public String getName() { return "semantic"; }
        @Override public boolean isEnabled() { return true; }
        @Override
        public List<ChatMessage> loadHistory(String conversationId, String userId, ChatMessage currentMessage) {
            String question = currentMessage != null ? currentMessage.getContent() : "";
            if (question.isBlank()) return List.of();

            List<ChatMessage> result = new ArrayList<>();

            // 1. 尝试加载用户画像（累积30+ Fact后生成）
            var profile = profileService.generateIfNeeded(userId);
            if (profile != null) {
                result.add(ChatMessage.system(profile.toPromptFragment()));
            }

            // 2. 语义检索相关 Facts + Episodes
            var memory = retrievalService.retrieve(question, userId);
            if (!memory.isEmpty()) {
                String prompt = injector.buildPromptFragment(memory);
                if (!prompt.isBlank()) {
                    result.add(ChatMessage.system(prompt));
                }
            }

            log.debug("语义记忆: userId={}, profile={}, facts={}, episodes={}",
                    userId, profile != null, memory.getFacts().size(), memory.getEpisodes().size());

            return result;
        }
    }
}
