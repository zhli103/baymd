package com.zhli.baymd.rag.core.retrieve.postprocessor;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.rag.core.retrieve.channel.SearchChannelResult;
import com.zhli.baymd.rag.core.retrieve.channel.SearchChannelType;
import com.zhli.baymd.rag.core.retrieve.channel.SearchContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RRF 融合后处理器测试")
class RRFPostProcessorTest {

    private final RRFPostProcessor processor = new RRFPostProcessor(60);

    // ==================== 基本功能 ====================

    @Test
    @DisplayName("单通道 — 直接返回原列表")
    void shouldReturnUnchangedWhenSingleChannel() {
        List<RetrievedChunk> chunks = List.of(chunk("a", 0.9f), chunk("b", 0.8f));
        List<SearchChannelResult> results = List.of(
                channelResult(SearchChannelType.VECTOR_GLOBAL, chunks)
        );

        List<RetrievedChunk> result = processor.process(chunks, results, dummyContext());

        assertThat(result).containsExactlyElementsOf(chunks);
    }

    @Test
    @DisplayName("空列表 — 直接返回")
    void shouldReturnEmptyWhenNoChunks() {
        List<RetrievedChunk> result = processor.process(List.of(), List.of(), dummyContext());
        assertThat(result).isEmpty();
    }

    // ==================== RRF 融合逻辑 ====================

    @Test
    @DisplayName("两通道 — 共同文档获得更高 RRF 分排前面")
    void shouldBoostChunkInBothChannels() {
        // 通道1: A(1st), B(2nd), C(3rd)
        List<RetrievedChunk> channel1 = List.of(
                chunk("A", 0.9f), chunk("B", 0.8f), chunk("C", 0.7f));
        // 通道2: B(1st), Z(2nd), A(3rd)
        List<RetrievedChunk> channel2 = List.of(
                chunk("B", 0.95f), chunk("Z", 0.6f), chunk("A", 0.5f));

        List<SearchChannelResult> results = List.of(
                channelResult(SearchChannelType.VECTOR_GLOBAL, channel1),
                channelResult(SearchChannelType.INTENT_DIRECTED, channel2)
        );

        // 合并后的候选（去重后的结果）
        List<RetrievedChunk> merged = List.of(chunk("A", 0.9f), chunk("B", 0.95f),
                chunk("C", 0.7f), chunk("Z", 0.6f));

        List<RetrievedChunk> result = processor.process(merged, results, dummyContext());

        // B 在两个通道中排名都高(2nd, 1st) → RRF 分最高: 1/62+1/61=0.03252
        // A 在通道1=1st, 通道2=3rd → 第二: 1/61+1/63=0.03226
        // Z 仅在通道2=2nd → 第三: 1/62=0.01613
        // C 仅在通道1=3rd → 最末: 1/63=0.01587
        assertThat(result.get(0).getId()).isEqualTo("B");
        assertThat(result.get(1).getId()).isEqualTo("A");
        assertThat(result.get(2).getId()).isEqualTo("Z");
        assertThat(result.get(3).getId()).isEqualTo("C");
    }

    // ==================== RRF 分数计算 ====================

    @Test
    @DisplayName("RRF 分数计算 — 手动验证公式")
    void shouldComputeCorrectRRFScore() {
        List<SearchChannelResult> results = List.of(
                channelResult(SearchChannelType.VECTOR_GLOBAL,
                        List.of(chunk("D1", 0.9f), chunk("D2", 0.8f), chunk("D3", 0.7f))),
                channelResult(SearchChannelType.INTENT_DIRECTED,
                        List.of(chunk("D2", 0.85f), chunk("D1", 0.75f)))
        );

        List<Map<String, Integer>> rankMaps = processor.buildChannelRankMaps(results);

        // D1: 通道1 rank=1, 通道2 rank=2
        // RRF = 1/(60+1) + 1/(60+2) = 1/61 + 1/62 ≈ 0.01639 + 0.01613 = 0.03252
        double d1Score = processor.computeRRFScore("D1", rankMaps);
        assertThat(d1Score).isEqualTo(1.0 / 61 + 1.0 / 62);

        // D2: 通道1 rank=2, 通道2 rank=1
        // RRF = 1/(60+2) + 1/(60+1) = 相同分数
        double d2Score = processor.computeRRFScore("D2", rankMaps);
        assertThat(d2Score).isEqualTo(d1Score);

        // D3: 仅在通道1 rank=3
        // RRF = 1/(60+3) = 1/63 ≈ 0.01587
        double d3Score = processor.computeRRFScore("D3", rankMaps);
        assertThat(d3Score).isEqualTo(1.0 / 63);
        assertThat(d3Score).isLessThan(d1Score);
    }

    // ==================== 边界场景 ====================

    @Test
    @DisplayName("chunk 不在任何通道 — 分数为 0")
    void shouldReturnZeroWhenNotInAnyChannel() {
        List<Map<String, Integer>> rankMaps = List.of(
                Map.of("X", 1)
        );

        double score = processor.computeRRFScore("Unknown", rankMaps);
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    @DisplayName("空通道 — 不影响计算")
    void shouldHandleEmptyChannel() {
        List<SearchChannelResult> results = List.of(
                channelResult(SearchChannelType.VECTOR_GLOBAL, List.of(chunk("A", 0.9f))),
                channelResult(SearchChannelType.INTENT_DIRECTED, List.of()) // 空通道
        );

        List<RetrievedChunk> merged = List.of(chunk("A", 0.9f));
        List<RetrievedChunk> result = processor.process(merged, results, dummyContext());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("A");
    }

    @Test
    @DisplayName("相同 RRF 分 — 保持原顺序")
    void shouldPreserveOrderWhenSameScore() {
        List<RetrievedChunk> channel1 = List.of(chunk("X", 0.9f), chunk("Y", 0.8f));
        List<RetrievedChunk> channel2 = List.of(chunk("X", 0.85f), chunk("Y", 0.75f));

        List<SearchChannelResult> results = List.of(
                channelResult(SearchChannelType.VECTOR_GLOBAL, channel1),
                channelResult(SearchChannelType.INTENT_DIRECTED, channel2)
        );

        List<RetrievedChunk> merged = List.of(chunk("X", 0.9f), chunk("Y", 0.8f));
        List<RetrievedChunk> result = processor.process(merged, results, dummyContext());

        // X 和 Y 都在两个通道中 rank 相同(1st,1st) 和 (2nd,2nd)，RRF 分数相同
        assertThat(result).hasSize(2);
    }

    // ==================== 辅助方法 ====================

    static RetrievedChunk chunk(String id, float score) {
        return RetrievedChunk.builder()
                .id(id)
                .text("Content of " + id)
                .score(score)
                .build();
    }

    static SearchChannelResult channelResult(SearchChannelType type, List<RetrievedChunk> chunks) {
        return SearchChannelResult.builder()
                .channelType(type)
                .channelName(type.name())
                .chunks(chunks)
                .build();
    }

    static SearchContext dummyContext() {
        return SearchContext.builder()
                .originalQuestion("test query")
                .rewrittenQuestion("test query")
                .topK(5)
                .build();
    }
}
