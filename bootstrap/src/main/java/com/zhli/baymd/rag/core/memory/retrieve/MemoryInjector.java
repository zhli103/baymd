package com.zhli.baymd.rag.core.memory.retrieve;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 记忆注入器 — 将检索到的 Facts/Episodes 格式化为 Prompt 片段。
 */
@Component
public class MemoryInjector {

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "health", "健康",
            "behavior", "行为习惯",
            "preference", "偏好",
            "goal", "目标"
    );

    /**
     * 将记忆检索结果构建为 Prompt 注入片段。
     * 如果没有任何记忆，返回空字符串。
     */
    public String buildPromptFragment(MemoryRetrievalService.MemoryResult memory) {
        if (memory == null || memory.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("\n## 关于该用户（从历史对话中提取）\n");

        for (var fact : memory.getFacts()) {
            String label = TYPE_LABELS.getOrDefault(fact.type(), fact.type());
            sb.append("- [").append(label).append("] ").append(fact.text());
            if (fact.confidence() != null && fact.confidence() < 0.8f) {
                sb.append(" (可信度:").append(String.format("%.0f%%", fact.confidence() * 100)).append(")");
            }
            sb.append("\n");
        }

        if (!memory.getEpisodes().isEmpty()) {
            sb.append("\n## 相关历史咨询\n");
            for (var ep : memory.getEpisodes()) {
                sb.append("- ").append(ep.title());
                if (ep.summary() != null && !ep.summary().isBlank()) {
                    sb.append(": ").append(ep.summary());
                }
                sb.append("\n");
            }
        }

        sb.append("\n请结合以上信息回答用户问题。\n");
        return sb.toString();
    }
}
