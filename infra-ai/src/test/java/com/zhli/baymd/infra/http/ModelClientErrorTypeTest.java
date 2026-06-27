package com.zhli.baymd.infra.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HTTP 错误类型分类")
class ModelClientErrorTypeTest {

    @Test
    @DisplayName("401 → UNAUTHORIZED")
    void unauthorized401() {
        assertThat(ModelClientErrorType.fromHttpStatus(401))
                .isEqualTo(ModelClientErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("403 → UNAUTHORIZED")
    void unauthorized403() {
        assertThat(ModelClientErrorType.fromHttpStatus(403))
                .isEqualTo(ModelClientErrorType.UNAUTHORIZED);
    }

    @Test
    @DisplayName("429 → RATE_LIMITED")
    void rateLimited() {
        assertThat(ModelClientErrorType.fromHttpStatus(429))
                .isEqualTo(ModelClientErrorType.RATE_LIMITED);
    }

    @Test
    @DisplayName("500 → SERVER_ERROR")
    void serverError500() {
        assertThat(ModelClientErrorType.fromHttpStatus(500))
                .isEqualTo(ModelClientErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("502/503 → SERVER_ERROR")
    void serverErrorRange() {
        assertThat(ModelClientErrorType.fromHttpStatus(502))
                .isEqualTo(ModelClientErrorType.SERVER_ERROR);
        assertThat(ModelClientErrorType.fromHttpStatus(503))
                .isEqualTo(ModelClientErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("400/404/422 → CLIENT_ERROR")
    void clientError() {
        assertThat(ModelClientErrorType.fromHttpStatus(400))
                .isEqualTo(ModelClientErrorType.CLIENT_ERROR);
        assertThat(ModelClientErrorType.fromHttpStatus(404))
                .isEqualTo(ModelClientErrorType.CLIENT_ERROR);
        assertThat(ModelClientErrorType.fromHttpStatus(422))
                .isEqualTo(ModelClientErrorType.CLIENT_ERROR);
    }

    @Test
    @DisplayName("200 → CLIENT_ERROR（非错误状态码）")
    void successUntyped() {
        assertThat(ModelClientErrorType.fromHttpStatus(200))
                .isEqualTo(ModelClientErrorType.CLIENT_ERROR);
    }
}
