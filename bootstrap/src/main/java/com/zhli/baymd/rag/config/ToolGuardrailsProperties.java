package com.zhli.baymd.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 工具护栏配置属性
 *
 * <p>在 application.yaml 中配置：</p>
 * <pre>
 * rag.guardrails:
 *   default:
 *     max-retries: 3
 *     timeout: 30s
 *     base-backoff: 100ms
 *     max-backoff: 10s
 *     jitter-factor: 0.3
 *   mcp-tool:
 *     max-retries: 2
 *     timeout: 20s
 *   retrieval-channel:
 *     max-retries: 1
 *     timeout: 15s
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.guardrails")
public class ToolGuardrailsProperties {

    /** 默认护栏配置 */
    private GuardrailsConfig defaults = new GuardrailsConfig();

    /** MCP 工具专用配置（覆盖默认） */
    private GuardrailsConfig mcpTool = new GuardrailsConfig();

    /** 检索通道专用配置（覆盖默认） */
    private GuardrailsConfig retrievalChannel = new GuardrailsConfig();

    @Data
    public static class GuardrailsConfig {
        /** 最大重试次数（不含首次调用），0 = 不重试 */
        private int maxRetries = 3;

        /** 单次调用超时 */
        private Duration timeout = Duration.ofSeconds(30);

        /** 基础退避时间（首次重试延迟） */
        private Duration baseBackoff = Duration.ofMillis(100);

        /** 最大退避时间（避免无限增长） */
        private Duration maxBackoff = Duration.ofSeconds(10);

        /** 抖动因子 (0.0 ~ 1.0)，0 = 无抖动 */
        private double jitterFactor = 0.3;
    }
}
