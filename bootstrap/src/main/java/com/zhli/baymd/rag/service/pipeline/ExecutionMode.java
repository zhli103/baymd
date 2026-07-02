package com.zhli.baymd.rag.service.pipeline;

/**
 * 对话执行模式 — 决定使用哪种执行器处理用户请求。
 *
 * <p>意图分类阶段产出一个执行模式，注册表根据模式选择对应的 {@link ConversationExecutor}。</p>
 */
public enum ExecutionMode {

    /** 歧义澄清 — 用户问题不够明确，需要追问引导 */
    CLARIFICATION,

    /** 系统直接处理 — 纯系统能力（闲聊、问候等），不走检索 */
    SYSTEM_ONLY,

    /** RAG 知识问答 — 检索 + LLM 生成 */
    RAG,

    /** Agent 模式 — LLM 自主决定多步工具调用（ReAct 循环） */
    AGENT
}
