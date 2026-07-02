package com.zhli.baymd.rag.service.pipeline;

/**
 * 对话执行器接口 — 每种执行模式对应一个实现。
 *
 * <p>替代原来 StreamChatPipeline 中的三段 if-else 硬编码：</p>
 * <ul>
 *   <li>{@code ClarificationExecutor} — 歧义引导</li>
 *   <li>{@code SystemOnlyExecutor} — 系统直接处理</li>
 *   <li>{@code RagExecutor} — RAG 知识问答</li>
 *   <li>{@code AgentExecutor} — Agent 多步推理</li>
 * </ul>
 */
public interface ConversationExecutor {

    /**
     * 本执行器对应的模式
     */
    ExecutionMode getMode();

    /**
     * 判断是否应使用本执行器处理当前上下文
     *
     * @param ctx 管道上下文
     * @return true 表示本执行器接管
     */
    boolean supports(StreamChatContext ctx);

    /**
     * 执行对话处理
     *
     * @param ctx 管道上下文（包含问题、历史、意图、回调等完整信息）
     */
    void execute(StreamChatContext ctx);
}
