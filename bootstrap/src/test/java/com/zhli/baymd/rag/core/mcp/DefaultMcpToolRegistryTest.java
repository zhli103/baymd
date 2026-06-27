package com.zhli.baymd.rag.core.mcp;

import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MCP 工具注册表 — DefaultMcpToolRegistry")
class DefaultMcpToolRegistryTest {

    private DefaultMcpToolRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DefaultMcpToolRegistry(Collections.emptyList());
    }

    @Test
    @DisplayName("初始状态 — 空注册表")
    void initialState() {
        assertThat(registry.size()).isEqualTo(0);
        assertThat(registry.contains("any")).isFalse();
        assertThat(registry.listAllTools()).isEmpty();
        assertThat(registry.listAllExecutors()).isEmpty();
    }

    @Test
    @DisplayName("注册一个执行器 → size=1, 可查询")
    void registerAndLookup() {
        registry.register(mockExecutor("tool-1", "echo"));

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.contains("tool-1")).isTrue();
        assertThat(registry.getExecutor("tool-1")).isPresent();
        assertThat(registry.listAllExecutors()).hasSize(1);
        assertThat(registry.listAllTools()).hasSize(1);
    }

    @Test
    @DisplayName("注册多个工具 → 独立查询")
    void registerMultiple() {
        registry.register(mockExecutor("t1", "echo"));
        registry.register(mockExecutor("t2", "weather"));
        registry.register(mockExecutor("t3", "search"));

        assertThat(registry.size()).isEqualTo(3);
        assertThat(registry.contains("t1")).isTrue();
        assertThat(registry.contains("t2")).isTrue();
        assertThat(registry.contains("t3")).isTrue();
    }

    @Test
    @DisplayName("重复注册相同 toolId → 覆盖，size 不变")
    void duplicateOverwrites() {
        registry.register(mockExecutor("tool-1", "echo"));
        registry.register(mockExecutor("tool-1", "echo-v2"));

        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.listAllTools().get(0).name()).isEqualTo("echo-v2");
    }

    @Test
    @DisplayName("注销存在工具 → size 减 1")
    void unregisterExisting() {
        registry.register(mockExecutor("tool-1", "echo"));
        registry.unregister("tool-1");

        assertThat(registry.size()).isEqualTo(0);
        assertThat(registry.contains("tool-1")).isFalse();
    }

    @Test
    @DisplayName("注销不存在工具 → 不抛异常，size 不变")
    void unregisterNonExistent() {
        registry.register(mockExecutor("tool-1", "echo"));
        registry.unregister("no-such-tool");
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("查找不存在 → Optional.empty()")
    void lookupNonExistent() {
        assertThat(registry.getExecutor("no-such-tool")).isEmpty();
    }

    @Test
    @DisplayName("注册 null → 忽略，不抛异常")
    void registerNullIgnored() {
        registry.register(null);
        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("listAllExecutors 返回列表引用不影响注册表")
    void listAllExecutorsDetached() {
        registry.register(mockExecutor("tool-1", "echo"));
        List<McpToolExecutor> list = registry.listAllExecutors();
        list.clear(); // 不影响注册表
        assertThat(registry.size()).isEqualTo(1);
    }

    // ==================== helper ====================

    private McpToolExecutor mockExecutor(String toolId, String toolName) {
        Tool mockTool = mock(Tool.class);
        when(mockTool.name()).thenReturn(toolName);

        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.getToolId()).thenReturn(toolId);
        when(executor.getToolDefinition()).thenReturn(mockTool);
        when(executor.execute(Map.of())).thenReturn(mock(CallToolResult.class));
        return executor;
    }
}
