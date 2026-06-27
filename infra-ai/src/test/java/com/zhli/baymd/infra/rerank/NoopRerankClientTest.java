package com.zhli.baymd.infra.rerank;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Noop 重排序客户端")
class NoopRerankClientTest {

    private final NoopRerankClient client = new NoopRerankClient();

    @Test
    @DisplayName("provider() → NOOP")
    void providerIsNoop() {
        assertThat(client.provider()).isEqualTo("noop");
    }

    @Test
    @DisplayName("null 输入 → 空列表")
    void nullInput() {
        assertThat(client.rerank("query", null, 5, null)).isEmpty();
    }

    @Test
    @DisplayName("空列表 → 空列表")
    void emptyInput() {
        assertThat(client.rerank("query", List.of(), 5, null)).isEmpty();
    }

    @Test
    @DisplayName("candidates <= topN → 原样返回")
    void fewerOrEqualCandidates() {
        var chunks = List.of(chunk("a", 0.9f), chunk("b", 0.8f), chunk("c", 0.7f));
        var result = client.rerank("query", chunks, 3, null);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("candidates > topN → 截取前 topN 个")
    void truncateToTopN() {
        var chunks = List.of(chunk("a", 0.9f), chunk("b", 0.8f),
                chunk("c", 0.7f), chunk("d", 0.6f), chunk("e", 0.5f));
        var result = client.rerank("query", chunks, 3, null);
        assertThat(result).hasSize(3);
        assertThat(result).extracting(RetrievedChunk::getId)
                .containsExactly("a", "b", "c");
    }

    @Test
    @DisplayName("topN <= 0 → 原样返回（不作截断）")
    void topNZeroOrLess() {
        var chunks = List.of(chunk("a", 0.9f));
        assertThat(client.rerank("query", chunks, 0, null)).hasSize(1);
        assertThat(client.rerank("query", chunks, -1, null)).hasSize(1);
    }

    private RetrievedChunk chunk(String id, float score) {
        return RetrievedChunk.builder().id(id).text("text-" + id).score(score).build();
    }
}
