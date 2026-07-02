package com.zhli.baymd.rag.core.rewrite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("子问题分解质量服务测试")
class DecompositionQualityServiceTest {

    private final DecompositionQualityService service = new DecompositionQualityService();

    // ==================== 去重 ====================

    @Test
    @DisplayName("去重 — 移除高度重复的子问题")
    void shouldDeduplicateSimilarQuestions() {
        List<String> input = List.of(
                "高血压患者饮食需要注意什么方面",   // 12 chars, 不会触发 enrichment
                "高血压患者饮食应该注意哪些方面",   // 高度相似，会被去重
                "高血压药物治疗方案有哪些类型"
        );

        List<String> result = service.refine(input, "高血压的饮食和药物问题");

        // 前两个高度相似应去重，最终保留2个
        assertThat(result).hasSize(2);
        assertThat(result).anyMatch(s -> s.contains("药物治疗"));
    }

    @Test
    @DisplayName("去重 — 完全不同的问题不合并")
    void shouldKeepDistinctQuestions() {
        List<String> input = List.of(
                "高血压应该挂什么科",
                "高血压吃什么药",
                "高血压怎么预防"
        );

        List<String> result = service.refine(input, "高血压相关问题");

        assertThat(result).hasSize(3);
    }

    // ==================== 数量截断 ====================

    @Test
    @DisplayName("截断 — 超过5个多样子问题时截断")
    void shouldTruncateTooManyQuestions() {
        List<String> input = List.of(
                "高血压饮食有什么建议",
                "糖尿病患者运动要注意什么",
                "冠心病早期症状有哪些",
                "孕妇贫血怎么补充营养",
                "儿童发烧多少度要去医院",
                "老年人骨质疏松如何预防",
                "慢性胃炎怎么调理恢复"
        );
        List<String> result = service.refine(input, "综合医疗问题");

        assertThat(result).hasSize(DecompositionQualityService.MAX_SUB_QUESTIONS);
    }

    // ==================== 短问题过滤 ====================

    @Test
    @DisplayName("过滤 — 太短的子问题被丢弃")
    void shouldDropTooShortQuestions() {
        List<String> input = List.of("abc", "正常长度的问题", "ab");

        List<String> result = service.refine(input, "正常长度的问题");

        assertThat(result).doesNotContain("abc", "ab");
    }

    // ==================== 输入为空 ====================

    @Test
    @DisplayName("空输入 — 返回改写后问题")
    void shouldReturnRewrittenWhenEmpty() {
        List<String> result = service.refine(List.of(), "改写后的问题");
        assertThat(result).containsExactly("改写后的问题");
    }

    @Test
    @DisplayName("空字符串 — 过滤后返回兜底")
    void shouldFilterEmptyStrings() {
        List<String> input = List.of("", "  ", "\n");
        List<String> result = service.refine(input, "兜底问题");
        assertThat(result).containsExactly("兜底问题");
    }

    // ==================== Jaccard 相似度 ====================

    @Test
    @DisplayName("Jaccard — 完全相同")
    void shouldReturnOneForIdenticalStrings() {
        double sim = service.jaccardSimilarity("高血压饮食", "高血压饮食");
        assertThat(sim).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Jaccard — 完全不同")
    void shouldReturnZeroForCompletelyDifferent() {
        double sim = service.jaccardSimilarity("高血压", "糖尿病");
        assertThat(sim).isLessThan(0.1);
    }

    @Test
    @DisplayName("Jaccard — 部分重叠")
    void shouldReturnMediumForPartialOverlap() {
        // "高血压饮食注意" vs "高血压药物注意" — 共享"高血","血压","注意"
        // 10 bigrams union, 3 intersection → 0.3
        double sim = service.jaccardSimilarity("高血压饮食注意", "高血压药物注意");
        assertThat(sim).isGreaterThan(0.25);
        assertThat(sim).isLessThan(0.5);
    }

    // ==================== 质量评估 ====================

    @Test
    @DisplayName("质量评估 — 高质量拆分")
    void shouldRateHighQuality() {
        List<String> subs = List.of(
                "高血压应该挂什么科",
                "高血压吃什么药",
                "高血压怎么预防"
        );
        double score = service.evaluateQuality(subs, "高血压的综合问题");
        assertThat(score).isGreaterThan(0.7);
    }

    @Test
    @DisplayName("质量评估 — 低质量（无拆分）")
    void shouldRateLowQualityForNoSplit() {
        List<String> subs = List.of("高血压的综合问题");
        double score = service.evaluateQuality(subs, "高血压的综合问题");
        assertThat(score).isLessThan(0.9); // 同原文扣分
    }

    @Test
    @DisplayName("质量评估 — 低质量（重复过多）")
    void shouldRateLowQualityForDuplicates() {
        List<String> subs = List.of(
                "高血压注意什么",
                "高血压注意哪些",
                "高血压应该注意什么"  // 高度相似
        );
        double score = service.evaluateQuality(subs, "高血压注意事项");
        assertThat(score).isLessThanOrEqualTo(0.8);
    }

    // ==================== 自包含增强 ====================

    @Test
    @DisplayName("自包含 — 短问题补充实体")
    void shouldEnrichContextlessQuestions() {
        // 需要 2+ 子问题才能触发自包含检查，且"吃什么药"缺少主语
        List<String> input = List.of("吃什么药", "有什么副作用");
        List<String> result = service.refine(
                input, "高血压患者应该如何用药");

        // "吃什么药" 不包含 "高血压"/"患者"，应被补充上下文
        assertThat(result.get(0)).containsAnyOf("高血压", "患者");
    }
}
