package com.zhli.baymd.rag.testbase;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 评测数据集加载器 — 从 classpath 加载 JSON 格式的评测集。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * EvaluationDataset smoke = EvaluationDataset.loadSmoke();
 * for (EvalItem item : smoke.getItems()) {
 *     String answer = systemUnderTest.ask(item.getQuestion());
 *     // 用 judge LLM 评分...
 * }
 * }</pre>
 *
 * <p>JSON 格式：每项包含 id, question, expectedIntent, expectedIntentKind, referencePoints, minAcceptableScore</p>
 */
public class EvaluationDataset {

    private static final String SMOKE_PATH = "eval-smoke.json";
    private static final String FULL_PATH = "eval-full.json";
    private static final Gson GSON = new Gson();

    private final List<EvalItem> items;

    private EvaluationDataset(List<EvalItem> items) {
        this.items = List.copyOf(items);
    }

    /**
     * 加载冒烟评测集（20 条，适合每次提交运行）
     */
    public static EvaluationDataset loadSmoke() {
        return load(SMOKE_PATH);
    }

    /**
     * 加载完整评测集（50+ 条，适合发版前运行）
     */
    public static EvaluationDataset loadFull() {
        return load(FULL_PATH);
    }

    private static EvaluationDataset load(String classpath) {
        try (Reader reader = new InputStreamReader(
                EvaluationDataset.class.getClassLoader().getResourceAsStream(classpath),
                StandardCharsets.UTF_8)) {
            if (reader == null) {
                throw new IllegalStateException("评测数据集未找到: " + classpath);
            }
            Type listType = new TypeToken<List<EvalItem>>() {}.getType();
            List<EvalItem> items = GSON.fromJson(reader, listType);
            return new EvaluationDataset(items);
        } catch (Exception e) {
            throw new RuntimeException("加载评测数据集失败: " + classpath, e);
        }
    }

    // ==================== 访问方法 ====================

    public List<EvalItem> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    /**
     * 按意图类型筛选
     */
    public List<EvalItem> filterByIntent(String intentPrefix) {
        return items.stream()
                .filter(i -> i.expectedIntent != null && i.expectedIntent.startsWith(intentPrefix))
                .collect(Collectors.toList());
    }

    /**
     * 按意图种类筛选 (KB / SYSTEM / MCP)
     */
    public List<EvalItem> filterByKind(String kind) {
        return items.stream()
                .filter(i -> kind.equalsIgnoreCase(i.expectedIntentKind))
                .collect(Collectors.toList());
    }

    /**
     * 返回前 N 条（用于快速验证）
     */
    public List<EvalItem> topN(int n) {
        return items.stream().limit(n).collect(Collectors.toList());
    }

    /**
     * 按意图类型分组统计
     */
    public String summary() {
        var byIntent = items.stream()
                .collect(Collectors.groupingBy(
                        i -> i.expectedIntent != null ? i.expectedIntent : "unknown",
                        Collectors.counting()));
        var byKind = items.stream()
                .collect(Collectors.groupingBy(
                        i -> i.expectedIntentKind != null ? i.expectedIntentKind : "unknown",
                        Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("=== 评测数据集统计 ===\n");
        sb.append(String.format("总数: %d 条\n", items.size()));
        sb.append("\n按意图类型:\n");
        byIntent.forEach((k, v) -> sb.append(String.format("  %s: %d\n", k, v)));
        sb.append("\n按意图种类:\n");
        byKind.forEach((k, v) -> sb.append(String.format("  %s: %d\n", k, v)));
        return sb.toString();
    }

    // ==================== 评测项模型 ====================

    /**
     * 单条评测项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvalItem {
        /** 评测项唯一 ID */
        private String id;

        /** 用户输入的问题 */
        private String question;

        /** 期望的意图路径（如 medical/dept_recommend） */
        private String expectedIntent;

        /** 期望的意图种类：KB(0) / SYSTEM(1) / MCP(2) */
        private String expectedIntentKind;

        /** 参考答案中应包含的关键要点 */
        private List<String> referencePoints;

        /** 最低可接受分数 (1-5)，用于 LLM-as-judge 自动评分 */
        private int minAcceptableScore;
    }
}
