package com.zhli.baymd.infra.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("启发式 Token 计数器")
class HeuristicTokenCounterServiceTest {

    private final HeuristicTokenCounterService counter = new HeuristicTokenCounterService();

    @Test
    @DisplayName("null / 空字符串 → 0")
    void emptyInput() {
        assertThat(counter.countTokens(null)).isEqualTo(0);
        assertThat(counter.countTokens("")).isEqualTo(0);
        assertThat(counter.countTokens("   ")).isEqualTo(0);
    }

    @Test
    @DisplayName("纯英文 — 4 字符 ≈ 1 token")
    void pureEnglish() {
        // "hello" = 5 ASCII chars → (5+3)/4 = 2 tokens
        assertThat(counter.countTokens("hello")).isEqualTo(2);
        // "abcdefgh" = 8 ASCII chars → (8+3)/4 = 2 tokens
        assertThat(counter.countTokens("abcdefgh")).isEqualTo(2);
    }

    @Test
    @DisplayName("纯中文 — 1 字符 ≈ 1 token")
    void pureChinese() {
        assertThat(counter.countTokens("你好")).isEqualTo(2);
        assertThat(counter.countTokens("你好世界")).isEqualTo(4);
    }

    @Test
    @DisplayName("纯日文假名 — 1 字符 ≈ 1 token")
    void japaneseKana() {
        assertThat(counter.countTokens("こんにちは")).isEqualTo(5);
    }

    @Test
    @DisplayName("混合中英文")
    void mixedChineseEnglish() {
        // "Hello你好" = 5 ASCII + 2 CJK
        // ASCII token: (5+3)/4 = 2
        // CJK token: 2
        // Total: 4
        assertThat(counter.countTokens("Hello你好")).isEqualTo(4);
    }

    @Test
    @DisplayName("空白字符被跳过")
    void whitespaceSkipped() {
        assertThat(counter.countTokens("a b c")).isEqualTo(1); // 3 ASCII chars
    }

    @Test
    @DisplayName("最少返回 1")
    void minimumOne() {
        assertThat(counter.countTokens("a")).isEqualTo(1);
        assertThat(counter.countTokens("中")).isEqualTo(1);
    }
}
