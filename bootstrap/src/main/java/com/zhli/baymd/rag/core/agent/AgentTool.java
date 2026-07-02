package com.zhli.baymd.rag.core.agent;

import java.util.Map;

/**
 * Agent 可调用工具的统一抽象。
 *
 * <p>任何 Agent 可以调用的能力（知识库检索、MCP 远程工具、系统操作等）
 * 都通过此接口暴露。Agent 通过 ReAct 循环自主决定何时调用哪个工具。</p>
 */
public interface AgentTool {

    /**
     * 工具名称（LLM 通过此名称识别和调用）
     */
    String getName();

    /**
     * 工具功能描述（会写入 Prompt，帮助 LLM 判断何时使用）
     */
    String getDescription();

    /**
     * 参数 Schema JSON（告知 LLM 需要哪些参数及类型）
     * <p>格式示例：</p>
     * <pre>{@code
     * {
     *   "type": "object",
     *   "properties": {
     *     "query": {"type": "string", "description": "搜索查询词"}
 *   },
     *   "required": ["query"]
     * }
     * }</pre>
     */
    Map<String, Object> getParametersSchema();

    /**
     * 执行工具并返回结果。
     *
     * @param parameters LLM 提取的参数
     * @return 工具执行结果（作为 observation 回灌给 LLM）
     */
    AgentToolResult execute(Map<String, Object> parameters);

    /**
     * 工具类型：knowledge_retrieval / mcp / system
     */
    String getType();
}
