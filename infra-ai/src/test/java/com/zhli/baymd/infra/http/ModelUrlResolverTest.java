package com.zhli.baymd.infra.http;

import com.zhli.baymd.infra.config.AIModelProperties;
import com.zhli.baymd.infra.enums.ModelCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("模型 URL 解析器")
class ModelUrlResolverTest {

    // ==================== joinUrl ====================

    @Test
    @DisplayName("base 有 /, path 有 / → 去重")
    void joinBothSlash() {
        var provider = provider("https://api.example.com/", Map.of("chat", "/v1/chat"));
        var result = ModelUrlResolver.resolveUrl(provider, null, ModelCapability.CHAT);
        assertThat(result).isEqualTo("https://api.example.com/v1/chat");
    }

    @Test
    @DisplayName("base 无 /, path 无 / → 补 /")
    void joinNoSlash() {
        var provider = provider("https://api.example.com", Map.of("chat", "v1/chat"));
        var result = ModelUrlResolver.resolveUrl(provider, null, ModelCapability.CHAT);
        assertThat(result).isEqualTo("https://api.example.com/v1/chat");
    }

    @Test
    @DisplayName("base 有 /, path 无 / → 直接拼接")
    void baseSlashOnly() {
        var provider = provider("https://api.example.com/", Map.of("chat", "v1/chat"));
        var result = ModelUrlResolver.resolveUrl(provider, null, ModelCapability.CHAT);
        assertThat(result).isEqualTo("https://api.example.com/v1/chat");
    }

    // ==================== priority: candidate > provider ====================

    @Test
    @DisplayName("candidate 自带 URL → 优先使用，忽略 provider")
    void candidateUrlWins() {
        var provider = provider("https://default.api.com", Map.of("chat", "/v1/chat"));
        var candidate = candidate("https://custom.api.com/v2/chat");
        var result = ModelUrlResolver.resolveUrl(provider, candidate, ModelCapability.CHAT);
        assertThat(result).isEqualTo("https://custom.api.com/v2/chat");
    }

    @Test
    @DisplayName("candidate URL 为空 → fallback 到 provider")
    void fallbackWhenCandidateEmpty() {
        var candidate = new AIModelProperties.ModelCandidate();
        candidate.setUrl("");
        var provider = provider("https://api.example.com", Map.of("chat", "/v1/chat"));
        var result = ModelUrlResolver.resolveUrl(provider, candidate, ModelCapability.CHAT);
        assertThat(result).isEqualTo("https://api.example.com/v1/chat");
    }

    // ==================== capability → endpoint ====================

    @Test
    @DisplayName("CHAT → chat 端点")
    void chatEndpoint() {
        var provider = provider("https://api.example.com", Map.of("chat", "/v1/chat"));
        assertThat(ModelUrlResolver.resolveUrl(provider, null, ModelCapability.CHAT))
                .endsWith("/v1/chat");
    }

    @Test
    @DisplayName("EMBEDDING → embedding 端点")
    void embeddingEndpoint() {
        var provider = provider("https://api.example.com", Map.of("embedding", "/v1/embeddings"));
        assertThat(ModelUrlResolver.resolveUrl(provider, null, ModelCapability.EMBEDDING))
                .endsWith("/v1/embeddings");
    }

    @Test
    @DisplayName("RERANK → rerank 端点")
    void rerankEndpoint() {
        var provider = provider("https://api.example.com", Map.of("rerank", "/v1/rerank"));
        assertThat(ModelUrlResolver.resolveUrl(provider, null, ModelCapability.RERANK))
                .endsWith("/v1/rerank");
    }

    // ==================== exception paths ====================

    @Test
    @DisplayName("provider 为 null → 抛出异常")
    void nullProviderThrows() {
        assertThatThrownBy(() -> ModelUrlResolver.resolveUrl(null, null, ModelCapability.CHAT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("baseUrl");
    }

    @Test
    @DisplayName("endpoint 缺失 → 抛出异常")
    void missingEndpointThrows() {
        var provider = provider("https://api.example.com", Map.of()); // 空 map
        assertThatThrownBy(() -> ModelUrlResolver.resolveUrl(provider, null, ModelCapability.CHAT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("endpoint");
    }

    // ==================== helpers ====================

    private AIModelProperties.ProviderConfig provider(String url, Map<String, String> endpoints) {
        var p = new AIModelProperties.ProviderConfig();
        p.setUrl(url);
        p.setEndpoints(new HashMap<>(endpoints));
        return p;
    }

    private AIModelProperties.ModelCandidate candidate(String url) {
        var c = new AIModelProperties.ModelCandidate();
        c.setUrl(url);
        return c;
    }
}
