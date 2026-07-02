package com.zhli.baymd.rag.core.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 工具执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentToolResult {

    /** 工具名称 */
    private String toolName;

    /** 执行是否成功 */
    @Builder.Default
    private boolean success = true;

    /** 结果文本内容（作为 observation 回灌给 LLM） */
    private String content;

    /** 错误信息（success=false 时） */
    private String errorMessage;

    /** 执行耗时（毫秒） */
    private long elapsedMs;

    /** 结果中的文档数量（仅知识库检索工具有意义） */
    @Builder.Default
    private int documentCount = 0;

    public static AgentToolResult success(String toolName, String content, int docCount, long elapsedMs) {
        return AgentToolResult.builder()
                .toolName(toolName)
                .success(true)
                .content(content)
                .documentCount(docCount)
                .elapsedMs(elapsedMs)
                .build();
    }

    public static AgentToolResult error(String toolName, String errorMessage, long elapsedMs) {
        return AgentToolResult.builder()
                .toolName(toolName)
                .success(false)
                .errorMessage(errorMessage)
                .elapsedMs(elapsedMs)
                .build();
    }
}
