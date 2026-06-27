package com.zhli.baymd.rag.core.retrieve.postprocessor;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.rag.core.retrieve.channel.SearchChannelResult;
import com.zhli.baymd.rag.core.retrieve.channel.SearchChannelType;
import com.zhli.baymd.rag.core.retrieve.channel.SearchContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DeduplicationPostProcessor 单元测试（纯逻辑）
 * <p>
 * 测试多通道检索结果去重：相同 chunk 保留高分、通道优先级合并
 */
@DisplayName("去重后置处理器 — Deduplication")
class DeduplicationPostProcessorTest {

    private DeduplicationPostProcessor dedup;

    @BeforeEach
    void setUp() {
        dedup = new DeduplicationPostProcessor();
    }

    // ==================== 基本功能 ====================

    @Test
    @DisplayName("空结果 → 返回空列表")
    void emptyResultsReturnsEmpty() {
        List<RetrievedChunk> result = dedup.process(List.of(), List.of(), null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("单通道单结果 → 直接返回")
    void singleChannelSingleChunk() {
        var chunk = chunk("c1", "内容A", 0.8f);
        var channelResult = result(SearchChannelType.VECTOR_GLOBAL, chunk);

        List<RetrievedChunk> result = dedup.process(
                List.of(chunk), List.of(channelResult), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("c1");
    }

    // ==================== 去重：相同 ID → 保留高分 ====================

    @Test
    @DisplayName("两个通道命中同一 chunk（同 ID）→ 保留高分的")
    void sameIdKeepsHigherScore() {
        var intentChunk = chunk("c1", "布洛芬副作用", 0.9f);
        var globalChunk = chunk("c1", "布洛芬副作用", 0.6f);

        var intentResult = result(SearchChannelType.INTENT_DIRECTED, intentChunk);
        var globalResult = result(SearchChannelType.VECTOR_GLOBAL, globalChunk);

        List<RetrievedChunk> result = dedup.process(
                List.of(intentChunk, globalChunk),
                List.of(intentResult, globalResult), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(0.9f);
    }

    @Test
    @DisplayName("多通道命中同 ID → 始终保留最高分")
    void keepsMaxScoreRegardlessOfChannelOrder() {
        var low  = chunk("c1", "text", 0.3f);
        var mid  = chunk("c1", "text", 0.6f);
        var high = chunk("c1", "text", 0.95f);

        // 故意把低分放在前面的通道
        var r3 = result(SearchChannelType.VECTOR_GLOBAL, low);
        var r2 = result(SearchChannelType.KEYWORD_ES, mid);
        var r1 = result(SearchChannelType.INTENT_DIRECTED, high);

        List<RetrievedChunk> result = dedup.process(
                List.of(low, mid, high),
                List.of(r3, r2, r1), null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(0.95f);
    }

    // ==================== 去重：无 ID → 按内容哈希 ====================

    @Test
    @DisplayName("chunk 无 ID 时按内容文本去重")
    void dedupByContentHashWhenNoId() {
        var c1 = RetrievedChunk.builder().text("相同内容").score(0.7f).build();
        var c2 = RetrievedChunk.builder().text("相同内容").score(0.5f).build();

        var intentResult = result(SearchChannelType.INTENT_DIRECTED, c1);
        var globalResult = result(SearchChannelType.VECTOR_GLOBAL, c2);

        List<RetrievedChunk> result = dedup.process(
                List.of(c1, c2),
                List.of(intentResult, globalResult), null);

        assertThat(result).hasSize(1);
    }

    // ==================== 不同 chunk → 全保留 ====================

    @Test
    @DisplayName("不同 ID 的 chunk → 全部保留，不合并")
    void differentIdsAllKept() {
        var c1 = chunk("c1", "内容A", 0.9f);
        var c2 = chunk("c2", "内容B", 0.8f);
        var c3 = chunk("c3", "内容C", 0.7f);

        var intentResult = result(SearchChannelType.INTENT_DIRECTED, c1, c2);
        var globalResult = result(SearchChannelType.VECTOR_GLOBAL, c3);

        List<RetrievedChunk> result = dedup.process(
                List.of(c1, c2, c3),
                List.of(intentResult, globalResult), null);

        assertThat(result).hasSize(3);
    }

    // ==================== 通道优先级 ====================

    @Test
    @DisplayName("意图定向通道结果排在前面")
    void intentDirectedResultsFirst() {
        var c1 = chunk("c1", "意图命中", 0.9f);
        var c2 = chunk("c2", "全局命中", 0.7f);

        var intentResult = result(SearchChannelType.INTENT_DIRECTED, c1);
        var globalResult = result(SearchChannelType.VECTOR_GLOBAL, c2);

        List<RetrievedChunk> result = dedup.process(
                List.of(c1, c2),
                List.of(globalResult, intentResult), // 故意乱序
                null);

        // INTENT_DIRECTED 优先 → c1 应该排在第一位
        assertThat(result.get(0).getId()).isEqualTo("c1");
    }

    // ==================== 元数据 ====================

    @Test
    @DisplayName("getName 返回名称，getOrder 返回 1")
    void metadataMethods() {
        assertThat(dedup.getName()).isEqualTo("Deduplication");
        assertThat(dedup.getOrder()).isEqualTo(1);
    }

    // ==================== 工具方法 ====================

    private RetrievedChunk chunk(String id, String text, float score) {
        return RetrievedChunk.builder().id(id).text(text).score(score).build();
    }

    private SearchChannelResult result(SearchChannelType type, RetrievedChunk... chunks) {
        return SearchChannelResult.builder()
                .channelType(type)
                .chunks(List.of(chunks))
                .latencyMs(100)
                .build();
    }
}
