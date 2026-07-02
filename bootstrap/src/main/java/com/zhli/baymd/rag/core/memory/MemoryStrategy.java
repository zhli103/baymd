package com.zhli.baymd.rag.core.memory;

import com.zhli.baymd.framework.convention.ChatMessage;

import java.util.List;

/**
 * 记忆策略接口 — 定义对话历史的加载方式。
 *
 * <p>三种实现：无记忆 / 滑动窗口 / 摘要压缩</p>
 */
public interface MemoryStrategy {

    /** 策略名称 */
    String getName();

    /** 是否启用 */
    boolean isEnabled();

    /**
     * 加载对话历史
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param currentMessage 当前用户消息（用于上下文感知）
     * @return 历史消息列表
     */
    List<ChatMessage> loadHistory(String conversationId, String userId, ChatMessage currentMessage);
}
