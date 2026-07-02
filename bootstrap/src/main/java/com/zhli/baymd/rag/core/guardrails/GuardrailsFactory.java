package com.zhli.baymd.rag.core.guardrails;

import com.zhli.baymd.framework.convention.ToolGuardrails;
import com.zhli.baymd.rag.config.ToolGuardrailsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 护栏工厂 — 从配置属性创建 ToolGuardrails 实例。
 *
 * <p>每种工具类型有独立的护栏配置，可通过 application.yaml 微调。</p>
 */
@Component
@RequiredArgsConstructor
public class GuardrailsFactory {

    private final ToolGuardrailsProperties properties;

    /**
     * 创建 MCP 工具调用护栏（合并 defaults + mcp-tool 覆盖）
     */
    public ToolGuardrails forMcpTool() {
        return build(properties.getDefaults(), properties.getMcpTool());
    }

    /**
     * 创建检索通道调用护栏
     */
    public ToolGuardrails forRetrievalChannel() {
        return build(properties.getDefaults(), properties.getRetrievalChannel());
    }

    /**
     * 创建通用护栏（仅用默认配置）
     */
    public ToolGuardrails defaults() {
        return fromConfig(properties.getDefaults());
    }

    /**
     * 合并 base + override：override 中 > 0 或非 null 的值覆盖 base
     */
    private ToolGuardrails build(ToolGuardrailsProperties.GuardrailsConfig base,
                                  ToolGuardrailsProperties.GuardrailsConfig override) {
        int maxRetries = override.getMaxRetries() != base.getMaxRetries()
                ? override.getMaxRetries() : base.getMaxRetries();
        var timeout = override.getTimeout() != null ? override.getTimeout() : base.getTimeout();
        var baseBackoff = override.getBaseBackoff() != null ? override.getBaseBackoff() : base.getBaseBackoff();
        var maxBackoff = override.getMaxBackoff() != null ? override.getMaxBackoff() : base.getMaxBackoff();
        double jitter = override.getJitterFactor() != base.getJitterFactor()
                ? override.getJitterFactor() : base.getJitterFactor();

        return ToolGuardrails.builder()
                .maxRetries(maxRetries)
                .timeout(timeout)
                .baseBackoff(baseBackoff)
                .maxBackoff(maxBackoff)
                .jitterFactor(jitter)
                .build();
    }

    private ToolGuardrails fromConfig(ToolGuardrailsProperties.GuardrailsConfig config) {
        return ToolGuardrails.builder()
                .maxRetries(config.getMaxRetries())
                .timeout(config.getTimeout())
                .baseBackoff(config.getBaseBackoff())
                .maxBackoff(config.getMaxBackoff())
                .jitterFactor(config.getJitterFactor())
                .build();
    }
}
