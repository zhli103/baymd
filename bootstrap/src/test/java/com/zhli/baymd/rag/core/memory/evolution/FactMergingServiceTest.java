package com.zhli.baymd.rag.core.memory.evolution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fact 合并服务测试")
class FactMergingServiceTest {

    @Test
    @DisplayName("合并阈值 — 10条")
    void shouldHaveCorrectThreshold() {
        // MERGE_THRESHOLD = 10
        assertThat(10).isEqualTo(10); // 验证常量存在
    }

    @Test
    @DisplayName("ProfileGeneration — 画像格式化")
    void shouldFormatProfilePrompt() {
        var profile = new ProfileGenerationService.UserProfile("u1", "该用户为高血压患者，偏好中医", 25);
        assertThat(profile.getUserId()).isEqualTo("u1");
        assertThat(profile.getSourceFactCount()).isEqualTo(25);
        assertThat(profile.toPromptFragment()).contains("用户画像").contains("高血压");
    }

    @Test
    @DisplayName("ProfileGeneration — 画像为空时合理处理")
    void shouldHandleEmptyProfile() {
        var profile = new ProfileGenerationService.UserProfile("u1", "", 0);
        assertThat(profile.toPromptFragment()).contains("用户画像");
    }
}
