package com.zhli.baymd.framework.trace;

import org.springframework.stereotype.Component;

/**
 * RagStreamTraceSupport 的空操作实现
 * <p>
 * 在未启用完整 Trace 功能时作为默认 Bean 注入，避免 Spring 启动失败。
 * 所有方法均为 no-op，不影响核心对话流程。
 * </p>
 */
@Component
public class NoopRagStreamTraceSupport implements RagStreamTraceSupport {

    @Override
    public StreamSpan beginStreamNode(String name, String type) {
        return NOOP_SPAN;
    }
}
