package com.zhli.baymd.rag.testbase;

import com.zhli.baymd.infra.chat.LLMService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试 Spring 配置 — 将 MockLLMService 作为 @Primary Bean 注入。
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 * @SpringBootTest
 * @Import(MockTestConfig.class)
 * class MyIntegrationTest {
 *     @Autowired
 *     private MockLLMService mockLLM;  // 可直接注入来控制 mock 行为
 *
 *     @Autowired
 *     private LLMService llmService;   // 所有依赖 LLMService 的 Bean 都会使用 mock
 * }
 * }</pre>
 *
 * <p>注意：只有显式 @Import 此配置的测试类才会使用 Mock，不影响现有测试。</p>
 */
@TestConfiguration
public class MockTestConfig {

    @Bean
    @Primary
    public MockLLMService mockLLMService() {
        return new MockLLMService();
    }
}
