package com.zhli.baymd.infra.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LLM 输出清理器")
class LLMResponseCleanerTest {

    @Test
    @DisplayName("null → null")
    void nullInput() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence(null)).isNull();
    }

    @Test
    @DisplayName("纯文本 → 保持原样")
    void plainText() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("hello world"))
                .isEqualTo("hello world");
    }

    @Test
    @DisplayName("移除 ```json ... ``` 代码围栏")
    void stripJsonFence() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("```json\n{\"key\": 1}\n```"))
                .isEqualTo("{\"key\": 1}");
    }

    @Test
    @DisplayName("移除无语言标注的围栏")
    void stripPlainFence() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("```\nsome code\n```"))
                .isEqualTo("some code");
    }

    @Test
    @DisplayName("移除 ```java ... ``` 围栏")
    void stripJavaFence() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("```java\nclass A {}\n```"))
                .isEqualTo("class A {}");
    }

    @Test
    @DisplayName("只有开头围栏无结尾 → 只移除开头")
    void leadingOnly() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("```json\n{\"key\": 1}"))
                .isEqualTo("{\"key\": 1}");
    }

    @Test
    @DisplayName("只有结尾围栏无开头 → 只移除结尾")
    void trailingOnly() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("{\"key\": 1}\n```"))
                .isEqualTo("{\"key\": 1}");
    }

    @Test
    @DisplayName("空白字符串 → 空白")
    void blankInput() {
        assertThat(LLMResponseCleaner.stripMarkdownCodeFence("   ")).isEqualTo("");
    }
}
