package com.zhli.baymd.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseExtractor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * StreamChatPipeline 核心链路集成测试
 * <p>
 * 测试完整 RAG 管线：Query改写 → 意图分类 → 知识库检索 → Rerank → LLM流式生成
 * 依赖 Docker 服务（PostgreSQL、Redis、RocketMQ）必须处于运行状态
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RAG 核心链路集成测试")
class StreamChatPipelineIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // ==================== 核心链路 + 速度测试 ====================

    @Test
    @DisplayName("科室推荐 — 头疼挂什么科")
    void testDeptRecommend() {
        StreamResult r = streamChat("头疼应该挂什么科");
        System.out.printf("[科室推荐] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含科室推荐").containsAnyOf("神经内科", "内科", "科室");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("症状自查 — 头晕乏力")
    void testSymptomCheck() {
        StreamResult r = streamChat("头晕乏力没精神怎么回事");
        System.out.printf("[症状自查] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含病因分析").containsAnyOf("贫血", "低血压", "可能", "原因");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("药物查询 — 布洛芬")
    void testDrugQuery() {
        StreamResult r = streamChat("布洛芬有什么副作用");
        System.out.printf("[药物查询] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含药品信息").containsAnyOf("胃肠道", "不良反应", "副作用", "布洛芬");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("饮食建议 — 高血压饮食")
    void testDietAdvice() {
        StreamResult r = streamChat("高血压吃什么好");
        System.out.printf("[饮食建议] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含饮食建议").containsAnyOf("低盐", "蔬菜", "钾", "饮食");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("中医辨证 — 手脚冰凉")
    void testTcmDiagnosis() {
        StreamResult r = streamChat("手脚冰凉是什么虚");
        System.out.printf("[中医辨证] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含中医分析").containsAnyOf("阳虚", "体质", "气血", "中医");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("报告解读 — 转氨酶")
    void testReportInterpret() {
        StreamResult r = streamChat("转氨酶升高是什么意思");
        System.out.printf("[报告解读] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含指标解读").containsAnyOf("肝", "ALT", "转氨酶", "升高");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    @Test
    @DisplayName("医院推荐 — 心脏病医院")
    void testHospitalRecommend() {
        StreamResult r = streamChat("心脏病去什么医院好");
        System.out.printf("[医院推荐] 首包=%dms 总耗时=%dms 内容长度=%d 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.contentLength(), r.snippet(100));
        assertThat(r.content).as("应包含就医建议").containsAnyOf("三甲", "心内", "医院", "专科");
        assertThat(r.totalMs).as("总耗时应在120秒内").isLessThan(120_000);
    }

    // ==================== 边界场景 ====================

    @Test
    @DisplayName("空检索 — 问一个知识库不可能有的问题")
    void testNoResult() {
        StreamResult r = streamChat("火星上怎么种土豆");
        System.out.printf("[空检索] 首包=%dms 总耗时=%dms 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.snippet(100));
        // 应返回未检索到相关内容
        assertThat(r.content).as("无结果时应提示").containsAnyOf("未检索到", "未收录", "无法", "知识库");
    }

    @Test
    @DisplayName("短问题 — 单个关键词")
    void testShortQuestion() {
        StreamResult r = streamChat("胃疼");
        System.out.printf("[短问题] 首包=%dms 总耗时=%dms 内容=%s%n",
                r.firstTokenMs, r.totalMs, r.snippet(100));
        assertThat(r.totalMs).as("短问题应能正常处理").isLessThan(180_000);
    }

    // ==================== 工具方法 ====================

    /**
     * 发起 SSE 流式请求，收集完整响应内容并记录耗时
     */
    private StreamResult streamChat(String question) {
        String url = "http://localhost:" + port + "/api/baymd/rag/v3/chat?question=" +
                java.net.URLEncoder.encode(question, StandardCharsets.UTF_8);

        AtomicLong firstTokenAt = new AtomicLong();
        AtomicLong doneAt = new AtomicLong();
        long start = System.currentTimeMillis();
        List<String> events = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        restTemplate.execute(url, HttpMethod.GET, null, (ClientHttpResponse response) -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    events.add(line);
                    if (line.startsWith("data:")) {
                        String json = line.substring(5).trim();
                        if (json.equals("[DONE]")) {
                            doneAt.set(System.currentTimeMillis());
                            break;
                        }
                        try {
                            com.google.gson.JsonObject obj =
                                    com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                            if ("response".equals(obj.has("type") ? obj.get("type").getAsString() : "")) {
                                String delta = obj.has("delta") ? obj.get("delta").getAsString() : "";
                                if (firstTokenAt.get() == 0 && !delta.isEmpty()) {
                                    firstTokenAt.set(System.currentTimeMillis());
                                }
                                content.append(delta);
                            }
                        } catch (Exception ignored) {
                            // meta/finish 等非内容事件
                        }
                    }
                }
            }
            return null;
        });

        long end = System.currentTimeMillis();
        return new StreamResult(
                content.toString(),
                firstTokenAt.get() > 0 ? firstTokenAt.get() - start : end - start,
                end - start
        );
    }

    record StreamResult(String content, long firstTokenMs, long totalMs) {
        int contentLength() { return content != null ? content.length() : 0; }
        String snippet(int maxLen) {
            if (content == null || content.isEmpty()) return "(空)";
            String s = content.replace("\n", " ").replace("\r", "");
            return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
        }
    }
}
