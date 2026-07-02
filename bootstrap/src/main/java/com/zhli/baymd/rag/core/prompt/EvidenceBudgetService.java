package com.zhli.baymd.rag.core.prompt;

import cn.hutool.core.collection.CollUtil;
import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.infra.token.TokenCounterService;
import com.zhli.baymd.rag.config.EvidenceBudgetProperties;
import com.zhli.baymd.rag.core.intent.NodeScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 证据预算服务 — 控制塞入 LLM Prompt 的证据 Token 数量。
 *
 * <h3>核心逻辑</h3>
 * <ol>
 *   <li>按置信度阈值过滤低分 chunk</li>
 *   <li>按意图比例分配 Token 预算</li>
 *   <li>超预算时按分数截断，保留每个意图至少 minChunksPerIntent 条</li>
 *   <li>记录被省略的条数</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EvidenceBudgetService {

    private final EvidenceBudgetProperties properties;
    private final TokenCounterService tokenCounter;

    /**
     * 对检索结果应用证据预算，返回截断后的 chunk 列表和省略统计。
     *
     * @param intentChunks 按意图分组的检索结果 (intentNodeId → chunks)
     * @param intentScores 意图分数列表（用于按意图比例分配预算）
     * @return 预算控制结果
     */
    public BudgetResult applyBudget(Map<String, List<RetrievedChunk>> intentChunks,
                                     List<NodeScore> intentScores) {
        if (CollUtil.isEmpty(intentChunks)) {
            return BudgetResult.empty();
        }

        // 1. 按置信度阈值过滤
        Map<String, List<RetrievedChunk>> filtered = filterByConfidence(intentChunks);
        if (filtered.isEmpty()) {
            return BudgetResult.empty();
        }

        // 2. 计算总 token 数
        int currentTokens = countTotalTokens(filtered);
        int maxTokens = properties.getMaxTokens();

        if (currentTokens <= maxTokens) {
            // 未超预算，全部保留
            return new BudgetResult(filtered, 0, currentTokens, false);
        }

        // 3. 超预算 — 按策略截断
        Map<String, List<RetrievedChunk>> truncated;
        if (properties.isAllocateByIntent() && CollUtil.isNotEmpty(intentScores)) {
            truncated = truncateByIntentProportion(filtered, intentScores, maxTokens);
        } else {
            truncated = truncateByGlobalScore(filtered, maxTokens);
        }

        int truncatedTokens = countTotalTokens(truncated);
        int omitted = countChunks(filtered) - countChunks(truncated);

        log.info("证据预算截断: 原始={} chunks / {} tokens → 截断后={} chunks / {} tokens, 省略={} 条, 预算={} tokens",
                countChunks(filtered), currentTokens,
                countChunks(truncated), truncatedTokens,
                omitted, maxTokens);

        return new BudgetResult(truncated, omitted, truncatedTokens, true);
    }

    /**
     * 判断检索结果是否有效（高于最低置信度阈值）
     */
    public boolean hasValidEvidence(Map<String, List<RetrievedChunk>> intentChunks) {
        if (CollUtil.isEmpty(intentChunks)) {
            return false;
        }
        double maxScore = intentChunks.values().stream()
                .flatMap(List::stream)
                .mapToDouble(chunk -> chunk.getScore() != null ? chunk.getScore().doubleValue() : 0.0)
                .max()
                .orElse(0.0);
        return maxScore >= properties.getMinConfidenceScore();
    }

    /**
     * 获取无证据时的短路响应文本
     */
    public String getNoEvidenceResponse() {
        return properties.getNoEvidenceResponse();
    }

    // ==================== 内部方法 ====================

    /**
     * 按置信度阈值过滤低分 chunk
     */
    private Map<String, List<RetrievedChunk>> filterByConfidence(Map<String, List<RetrievedChunk>> intentChunks) {
        double threshold = properties.getMinConfidenceScore();
        return intentChunks.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .filter(c -> {
                                    Float score = c.getScore();
                                    return score == null || score >= threshold;
                                })
                                .sorted(Comparator.comparingDouble(
                                        (RetrievedChunk c) -> c.getScore() != null ? c.getScore().doubleValue() : 0.0
                                ).reversed())
                                .collect(Collectors.toList())
                ))
                .entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 按意图比例分配预算截断
     */
    private Map<String, List<RetrievedChunk>> truncateByIntentProportion(
            Map<String, List<RetrievedChunk>> intentChunks,
            List<NodeScore> intentScores,
            int maxTokens) {

        int minPerIntent = properties.getMinChunksPerIntent();
        int totalIntents = intentChunks.size();

        // 先保留每个意图至少 minPerIntent 条
        int reservedTokens = 0;
        Map<String, List<RetrievedChunk>> result = new java.util.LinkedHashMap<>();
        Map<String, List<RetrievedChunk>> remaining = new java.util.LinkedHashMap<>();

        for (var entry : intentChunks.entrySet()) {
            List<RetrievedChunk> chunks = entry.getValue();
            List<RetrievedChunk> reserved = chunks.stream().limit(minPerIntent).collect(Collectors.toList());
            List<RetrievedChunk> rest = chunks.stream().skip(minPerIntent).collect(Collectors.toList());
            result.put(entry.getKey(), new ArrayList<>(reserved));
            remaining.put(entry.getKey(), rest);
            reservedTokens += countTokens(reserved);
        }

        int remainingBudget = maxTokens - reservedTokens;
        if (remainingBudget <= 0) {
            return result; // 最小保留已经吃完了预算
        }

        // 剩余预算按意图分数比例分配
        Map<String, Double> intentWeights = computeIntentWeights(intentChunks.keySet(), intentScores);

        for (var entry : remaining.entrySet()) {
            String intentId = entry.getKey();
            List<RetrievedChunk> rest = entry.getValue();
            if (rest.isEmpty()) continue;

            double weight = intentWeights.getOrDefault(intentId, 1.0 / totalIntents);
            int intentBudget = (int) (remainingBudget * weight);

            List<RetrievedChunk> added = new ArrayList<>();
            int used = 0;
            for (RetrievedChunk chunk : rest) {
                int chunkTokens = countTokens(List.of(chunk));
                if (used + chunkTokens > intentBudget && !added.isEmpty()) {
                    break;
                }
                added.add(chunk);
                used += chunkTokens;
            }
            result.get(intentId).addAll(added);
        }

        return result;
    }

    /**
     * 全局按分数截断（不按意图分配）
     */
    private Map<String, List<RetrievedChunk>> truncateByGlobalScore(
            Map<String, List<RetrievedChunk>> intentChunks,
            int maxTokens) {

        // 全部展开，按分数排序
        List<Map.Entry<String, RetrievedChunk>> allChunks = intentChunks.entrySet().stream()
                .flatMap(e -> e.getValue().stream()
                        .map(c -> Map.entry(e.getKey(), c)))
                .sorted(Comparator.comparingDouble(
                        (Map.Entry<String, RetrievedChunk> e) ->
                                e.getValue().getScore() != null
                                        ? e.getValue().getScore().doubleValue() : 0.0
                ).reversed())
                .toList();

        Map<String, List<RetrievedChunk>> result = new java.util.LinkedHashMap<>();
        int usedTokens = 0;
        int minPerIntent = properties.getMinChunksPerIntent();

        // 先保证每个意图至少 minPerIntent
        for (var entry : intentChunks.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>());
            List<RetrievedChunk> top = entry.getValue().stream()
                    .limit(minPerIntent)
                    .toList();
            result.get(entry.getKey()).addAll(top);
            usedTokens += countTokens(top);
        }

        // 剩余预算按全局分数分配
        for (var entry : allChunks) {
            if (usedTokens >= maxTokens) break;
            String intentId = entry.getKey();
            RetrievedChunk chunk = entry.getValue();
            List<RetrievedChunk> existing = result.get(intentId);
            // 跳过已经在 minPerIntent 中的
            if (existing.contains(chunk)) continue;

            int chunkTokens = countTokens(List.of(chunk));
            if (usedTokens + chunkTokens > maxTokens) break;

            existing.add(chunk);
            usedTokens += chunkTokens;
        }

        return result;
    }

    /**
     * 根据意图分数计算权重（用于按比例分配）
     */
    private Map<String, Double> computeIntentWeights(java.util.Set<String> intentIds,
                                                       List<NodeScore> intentScores) {
        Map<String, Double> weights = new java.util.LinkedHashMap<>();
        double totalScore = 0.0;

        for (NodeScore ns : intentScores) {
            String id = ns.getNode() != null ? ns.getNode().getId() : null;
            if (id != null && intentIds.contains(id)) {
                double score = ns.getScore();
                weights.put(id, score);
                totalScore += score;
            }
        }

        // 归一化
        if (totalScore > 0) {
            for (var entry : weights.entrySet()) {
                weights.put(entry.getKey(), entry.getValue() / totalScore);
            }
        }

        // 为没有分数的意图分配平均权重
        double unassignedWeight = 1.0 / Math.max(1, intentIds.size());
        for (String id : intentIds) {
            weights.putIfAbsent(id, unassignedWeight);
        }

        return weights;
    }

    private int countTotalTokens(Map<String, List<RetrievedChunk>> intentChunks) {
        return intentChunks.values().stream()
                .mapToInt(this::countTokens)
                .sum();
    }

    private int countTokens(List<RetrievedChunk> chunks) {
        return chunks.stream()
                .mapToInt(c -> {
                    String text = c.getText() != null ? c.getText() : "";
                    return tokenCounter.countTokens(text);
                })
                .sum();
    }

    private static int countChunks(Map<String, List<RetrievedChunk>> intentChunks) {
        return intentChunks.values().stream().mapToInt(List::size).sum();
    }

    // ==================== 结果类型 ====================

    /**
     * 预算控制结果
     */
    public record BudgetResult(
            Map<String, List<RetrievedChunk>> chunks,
            int omittedCount,
            int totalTokens,
            boolean truncated
    ) {
        public static BudgetResult empty() {
            return new BudgetResult(Map.of(), 0, 0, false);
        }

        public boolean isEmpty() {
            return chunks.isEmpty();
        }

        public String omittedNote() {
            if (omittedCount <= 0) return "";
            return String.format("（注：共检索到相关文档，因篇幅限制已省略 %d 条）", omittedCount);
        }
    }
}
