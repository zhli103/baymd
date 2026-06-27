package com.zhli.baymd.framework.convention;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@DisplayName("对话消息模型 — ChatMessage")
class ChatMessageTest {

    // ==================== Role.fromString ====================

    @Test
    @DisplayName("fromString(\"user\") → USER")
    void roleUser() {
        assertThat(ChatMessage.Role.fromString("user")).isEqualTo(ChatMessage.Role.USER);
    }

    @Test
    @DisplayName("fromString(\"USER\") 大小写不敏感 → USER")
    void roleCaseInsensitive() {
        assertThat(ChatMessage.Role.fromString("USER")).isEqualTo(ChatMessage.Role.USER);
        assertThat(ChatMessage.Role.fromString("User")).isEqualTo(ChatMessage.Role.USER);
    }

    @Test
    @DisplayName("fromString(\"assistant\") → ASSISTANT")
    void roleAssistant() {
        assertThat(ChatMessage.Role.fromString("assistant")).isEqualTo(ChatMessage.Role.ASSISTANT);
    }

    @Test
    @DisplayName("fromString(\"system\") → SYSTEM")
    void roleSystem() {
        assertThat(ChatMessage.Role.fromString("system")).isEqualTo(ChatMessage.Role.SYSTEM);
    }

    @Test
    @DisplayName("fromString(非法值) → 抛出异常")
    void invalidRoleThrows() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ChatMessage.Role.fromString("admin"));
    }

    // ==================== factory methods ====================

    @Test
    @DisplayName("ChatMessage.system() 创建 SYSTEM 角色消息")
    void systemMessage() {
        var msg = ChatMessage.system("You are a helpful assistant");
        assertThat(msg.getRole()).isEqualTo(ChatMessage.Role.SYSTEM);
        assertThat(msg.getContent()).isEqualTo("You are a helpful assistant");
    }

    @Test
    @DisplayName("ChatMessage.user() 创建 USER 角色消息")
    void userMessage() {
        var msg = ChatMessage.user("Hello");
        assertThat(msg.getRole()).isEqualTo(ChatMessage.Role.USER);
        assertThat(msg.getContent()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("ChatMessage.assistant(content) 创建只含内容的回复")
    void assistantContentOnly() {
        var msg = ChatMessage.assistant("我来帮你查一下");
        assertThat(msg.getRole()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(msg.getContent()).isEqualTo("我来帮你查一下");
        assertThat(msg.getThinkingContent()).isNull();
    }

    @Test
    @DisplayName("ChatMessage.assistant(content, thinking, duration) 含思考过程")
    void assistantWithThinking() {
        var msg = ChatMessage.assistant("结论", "思考过程...", 5);
        assertThat(msg.getContent()).isEqualTo("结论");
        assertThat(msg.getThinkingContent()).isEqualTo("思考过程...");
        assertThat(msg.getThinkingDuration()).isEqualTo(5);
    }
}
