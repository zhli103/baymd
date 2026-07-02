package com.zhli.baymd.rag.core.agent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.zhli.baymd.framework.convention.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Checkpoint 持久化服务 — 支持 Agent 执行中断后恢复。
 *
 * <p>使用 Redis String 存储 JSON 序列化的消息列表，TTL 24h。</p>
 */
@Slf4j
@Service
public class CheckpointService {

    private static final String KEY_PREFIX = "baymd:checkpoint:";
    private static final Duration TTL = Duration.ofHours(24);
    private static final Gson GSON = new Gson();

    private final StringRedisTemplate redisTemplate;

    public CheckpointService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存 Checkpoint（JSON 序列化到 Redis String）
     */
    public void save(String conversationId, List<ChatMessage> messages,
                     int stepCount, String runId) {
        CheckpointData data = CheckpointData.builder()
                .conversationId(conversationId)
                .messages(copyMessages(messages))
                .stepCount(stepCount)
                .runId(runId)
                .savedAt(Instant.now())
                .build();

        String key = checkpointKey(conversationId);
        redisTemplate.opsForValue().set(key, GSON.toJson(data), TTL);
        log.info("Checkpoint 已保存: conversationId={}, steps={}, runId={}",
                conversationId, stepCount, runId);
    }

    /**
     * 加载 Checkpoint
     */
    public Optional<CheckpointData> load(String conversationId) {
        String key = checkpointKey(conversationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json != null && !json.isEmpty()) {
            try {
                CheckpointData data = GSON.fromJson(json, CheckpointData.class);
                log.info("Checkpoint 已加载: conversationId={}, steps={}, runId={}",
                        conversationId, data.getStepCount(), data.getRunId());
                return Optional.of(data);
            } catch (Exception e) {
                log.warn("Checkpoint 反序列化失败: conversationId={}", conversationId, e);
            }
        }
        return Optional.empty();
    }

    /**
     * 删除 Checkpoint
     */
    public void delete(String conversationId) {
        redisTemplate.delete(checkpointKey(conversationId));
        log.debug("Checkpoint 已删除: conversationId={}", conversationId);
    }

    /**
     * 检查是否存在 Checkpoint
     */
    public boolean exists(String conversationId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(checkpointKey(conversationId)));
    }

    private String checkpointKey(String conversationId) {
        return KEY_PREFIX + conversationId;
    }

    /**
     * 深拷贝消息列表（避免外部修改影响 checkpoint）
     */
    private static List<ChatMessage> copyMessages(List<ChatMessage> original) {
        if (original == null) return new ArrayList<>();
        List<ChatMessage> copy = new ArrayList<>(original.size());
        for (ChatMessage msg : original) {
            ChatMessage copied = new ChatMessage();
            copied.setRole(msg.getRole());
            copied.setContent(msg.getContent());
            copied.setThinkingContent(msg.getThinkingContent());
            copied.setThinkingDuration(msg.getThinkingDuration());
            copy.add(copied);
        }
        return copy;
    }

    // ==================== 数据模型 ====================

    /**
     * Checkpoint 快照数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckpointData {
        /** 会话 ID */
        private String conversationId;

        /** 当前消息列表（含 system, history, user, assistant, tool_result） */
        private List<ChatMessage> messages;

        /** 已执行的 ReAct 步数 */
        private int stepCount;

        /** ReAct 运行 ID */
        private String runId;

        /** 保存时间 */
        private Instant savedAt;

        /** 是否已恢复过 */
        @Builder.Default
        private boolean resumed = false;
    }
}
