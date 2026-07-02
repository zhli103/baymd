package com.zhli.baymd.rag.core.agent;

import cn.hutool.core.collection.CollUtil;
import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.rag.core.retrieve.MultiChannelRetrievalEngine;
import com.zhli.baymd.rag.dto.SubQuestionIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识库检索工具 — 将 RAG 检索包装为 Agent 可调用的工具。
 *
 * <p>Agent 在需要查知识库时主动调用此工具，检索结果作为 observation 回灌给 LLM。
 * 这使得检索从主链路径降级为 Agent 可按需调用的工具之一。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetrievalTool implements AgentTool {

    private static final String TOOL_NAME = "search_knowledge_base";
    private static final String TOOL_TYPE = "knowledge_retrieval";

    private final MultiChannelRetrievalEngine retrievalEngine;

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "搜索医疗知识库，获取与查询相关的文档片段。当用户询问医学问题、症状、药物、"
                + "疾病、治疗方法等需要专业知识的问题时使用此工具。"
                + "参数 query 应该是从用户问题中提取的关键搜索词。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "query", Map.of(
                                "type", "string",
                                "description", "从用户问题中提取的关键搜索词或问题"
                        )
                ),
                "required", List.of("query")
        );
    }

    @Override
    public AgentToolResult execute(Map<String, Object> parameters) {
        long start = System.currentTimeMillis();

        try {
            String query = (String) parameters.getOrDefault("query", "");
            if (query.isBlank()) {
                return AgentToolResult.error(TOOL_NAME, "搜索词不能为空", System.currentTimeMillis() - start);
            }

            int topK = getTopK(parameters);
            log.info("Agent 调用知识库检索: query={}, topK={}", query, topK);

            // 构造简易 SubQuestionIntent（无需完整意图分类，传入空 nodeScores）
            SubQuestionIntent intent = new SubQuestionIntent(query, List.of());
            List<RetrievedChunk> chunks = retrievalEngine.retrieveKnowledgeChannels(
                    List.of(intent), topK
            );

            if (CollUtil.isEmpty(chunks)) {
                return AgentToolResult.success(TOOL_NAME,
                        "未找到与 \"" + query + "\" 相关的文档。",
                        0, System.currentTimeMillis() - start);
            }

            // 格式化结果
            String content = formatRetrievalResults(query, chunks);
            return AgentToolResult.success(TOOL_NAME, content, chunks.size(),
                    System.currentTimeMillis() - start);

        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return AgentToolResult.error(TOOL_NAME,
                    "检索异常: " + e.getMessage(),
                    System.currentTimeMillis() - start);
        }
    }

    @Override
    public String getType() {
        return TOOL_TYPE;
    }

    private int getTopK(Map<String, Object> parameters) {
        Object topKObj = parameters.get("top_k");
        if (topKObj instanceof Number n) {
            return Math.min(n.intValue(), 20); // 上限 20
        }
        return 5; // 默认 5 条
    }

    private String formatRetrievalResults(String query, List<RetrievedChunk> chunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("知识库检索结果（查询: \"").append(query).append("\"，共 ")
                .append(chunks.size()).append(" 条）：\n\n");

        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            sb.append("--- 文档片段 ").append(i + 1);
            if (chunk.getScore() != null) {
                sb.append(" (相关性: ").append(String.format("%.2f", chunk.getScore())).append(")");
            }
            sb.append(" ---\n");
            sb.append(chunk.getText()).append("\n\n");
        }

        return sb.toString().trim();
    }
}
