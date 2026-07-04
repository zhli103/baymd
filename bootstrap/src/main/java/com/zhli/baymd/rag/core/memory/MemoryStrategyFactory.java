package com.zhli.baymd.rag.core.memory;

import com.zhli.baymd.rag.config.MemoryProperties;
import com.zhli.baymd.rag.core.memory.retrieve.MemoryInjector;
import com.zhli.baymd.rag.core.memory.retrieve.MemoryRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 记忆策略工厂 — 根据配置创建对应的 MemoryStrategy 实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryStrategyFactory {

    private final ConversationMemoryStore store;
    private final ConversationMemorySummaryService summaryService;
    private final MemoryRetrievalService retrievalService;
    private final MemoryInjector memoryInjector;
    private final MemoryProperties props;

    public MemoryStrategy create() {
        String name = props.getStrategy() != null ? props.getStrategy().trim().toLowerCase() : "sliding_window";
        return switch (name) {
            case "none" -> {
                log.info("记忆策略: 无记忆");
                yield new MemoryStrategies.NoMemory();
            }
            case "summary_compression", "summary" -> {
                log.info("记忆策略: 摘要压缩");
                yield new MemoryStrategies.SummaryCompression(store, summaryService, props);
            }
            case "semantic" -> {
                log.info("记忆策略: 语义记忆 (向量检索 + Facts/Episodes)");
                yield new MemoryStrategies.SemanticMemory(retrievalService, memoryInjector);
            }
            default -> {
                log.info("记忆策略: 滑动窗口 ({}轮)", props.getHistoryKeepTurns());
                yield new MemoryStrategies.SlidingWindow(store, props);
            }
        };
    }
}
