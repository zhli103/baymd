package com.zhli.baymd.rag.core.rewrite;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 子问题分解质量服务 — 验证和提升 LLM 拆分结果的质量。
 *
 * <h3>核心能力</h3>
 * <ul>
 *   <li>去重：移除语义高度重复的子问题（Jaccard 相似度 > 0.7）</li>
 *   <li>截断：超过 maxCount 时按顺序保留前 N 个</li>
 *   <li>完整性：确保每个子问题是自包含的（含核心实体词）</li>
 *   <li>降级：拆分结果无效时回退到原问题</li>
 * </ul>
 */
@Slf4j
@Service
public class DecompositionQualityService {

    /** 最大子问题数量 */
    static final int MAX_SUB_QUESTIONS = 5;

    /** 最小子问题长度（字符） */
    private static final int MIN_SUB_QUESTION_LENGTH = 4;

    /** Jaccard 相似度阈值：超过此值视为重复 */
    private static final double DEDUP_SIMILARITY_THRESHOLD = 0.35;

    /** 最大总子问题 token 数（防 Prompt 溢出） */
    private static final int MAX_TOTAL_TOKENS = 512;

    /**
     * 对拆分结果进行质量检查和优化。
     *
     * @param subQuestions   原始子问题列表
     * @param rewrittenQuery 改写后的查询（作为兜底）
     * @return 优化后的子问题列表
     */
    public List<String> refine(List<String> subQuestions, String rewrittenQuery) {
        if (CollUtil.isEmpty(subQuestions)) {
            return List.of(rewrittenQuery);
        }

        List<String> result = new ArrayList<>(subQuestions);

        // 1. 清理空白和截断
        result = cleanAndValidate(result);

        // 2. 去重
        result = deduplicate(result);

        // 3. 数量截断
        if (result.size() > MAX_SUB_QUESTIONS) {
            log.info("子问题数量超限: {} > {}, 截断保留前 {} 个",
                    result.size(), MAX_SUB_QUESTIONS, MAX_SUB_QUESTIONS);
            result = result.subList(0, MAX_SUB_QUESTIONS);
        }

        // 4. 确保自包含性（补全缺少上下文的子问题）
        result = ensureSelfContained(result, rewrittenQuery);

        // 5. Token 预算检查
        result = enforceTokenBudget(result);

        // 6. 兜底保证
        if (result.isEmpty()) {
            return List.of(rewrittenQuery);
        }

        if (result.size() != subQuestions.size()) {
            log.info("子问题优化: {} → {} (去重={}, 截断={})",
                    subQuestions.size(), result.size(),
                    subQuestions.size() - countUnique(subQuestions),
                    Math.max(0, subQuestions.size() - MAX_SUB_QUESTIONS));
        }

        return result;
    }

    /**
     * 评估拆分质量分数（0.0 ~ 1.0），供上层判断是否需要回退。
     */
    public double evaluateQuality(List<String> subQuestions, String originalQuestion) {
        if (CollUtil.isEmpty(subQuestions)) return 0.0;

        double score = 1.0;

        // 子问题太多 → 扣分
        if (subQuestions.size() > MAX_SUB_QUESTIONS) {
            score -= 0.3;
        }
        // 只有一个且与原文一样 → 低质量（没有真正拆分）
        if (subQuestions.size() == 1
                && subQuestions.get(0).equals(originalQuestion)) {
            score -= 0.2;
        }
        // 有太短的子问题
        for (String sq : subQuestions) {
            if (sq.length() < MIN_SUB_QUESTION_LENGTH) {
                score -= 0.1;
                break;
            }
        }
        // 子问题之间高度重复
        if (hasHighSimilarity(subQuestions)) {
            score -= 0.2;
        }

        return Math.max(0.0, score);
    }

    // ==================== 内部方法 ====================

    private List<String> cleanAndValidate(List<String> questions) {
        List<String> cleaned = new ArrayList<>();
        for (String q : questions) {
            String trimmed = q.trim();
            // 移除末尾多余标点
            if (trimmed.endsWith("？") || trimmed.endsWith("?")) {
                // keep as-is
            }
            if (trimmed.length() >= MIN_SUB_QUESTION_LENGTH) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }

    /**
     * 基于 Jaccard 相似度去重。
     */
    List<String> deduplicate(List<String> questions) {
        if (questions.size() <= 1) return new ArrayList<>(questions);

        List<String> result = new ArrayList<>();
        for (String q : questions) {
            boolean isDup = false;
            for (String existing : result) {
                if (jaccardSimilarity(q, existing) > DEDUP_SIMILARITY_THRESHOLD) {
                    isDup = true;
                    break;
                }
            }
            if (!isDup) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * 确保子问题自包含：如果子问题太短且不包含改写后的关键实体，
     * 尝试用改写后的查询补充上下文。
     */
    private List<String> ensureSelfContained(List<String> questions, String rewritten) {
        if (questions.size() <= 1) return questions;

        // 提取改写后查询的核心实体词（长度 > 1 的实词）
        Set<String> coreTokens = extractCoreTokens(rewritten);

        List<String> enriched = new ArrayList<>();
        for (String q : questions) {
            Set<String> qTokens = extractCoreTokens(q);
            // 如果子问题缺少核心实体，添加改写后的核心词作为上下文前缀
            if (q.length() < 10 && !hasOverlap(qTokens, coreTokens)) {
                String prefix = extractKeyPhrase(rewritten);
                enriched.add(prefix + "中的" + q);
            } else {
                enriched.add(q);
            }
        }
        return enriched;
    }

    private List<String> enforceTokenBudget(List<String> questions) {
        int total = 0;
        List<String> within = new ArrayList<>();
        for (String q : questions) {
            // 中文字符 ≈ 1 token，英文 ≈ 0.25 token，粗略估算
            int estTokens = estimateTokens(q);
            if (total + estTokens > MAX_TOTAL_TOKENS) break;
            total += estTokens;
            within.add(q);
        }
        return within.isEmpty() ? questions : within;
    }

    // ==================== 相似度计算 ====================

    /**
     * Jaccard 相似度 = |A ∩ B| / |A ∪ B|（基于字符 bigram）
     */
    double jaccardSimilarity(String a, String b) {
        if (a.equals(b)) return 1.0;

        Set<String> bigramsA = charBigrams(a);
        Set<String> bigramsB = charBigrams(b);
        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 0.0;

        Set<String> union = new HashSet<>(bigramsA);
        union.addAll(bigramsB);

        Set<String> intersection = new HashSet<>(bigramsA);
        intersection.retainAll(bigramsB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> charBigrams(String s) {
        Set<String> bigrams = new HashSet<>();
        for (int i = 0; i < s.length() - 1; i++) {
            bigrams.add(s.substring(i, i + 2));
        }
        return bigrams;
    }

    private boolean hasHighSimilarity(List<String> questions) {
        for (int i = 0; i < questions.size(); i++) {
            for (int j = i + 1; j < questions.size(); j++) {
                if (jaccardSimilarity(questions.get(i), questions.get(j)) > DEDUP_SIMILARITY_THRESHOLD) {
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== Token 工具 ====================

    private Set<String> extractCoreTokens(String text) {
        Set<String> tokens = new HashSet<>();
        // 简单分词：按常见分隔符拆分，保留长度 > 1 的词
        for (String part : text.split("[\\s，。？?！!、；;：:（）()]+")) {
            if (part.length() > 1) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String extractKeyPhrase(String text) {
        // 截取前 12 个字符作为关键短语
        return text.length() <= 12 ? text : text.substring(0, 12);
    }

    private boolean hasOverlap(Set<String> a, Set<String> b) {
        // 精确匹配
        for (String token : a) {
            if (b.contains(token)) return true;
        }
        // 子串匹配：a 中的词是否出现在 b 的某个词中
        for (String ta : a) {
            if (ta.length() < 2) continue;
            for (String tb : b) {
                if (tb.length() < 2) continue;
                if (ta.contains(tb) || tb.contains(ta)) return true;
            }
        }
        return false;
    }

    private int estimateTokens(String text) {
        int tokens = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) continue;
            if (c <= 0x7F) {
                tokens += (i % 4 == 0) ? 1 : 0; // 英文约 4 字符 1 token
            } else {
                tokens += 1; // CJK 字符约 1 token
            }
        }
        return Math.max(tokens, 1);
    }

    private static int countUnique(List<String> list) {
        return new HashSet<>(list).size();
    }
}
