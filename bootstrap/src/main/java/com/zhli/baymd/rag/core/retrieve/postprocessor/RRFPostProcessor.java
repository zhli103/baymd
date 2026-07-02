package com.zhli.baymd.rag.core.retrieve.postprocessor;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.rag.core.retrieve.channel.SearchChannelResult;
import com.zhli.baymd.rag.core.retrieve.channel.SearchContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（Reciprocal Rank Fusion）后置处理器。
 *
 * <p>使用倒序排名融合算法对多通道检索结果重新排序，替代简单的通道优先级+去重。
 * RRF 是确定性的（无需模型调用），精度优于纯优先级排序。</p>
 *
 * <h3>公式</h3>
 * <pre>
 * RRF_score(d) = Σ 1 / (k + rank_i(d))
 * </pre>
 * 其中 k=60, rank_i(d) 是文档 d 在通道 i 中的排名（从 1 开始）。
 *
 * <h3>在执行链中的位置</h3>
 * <pre>
 * Deduplication (order=1) → RRF (order=5) → Rerank (order=10)
 * </pre>
 */
@Slf4j
@Component
public class RRFPostProcessor implements SearchResultPostProcessor {

    /** RRF k 参数（默认 60，业界标准值） */
    static final int DEFAULT_K = 60;

    private final int k;

    public RRFPostProcessor() {
        this(DEFAULT_K);
    }

    /** 允许通过构造器自定义 k 值（测试用） */
    RRFPostProcessor(int k) {
        this.k = k;
    }

    @Override
    public String getName() {
        return "RRF";
    }

    @Override
    public int getOrder() {
        return 5; // 在 Dedup(1) 之后，Rerank(10) 之前
    }

    @Override
    public boolean isEnabled(SearchContext context) {
        return true;
    }

    @Override
    public List<RetrievedChunk> process(List<RetrievedChunk> chunks,
                                         List<SearchChannelResult> results,
                                         SearchContext context) {
        if (chunks.isEmpty() || results == null || results.size() <= 1) {
            // 只有一个通道时 RRF 没有意义，直接返回
            return chunks;
        }

        // 1. 为每个通道建立 chunk_id → rank 的映射
        List<Map<String, Integer>> channelRankMaps = buildChannelRankMaps(results);

        // 2. 为每个 chunk 计算 RRF 分数
        Map<String, RRFScore> scoreMap = new HashMap<>();
        for (RetrievedChunk chunk : chunks) {
            String chunkId = chunkId(chunk);
            double rrfScore = computeRRFScore(chunkId, channelRankMaps);
            scoreMap.put(chunkId, new RRFScore(chunk, rrfScore));
        }

        // 3. 按 RRF 分数降序排列
        List<RetrievedChunk> sorted = chunks.stream()
                .sorted(Comparator.comparingDouble(
                        c -> -scoreMap.getOrDefault(chunkId(c), new RRFScore(c, 0.0)).score))
                .toList();

        if (log.isDebugEnabled()) {
            log.debug("RRF 融合完成: 输入={} chunks, {} 通道, 输出={} chunks",
                    chunks.size(), results.size(), sorted.size());
        }

        return sorted;
    }

    // ==================== 内部方法 ====================

    /**
     * 为每个通道建立 chunk_id → rank 映射。
     * rank 从 1 开始，chunks 在通道结果中已按分数降序排列。
     */
    List<Map<String, Integer>> buildChannelRankMaps(List<SearchChannelResult> results) {
        List<Map<String, Integer>> maps = new ArrayList<>();
        for (SearchChannelResult result : results) {
            Map<String, Integer> rankMap = new HashMap<>();
            List<RetrievedChunk> channelChunks = result.getChunks();
            for (int i = 0; i < channelChunks.size(); i++) {
                String id = chunkId(channelChunks.get(i));
                rankMap.putIfAbsent(id, i + 1); // rank 从 1 开始，保留首次排名
            }
            maps.add(rankMap);
        }
        return maps;
    }

    /**
     * 计算单个 chunk 的 RRF 分数。
     */
    double computeRRFScore(String chunkId, List<Map<String, Integer>> channelRankMaps) {
        double score = 0.0;
        for (Map<String, Integer> rankMap : channelRankMaps) {
            Integer rank = rankMap.get(chunkId);
            if (rank != null) {
                score += 1.0 / (k + rank);
            }
            // chunk 不在该通道中 → 贡献 0
        }
        return score;
    }

    /**
     * 获取 chunk 唯一标识
     */
    static String chunkId(RetrievedChunk chunk) {
        if (chunk.getId() != null) return chunk.getId();
        return String.valueOf(chunk.getText() != null ? chunk.getText().hashCode() : chunk.hashCode());
    }

    // ==================== 内部类型 ====================

    record RRFScore(RetrievedChunk chunk, double score) {}
}
