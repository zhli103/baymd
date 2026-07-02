package com.zhli.baymd.rag.testbase;

import lombok.Getter;

/**
 * Mock 响应对象 — 封装预设 LLM 响应的内容和类型。
 */
@Getter
public class MockResponse {

    private final MockLLMService.MockResponseType type;
    private final String content;
    private final String thinkingContent;
    private final String toolName;
    private final String toolArguments;

    private MockResponse(MockLLMService.MockResponseType type, String content,
                         String thinkingContent, String toolName, String toolArguments) {
        this.type = type;
        this.content = content;
        this.thinkingContent = thinkingContent;
        this.toolName = toolName;
        this.toolArguments = toolArguments;
    }

    /**
     * 创建纯文本响应
     */
    public static MockResponse text(String content) {
        return new MockResponse(MockLLMService.MockResponseType.TEXT, content, null, null, null);
    }

    /**
     * 创建工具调用响应
     */
    public static MockResponse toolCall(String toolName, String arguments) {
        return new MockResponse(MockLLMService.MockResponseType.TOOL_CALL, null, null, toolName, arguments);
    }

    /**
     * 创建"先思考再工具调用"响应（用于 ReAct 中间步骤测试）
     */
    public static MockResponse thinkThenToolCall(String thinking, String toolName, String arguments) {
        return new MockResponse(MockLLMService.MockResponseType.THINK_THEN_TOOL_CALL,
                null, thinking, toolName, arguments);
    }

    /**
     * 将本响应序列化为工具调用 JSON（OpenAI function calling 格式）
     */
    public String toToolCallJson() {
        if (type == MockLLMService.MockResponseType.TOOL_CALL
                || type == MockLLMService.MockResponseType.THINK_THEN_TOOL_CALL) {
            return String.format(
                    "{\"tool_calls\":[{\"id\":\"call_mock_%d\",\"type\":\"function\"," +
                            "\"function\":{\"name\":\"%s\",\"arguments\":\"%s\"}}]}",
                    System.nanoTime(), toolName, toolArguments.replace("\"", "\\\""));
        }
        return content;
    }
}
