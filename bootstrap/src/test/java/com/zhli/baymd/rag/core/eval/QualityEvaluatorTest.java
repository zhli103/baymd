package com.zhli.baymd.rag.core.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("质量评估器测试")
class QualityEvaluatorTest {

    @Test
    @DisplayName("空回答 — 返回空分数")
    void shouldReturnEmptyForBlankAnswer() {
        QualityEvaluator.QualityScore score =
                QualityEvaluator.QualityScore.empty();
        assertThat(score.isValid()).isFalse();
        assertThat(score.average()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("有效分数 — 均值计算正确")
    void shouldComputeCorrectAverage() {
        QualityEvaluator.QualityScore score = new QualityEvaluator.QualityScore(4, 3, 5, 4);
        assertThat(score.isValid()).isTrue();
        assertThat(score.average()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("分数摘要 — 格式正确")
    void shouldFormatSummaryCorrectly() {
        QualityEvaluator.QualityScore score = new QualityEvaluator.QualityScore(5, 4, 5, 3);
        String summary = score.toSummary();
        assertThat(summary).contains("准确度=5").contains("均分=4");
    }
}
