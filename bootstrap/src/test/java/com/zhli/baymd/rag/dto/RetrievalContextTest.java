package com.zhli.baymd.rag.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RetrievalContext 单元测试（纯逻辑）
 * <p>
 * 测试检索上下文的状态判断：isEmpty / hasKb / hasMcp
 */
@DisplayName("检索上下文 — RetrievalContext")
class RetrievalContextTest {

    @Test
    @DisplayName("新构建的空上下文 → isEmpty = true")
    void emptyByDefault() {
        var ctx = RetrievalContext.builder().build();
        assertThat(ctx.isEmpty()).isTrue();
        assertThat(ctx.hasKb()).isFalse();
        assertThat(ctx.hasMcp()).isFalse();
    }

    @Test
    @DisplayName("有 KB 上下文 → isEmpty = false, hasKb = true")
    void hasKbContext() {
        var ctx = RetrievalContext.builder()
                .kbContext("布洛芬是一种非甾体抗炎药")
                .build();

        assertThat(ctx.isEmpty()).isFalse();
        assertThat(ctx.hasKb()).isTrue();
        assertThat(ctx.hasMcp()).isFalse();
    }

    @Test
    @DisplayName("有 MCP 上下文 → isEmpty = false, hasMcp = true")
    void hasMcpContext() {
        var ctx = RetrievalContext.builder()
                .mcpContext("今日北京气温 25°C，空气质量良")
                .build();

        assertThat(ctx.isEmpty()).isFalse();
        assertThat(ctx.hasMcp()).isTrue();
        assertThat(ctx.hasKb()).isFalse();
    }

    @Test
    @DisplayName("同时有 KB 和 MCP → 两者都返回 true")
    void hasBoth() {
        var ctx = RetrievalContext.builder()
                .kbContext("药品说明书片段")
                .mcpContext("实时天气数据")
                .build();

        assertThat(ctx.isEmpty()).isFalse();
        assertThat(ctx.hasKb()).isTrue();
        assertThat(ctx.hasMcp()).isTrue();
    }

    @Test
    @DisplayName("空字符串不视为有效上下文")
    void blankContextIsEmpty() {
        var ctx = RetrievalContext.builder()
                .kbContext("  ")
                .mcpContext("")
                .build();

        assertThat(ctx.isEmpty()).isTrue();
        assertThat(ctx.hasKb()).isFalse();
        assertThat(ctx.hasMcp()).isFalse();
    }
}
