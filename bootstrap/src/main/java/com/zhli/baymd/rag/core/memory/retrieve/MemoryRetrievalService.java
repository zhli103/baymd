package com.zhli.baymd.rag.core.memory.retrieve;

import com.zhli.baymd.infra.embedding.EmbeddingService;
import com.zhli.baymd.rag.dao.mapper.UserEpisodeVectorMapper;
import com.zhli.baymd.rag.dao.mapper.UserFactVectorMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 语义记忆检索服务 — 用用户问题向量检索相关历史 Facts 和 Episodes。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryRetrievalService {

    private final EmbeddingService embeddingService;
    private final UserFactVectorMapper factVectorMapper;
    private final UserEpisodeVectorMapper episodeVectorMapper;

    private static final double MIN_SIMILARITY = 0.3;
    private static final int FACT_TOP_K = 10;
    private static final int EPISODE_TOP_K = 5;

    /**
     * 检索与问题相关的用户记忆。
     */
    public MemoryResult retrieve(String question, String userId) {
        try {
            List<Float> vec = embeddingService.embed(question);
            String vecStr = vec.stream().map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));

            // 并行检索 Fact 和 Episode
            List<UserFactVectorMapper.FactVectorResult> factResults =
                    factVectorMapper.searchSimilar(userId, vecStr, FACT_TOP_K);
            List<UserEpisodeVectorMapper.EpisodeVectorResult> episodeResults =
                    episodeVectorMapper.searchSimilar(userId, vecStr, EPISODE_TOP_K);

            // 过滤低分结果
            List<MemoryResult.FactHit> facts = new ArrayList<>();
            for (var fr : factResults) {
                if (fr.similarity() != null && fr.similarity() >= MIN_SIMILARITY) {
                    facts.add(new MemoryResult.FactHit(
                            fr.factText(), fr.factType(), fr.confidence(), fr.similarity()));
                }
            }

            List<MemoryResult.EpisodeHit> episodes = new ArrayList<>();
            for (var er : episodeResults) {
                if (er.similarity() != null && er.similarity() >= MIN_SIMILARITY) {
                    episodes.add(new MemoryResult.EpisodeHit(
                            er.title(), er.summary(), er.similarity()));
                }
            }

            if (!facts.isEmpty() || !episodes.isEmpty()) {
                log.debug("语义记忆检索: question={}, facts={}, episodes={}",
                        question.substring(0, Math.min(30, question.length())),
                        facts.size(), episodes.size());
            }

            return new MemoryResult(facts, episodes);

        } catch (Exception e) {
            log.warn("语义记忆检索失败: userId={}", userId, e);
            return MemoryResult.empty();
        }
    }

    @Data
    public static class MemoryResult {
        private final List<FactHit> facts;
        private final List<EpisodeHit> episodes;

        public boolean isEmpty() { return facts.isEmpty() && episodes.isEmpty(); }

        public static MemoryResult empty() { return new MemoryResult(List.of(), List.of()); }

        public record FactHit(String text, String type, Float confidence, Double similarity) {}
        public record EpisodeHit(String title, String summary, Double similarity) {}
    }
}
