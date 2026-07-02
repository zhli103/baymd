package com.zhli.baymd.rag.core.eval;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.framework.convention.ChatRequest;
import com.zhli.baymd.infra.chat.LLMService;
import com.zhli.baymd.infra.util.LLMResponseCleaner;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * LLM-as-Judge 质量评估器 — 从 4 个维度对 RAG 回答打分。
 *
 * <p>异步执行，不阻塞用户响应。评分结果写入 Trace。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityEvaluator {

    private final LLMService llmService;
    private static final Gson GSON = new Gson();

    private static final String JUDGE_PROMPT = """
            你是 RAG 系统质量评审专家。基于以下参考要点，从4个维度给回答评分(1-5):

            【用户问题】: %s
            【参考要点】: %s
            【系统回答】: %s

            严格返回 JSON：
            {
              "accuracy": <1-5, 回答与参考要点的匹配度>,
              "completeness": <1-5, 是否覆盖了所有参考要点>,
              "faithfulness": <1-5, 回答是否有编造内容(5=完全基于证据)>,
              "conciseness": <1-5, 是否简洁无冗余>
            }
            """;

    /**
     * 异步评估回答质量
     */
    public CompletableFuture<QualityScore> evaluateAsync(String question, String answer,
                                                          List<String> referencePoints) {
        return CompletableFuture.supplyAsync(() -> evaluate(question, answer, referencePoints));
    }

    /**
     * 同步评估
     */
    public QualityScore evaluate(String question, String answer, List<String> referencePoints) {
        if (answer == null || answer.isBlank()) {
            return QualityScore.empty();
        }
        try {
            String refs = referencePoints != null && !referencePoints.isEmpty()
                    ? String.join("、", referencePoints) : "无";

            String prompt = String.format(JUDGE_PROMPT, question, refs, answer);
            ChatRequest req = ChatRequest.builder()
                    .messages(List.of(ChatMessage.user(prompt)))
                    .temperature(0.0).maxTokens(256).build();

            String raw = llmService.chat(req);
            String cleaned = LLMResponseCleaner.stripMarkdownCodeFence(raw);
            JsonObject json = GSON.fromJson(cleaned, JsonObject.class);

            return new QualityScore(
                    getInt(json, "accuracy"),
                    getInt(json, "completeness"),
                    getInt(json, "faithfulness"),
                    getInt(json, "conciseness")
            );
        } catch (Exception e) {
            log.warn("质量评估失败: question={}", question.substring(0, Math.min(50, question.length())), e);
            return QualityScore.empty();
        }
    }

    private int getInt(JsonObject json, String key) {
        if (json == null || !json.has(key)) return 0;
        try { return json.get(key).getAsInt(); } catch (Exception e) { return 0; }
    }

    @Data
    @Builder
    public static class QualityScore {
        private final int accuracy;
        private final int completeness;
        private final int faithfulness;
        private final int conciseness;

        public double average() { return (accuracy + completeness + faithfulness + conciseness) / 4.0; }

        public boolean isValid() { return accuracy > 0 && completeness > 0; }

        public static QualityScore empty() {
            return new QualityScore(0, 0, 0, 0);
        }

        public String toSummary() {
            return String.format("准确度=%d 完整度=%d 忠实度=%d 简洁度=%d 均分=%.1f",
                    accuracy, completeness, faithfulness, conciseness, average());
        }
    }
}
