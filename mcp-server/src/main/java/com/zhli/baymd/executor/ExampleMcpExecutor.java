package com.zhli.baymd.mcp.executor;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具集成示例
 *
 * 演示如何通过 @Bean 注册一个 SyncToolSpecification 并自动接入 MCP Server。
 * 实现自己的 MCP 工具时，只需照此模式编写 @Component + @Bean 即可。
 */
@Slf4j
@Component
public class ExampleMcpExecutor {

    private static final String TOOL_ID = "example_echo";

    @Bean
    public McpServerFeatures.SyncToolSpecification exampleToolSpecification() {
        return new McpServerFeatures.SyncToolSpecification(buildTool(),
                (exchange, request) -> handleCall(request));
    }

    private Tool buildTool() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("message", Map.of(
                "type", "string",
                "description", "任意文本内容"
        ));
        properties.put("repeat", Map.of(
                "type", "integer",
                "description", "重复次数，默认 1",
                "default", 1
        ));

        JsonSchema inputSchema = new JsonSchema(
                "object", properties, List.of("message"), null, null, null);

        return Tool.builder()
                .name(TOOL_ID)
                .description("示例工具：回显输入内容。可供项目扩展时参考 MCP 工具注册模式。")
                .inputSchema(inputSchema)
                .build();
    }

    private CallToolResult handleCall(CallToolRequest request) {
        try {
            Map<String, Object> args = request.arguments() != null ? request.arguments() : Map.of();
            String message = args.getOrDefault("message", "").toString();
            int repeat = 1;
            Object repeatArg = args.get("repeat");
            if (repeatArg instanceof Number n) {
                repeat = Math.max(1, Math.min(10, n.intValue()));
            }

            String result = message.repeat(repeat);
            log.info("MCP example_echo executed, message length={}, repeat={}", message.length(), repeat);
            return CallToolResult.builder()
                    .content(List.of(new TextContent(result)))
                    .isError(false)
                    .build();
        } catch (Exception e) {
            log.error("MCP example_echo failed", e);
            return CallToolResult.builder()
                    .content(List.of(new TextContent("Error: " + e.getMessage())))
                    .isError(true)
                    .build();
        }
    }
}
