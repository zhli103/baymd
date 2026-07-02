package com.zhli.baymd.rag.testbase;

import com.zhli.baymd.framework.convention.ChatMessage;
import com.zhli.baymd.framework.convention.ChatRequest;

import java.util.List;

/**
 * 测试固件 — 提供常用的测试数据和构造方法。
 *
 * <p>所有方法返回不可变或独立的实例，确保测试之间不会相互干扰。</p>
 */
public final class TestFixtures {

    private TestFixtures() {
        // 工具类，禁止实例化
    }

    // ==================== 简单消息 ====================

    /** 标准系统提示词 */
    public static final String SYSTEM_PROMPT = "你是一个医疗健康助手。";

    /** 常见医疗问题 */
    public static final String Q_HEADACHE = "头疼应该挂什么科？";
    public static final String Q_HYPERTENSION = "高血压患者饮食需要注意什么？";
    public static final String Q_INSOMNIA = "失眠怎么办？";
    public static final String Q_FEVER = "发烧多少度需要吃退烧药？";
    public static final String Q_EMPTY_RESULT = "火星上怎么种土豆？";

    // ==================== 快捷构造 ====================

    public static ChatMessage systemMsg() {
        return ChatMessage.system(SYSTEM_PROMPT);
    }

    public static ChatMessage userMsg(String content) {
        return ChatMessage.user(content);
    }

    public static ChatMessage assistantMsg(String content) {
        return ChatMessage.assistant(content);
    }

    public static ChatRequest simpleRequest(String question) {
        return ChatRequest.builder()
                .messages(List.of(ChatMessage.user(question)))
                .temperature(0.0)
                .build();
    }

    /**
     * 构造带系统提示词和用户消息的标准请求
     */
    public static ChatRequest standardRequest(String question) {
        return ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system(SYSTEM_PROMPT),
                        ChatMessage.user(question)
                ))
                .temperature(0.0)
                .build();
    }

    /**
     * 构造多轮对话的请求（含历史消息）
     */
    public static ChatRequest multiTurnRequest(List<ChatMessage> history, String question) {
        return ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.system(SYSTEM_PROMPT),
                        ChatMessage.user("我之前问过一些问题"),
                        ChatMessage.assistant("好的，请继续。"),
                        ChatMessage.user(question)
                ))
                .temperature(0.3)
                .build();
    }

    // ==================== 常见 Mock 响应 ====================

    /** 科室推荐回答 */
    public static final String RESP_DEPT = "根据您的症状（头疼），建议挂**神经内科**。如果伴有发热，建议先到发热门诊排查。";

    /** 饮食建议回答 */
    public static final String RESP_DIET = "高血压患者的饮食建议：1. 控制钠盐摄入（每日<5g）; 2. 增加富含钾的食物（香蕉、土豆）; 3. 限制酒精摄入。";

    /** 无法回答 */
    public static final String RESP_NO_ANSWER = "抱歉，知识库中未收录相关内容，无法回答您的问题。";

    /** 病情澄清引导 */
    public static final String RESP_CLARIFY = "为了更好地帮助您，请问：1. 头疼持续多久了？2. 是否伴有恶心或视力模糊？";

    /** 工具调用：查询天气 */
    public static final String RESP_WEATHER_TOOL = "{\"tool_calls\":[{\"id\":\"call_001\",\"type\":\"function\","
            + "\"function\":{\"name\":\"get_weather\",\"arguments\":\"{\\\"city\\\":\\\"北京\\\"}\"}}]}";

    // ==================== 模拟对话历史 ====================

    public static List<ChatMessage> shortHistory() {
        return List.of(
                ChatMessage.user("我最近总是头疼"),
                ChatMessage.assistant("请问头疼持续多久了？"),
                ChatMessage.user("大概三天了")
        );
    }

    public static List<ChatMessage> longHistory() {
        return List.of(
                ChatMessage.user("我最近总是头疼"),
                ChatMessage.assistant("请问头疼持续多久了？是钝痛还是刺痛？"),
                ChatMessage.user("大概三天了，是钝痛"),
                ChatMessage.assistant("了解了。有伴随恶心或视力模糊吗？"),
                ChatMessage.user("没有恶心，但有时候会眼花"),
                ChatMessage.assistant("建议您到神经内科就诊，可能是偏头痛或视疲劳引起的。"),
                ChatMessage.user("需要做CT检查吗？"),
                ChatMessage.assistant("需要医生面诊后判断。如果医生怀疑有颅内问题，可能会建议做CT。"),
                ChatMessage.user("好的，那我明天去挂号"),
                ChatMessage.assistant("好的，祝您早日康复！")
        );
    }
}
