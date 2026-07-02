package com.zhli.baymd.rag.testbase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EvaluationDataset 加载器单元测试
 */
@DisplayName("评测数据集加载测试")
class EvaluationDatasetTest {

    @Test
    @DisplayName("加载冒烟评测集 — 20条")
    void shouldLoadSmokeDataset() {
        EvaluationDataset ds = EvaluationDataset.loadSmoke();

        assertThat(ds.size()).isEqualTo(20);
        assertThat(ds.getItems().get(0).getId()).isEqualTo("smoke-001");
        assertThat(ds.getItems().get(0).getQuestion()).isEqualTo("头疼应该挂什么科");
    }

    @Test
    @DisplayName("加载完整评测集 — 50条")
    void shouldLoadFullDataset() {
        EvaluationDataset ds = EvaluationDataset.loadFull();

        assertThat(ds.size()).isEqualTo(50);
    }

    @Test
    @DisplayName("按意图类型筛选")
    void shouldFilterByIntent() {
        EvaluationDataset ds = EvaluationDataset.loadSmoke();

        List<EvaluationDataset.EvalItem> drugItems = ds.filterByIntent("medical/drug_query");
        assertThat(drugItems).isNotEmpty();
        drugItems.forEach(item ->
                assertThat(item.getExpectedIntent()).startsWith("medical/drug_query"));
    }

    @Test
    @DisplayName("按意图种类筛选")
    void shouldFilterByKind() {
        EvaluationDataset ds = EvaluationDataset.loadFull();

        List<EvaluationDataset.EvalItem> kbItems = ds.filterByKind("KB");
        assertThat(kbItems).hasSize(50); // 目前全部是 KB 类型
    }

    @Test
    @DisplayName("topN — 返回前N条")
    void shouldReturnTopN() {
        EvaluationDataset ds = EvaluationDataset.loadSmoke();

        assertThat(ds.topN(5)).hasSize(5);
        assertThat(ds.topN(100)).hasSize(20); // 不超出实际数量
    }

    @Test
    @DisplayName("summary — 输出统计信息")
    void shouldPrintSummary() {
        EvaluationDataset ds = EvaluationDataset.loadSmoke();

        String summary = ds.summary();
        assertThat(summary).contains("总数: 20 条");
        assertThat(summary).contains("medical/dept_recommend");
        assertThat(summary).contains("medical/drug_query");
    }

    @Test
    @DisplayName("EvalItem — 所有项均有必要字段")
    void shouldHaveAllRequiredFields() {
        EvaluationDataset ds = EvaluationDataset.loadFull();

        for (EvaluationDataset.EvalItem item : ds.getItems()) {
            assertThat(item.getId()).as("id 不应为空").isNotBlank();
            assertThat(item.getQuestion()).as("question 不应为空").isNotBlank();
            assertThat(item.getMinAcceptableScore()).as("minAcceptableScore 应在1-5之间")
                    .isBetween(1, 5);
        }
    }
}
