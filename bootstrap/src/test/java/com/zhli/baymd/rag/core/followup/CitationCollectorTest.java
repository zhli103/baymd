package com.zhli.baymd.rag.core.followup;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("引用收集器测试")
class CitationCollectorTest {

    @Test
    @DisplayName("收集引用 — 去重")
    void shouldDeduplicateById() {
        CitationCollector collector = new CitationCollector();
        RetrievedChunk chunk = RetrievedChunk.builder().id("doc-1")
                .text("Document content").score(0.9f).build();

        collector.collect(Map.of("intent1", List.of(chunk, chunk))); // 同一文档两次

        assertThat(collector.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("空检索 — 引用列表为空")
    void shouldReturnEmptyWhenNoChunks() {
        CitationCollector collector = new CitationCollector();
        assertThat(collector.isEmpty()).isTrue();
        assertThat(collector.toCitationList()).isEmpty();
    }

    @Test
    @DisplayName("引用格式 — 包含索引和摘要")
    void shouldFormatCitationCorrectly() {
        CitationCollector collector = new CitationCollector();
        collector.collect(Map.of("i1", List.of(
                RetrievedChunk.builder().id("d1").text("ABCDEFGHIJ").score(0.8f).build()
        )));

        List<CitationCollector.Citation> citations = collector.toCitationList();
        assertThat(citations).hasSize(1);
        assertThat(citations.get(0).id()).isEqualTo("d1");
        assertThat(citations.get(0).index()).isEqualTo(1);
        assertThat(citations.get(0).snippet()).isEqualTo("ABCDEFGHIJ");
    }
}
