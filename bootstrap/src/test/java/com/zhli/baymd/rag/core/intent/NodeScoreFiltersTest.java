package com.zhli.baymd.rag.core.intent;

import com.zhli.baymd.rag.enums.IntentKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("意图评分过滤器 — NodeScoreFilters")
class NodeScoreFiltersTest {

    private static NodeScore mcpScore(String toolId, double score) {
        return NodeScore.builder()
                .node(IntentNode.builder().kind(IntentKind.MCP).mcpToolId(toolId).build())
                .score(score)
                .build();
    }

    private static NodeScore kbScore(double score) {
        return NodeScore.builder()
                .node(IntentNode.builder().kind(IntentKind.KB).build())
                .score(score)
                .build();
    }

    private static NodeScore systemScore(double score) {
        return NodeScore.builder()
                .node(IntentNode.builder().kind(IntentKind.SYSTEM).build())
                .score(score)
                .build();
    }

    // ==================== MCP filter ====================

    @Test
    @DisplayName("mcp() 只保留 MCP 类型且有 toolId 的")
    void mcpFilterKeepsOnlyMcpWithToolId() {
        List<NodeScore> scores = List.of(
                mcpScore("tool-1", 0.9),
                mcpScore(null, 0.8),     // 无 toolId → 排除
                kbScore(0.7),
                systemScore(0.6)
        );
        List<NodeScore> result = NodeScoreFilters.mcp(scores);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNode().getMcpToolId()).isEqualTo("tool-1");
    }

    @Test
    @DisplayName("mcp() 无 MCP 意图 → 空列表")
    void mcpEmptyWhenNoMcp() {
        List<NodeScore> result = NodeScoreFilters.mcp(List.of(kbScore(0.9), systemScore(0.8)));
        assertThat(result).isEmpty();
    }

    // ==================== KB filter (no minScore) ====================

    @Test
    @DisplayName("kb() 只保留 KB 类型（含 kind=null）")
    void kbFilterKeepsKB() {
        var nullKindScore = NodeScore.builder()
                .node(IntentNode.builder().kind(null).build())
                .score(0.5)
                .build();

        List<NodeScore> scores = List.of(kbScore(0.9), systemScore(0.7), nullKindScore);
        List<NodeScore> result = NodeScoreFilters.kb(scores);
        assertThat(result).hasSize(2); // KB + null-kind
    }

    @Test
    @DisplayName("kb() 无 KB 意图 → 空列表")
    void kbEmptyWhenNoKB() {
        List<NodeScore> result = NodeScoreFilters.kb(List.of(systemScore(0.9)));
        assertThat(result).isEmpty();
    }

    // ==================== KB filter (with minScore) ====================

    @Test
    @DisplayName("kb(minScore) 过滤低于最低分的 KB score")
    void kbWithMinScore() {
        List<NodeScore> scores = List.of(kbScore(0.9), kbScore(0.3), kbScore(0.6));
        List<NodeScore> result = NodeScoreFilters.kb(scores, 0.5);
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NodeScore::getScore).containsExactly(0.9, 0.6);
    }

    @Test
    @DisplayName("kb(minScore) 也排除非 KB 类型")
    void kbWithMinScoreExcludesNonKB() {
        List<NodeScore> scores = List.of(kbScore(0.9), systemScore(0.9));
        List<NodeScore> result = NodeScoreFilters.kb(scores, 0.5);
        assertThat(result).hasSize(1).extracting(s -> s.getNode().getKind())
                .containsExactly(IntentKind.KB);
    }

    // ==================== node=null safety ====================

    @Test
    @DisplayName("node=null 的 score 被过滤掉")
    void nullNodeFilteredOut() {
        var nullNode = NodeScore.builder().node(null).score(0.9).build();
        assertThat(NodeScoreFilters.kb(List.of(nullNode))).isEmpty();
        assertThat(NodeScoreFilters.mcp(List.of(nullNode))).isEmpty();
    }

    @Test
    @DisplayName("空列表 → 返回空")
    void emptyInput() {
        assertThat(NodeScoreFilters.kb(Collections.emptyList())).isEmpty();
        assertThat(NodeScoreFilters.mcp(Collections.emptyList())).isEmpty();
    }
}
