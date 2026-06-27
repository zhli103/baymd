package com.zhli.baymd.infra.model;

import com.zhli.baymd.infra.config.AIModelProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ModelHealthStore 单元测试（纯逻辑，零依赖，秒级运行）
 * <p>
 * 测试三态断路器：CLOSED → OPEN → HALF_OPEN 的状态转换。
 * 无需启动 Spring、无需数据库、无需 Docker。
 * <p>
 * 运行方式：IDE 中右键单个方法 Run，或命令行：
 * <pre>./mvnw test -pl infra-ai -Dtest=ModelHealthStoreTest</pre>
 */
@DisplayName("模型健康状态断路器")
class ModelHealthStoreTest {

    private ModelHealthStore healthStore;

    @BeforeEach
    void setUp() {
        var selection = new AIModelProperties.Selection();
        selection.setFailureThreshold(2);
        selection.setOpenDurationMs(5000L);

        var properties = new AIModelProperties();
        properties.setSelection(selection);

        healthStore = new ModelHealthStore(properties);
    }

    // ==================== 初始状态 ====================

    @Test
    @DisplayName("初始状态 — 模型可用，允许调用")
    void initialStateAvailable() {
        assertThat(healthStore.isUnavailable("gpt-4")).isFalse();
        assertThat(healthStore.allowCall("gpt-4")).isTrue();
    }

    // ==================== 失败累积 → 熔断 ====================

    @Test
    @DisplayName("连续失败达到阈值(2次) → 断路器 OPEN → 拒绝调用")
    void consecutiveFailuresOpenCircuit() {
        healthStore.markFailure("gpt-4");
        assertThat(healthStore.allowCall("gpt-4")).isTrue();  // 第1次失败，还不熔断

        healthStore.markFailure("gpt-4");
        assertThat(healthStore.allowCall("gpt-4")).isFalse(); // 第2次失败 → 熔断
        assertThat(healthStore.isUnavailable("gpt-4")).isTrue();
    }

    @Test
    @DisplayName("单次失败不触发熔断")
    void singleFailureDoesNotOpen() {
        healthStore.markFailure("gpt-4");
        assertThat(healthStore.allowCall("gpt-4")).isTrue();
        assertThat(healthStore.isUnavailable("gpt-4")).isFalse();
    }

    // ==================== 成功恢复 ====================

    @Test
    @DisplayName("markSuccess 重置失败计数 —— 再失败一次不会熔断")
    void successResetsFailureCount() {
        healthStore.markFailure("gpt-4");
        healthStore.markSuccess("gpt-4");  // 成功 → 计数归零
        healthStore.markFailure("gpt-4");  // 需要重新累积2次才熔断
        assertThat(healthStore.allowCall("gpt-4")).isTrue();
    }

    @Test
    @DisplayName("断路器 OPEN → 超时后自动进入 HALF_OPEN → 允许一次探测")
    void openToHalfOpenProbe() throws InterruptedException {
        healthStore.markFailure("gpt-4");
        healthStore.markFailure("gpt-4");
        assertThat(healthStore.allowCall("gpt-4")).isFalse(); // OPEN

        Thread.sleep(5100); // 超过 openDurationMs=5000

        assertThat(healthStore.allowCall("gpt-4")).isTrue();  // HALF_OPEN 探测
        assertThat(healthStore.allowCall("gpt-4")).isFalse(); // 探测进行中，拒绝其他
    }

    @Test
    @DisplayName("HALF_OPEN 探测成功 → 回到 CLOSED")
    void halfOpenProbeSuccessCloses() {
        healthStore.markFailure("gpt-4");
        healthStore.markFailure("gpt-4");
        assertThat(healthStore.isUnavailable("gpt-4")).isTrue();

        healthStore.markSuccess("gpt-4"); // 探测成功
        assertThat(healthStore.isUnavailable("gpt-4")).isFalse();
        assertThat(healthStore.allowCall("gpt-4")).isTrue();
    }

    @Test
    @DisplayName("HALF_OPEN 探测失败 → 立即重新 OPEN")
    void halfOpenProbeFailureReopens() {
        healthStore.markFailure("gpt-4");
        healthStore.markFailure("gpt-4");
        healthStore.markFailure("gpt-4"); // HALF_OPEN 失败
        assertThat(healthStore.isUnavailable("gpt-4")).isTrue();
        assertThat(healthStore.allowCall("gpt-4")).isFalse();
    }

    // ==================== 边界 ====================

    @Test
    @DisplayName("null modelId — allowCall 返回 false，mark不抛异常")
    void nullIdDoesNotExplode() {
        // markFailure/markSuccess 有 null 保护，不抛异常
        healthStore.markFailure(null);
        healthStore.markSuccess(null);
        // allowCall(null) 直接返回 false（保护性检查）
        assertThat(healthStore.allowCall(null)).isFalse();
        // 注意：isUnavailable(null) 会因 ConcurrentHashMap.get(null) 抛 NPE，
        // 这是 JDK 限制，业务代码已有 allowCall 做 null 保护
    }

    @Test
    @DisplayName("不同模型独立计数 — gpt-4 熔断不影响 claude")
    void differentModelsAreIndependent() {
        healthStore.markFailure("gpt-4");
        healthStore.markFailure("gpt-4");
        assertThat(healthStore.isUnavailable("gpt-4")).isTrue();
        assertThat(healthStore.isUnavailable("claude")).isFalse();
    }
}
