package com.zhli.baymd.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ReAct Agent 配置属性
 *
 * <pre>
 * rag.react:
 *   max-iterations: 10
 *   enable-thinking-stream: true
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.react")
public class ReActProperties {

    /** ReAct 循环最大迭代次数 */
    private int maxIterations = 10;

    /** 单次会话最大 Token 消耗 */
    private int maxTotalTokens = 100_000;

    /** 总挂钟时间上限（毫秒） */
    private long maxWallClockMs = 120_000;

    /** 是否流式展示思考过程 */
    private boolean enableThinkingStream = true;
}
