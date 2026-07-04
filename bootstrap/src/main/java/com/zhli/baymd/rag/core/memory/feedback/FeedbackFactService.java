package com.zhli.baymd.rag.core.memory.feedback;

import com.zhli.baymd.rag.dao.entity.UserFactDO;
import com.zhli.baymd.rag.dao.mapper.UserFactMapper;
import com.zhli.baymd.rag.dao.mapper.UserFactVectorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 反馈-Fact 联动服务 — 用户点踩时自动降级/清理关联事实。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackFactService {

    private final UserFactMapper factMapper;
    private final UserFactVectorMapper factVectorMapper;

    private static final int DISLIKE_PURGE_THRESHOLD = 3;

    /**
     * 用户点踩时调用：降级关联 Facts 的 confidence。
     */
    public void onDislike(String messageId, String userId) {
        List<UserFactDO> facts = factMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserFactDO>()
                        .eq(UserFactDO::getSourceMsgId, messageId)
                        .eq(UserFactDO::getUserId, userId));

        if (facts.isEmpty()) return;

        for (UserFactDO fact : facts) {
            float newConf = Math.max(0.1f, fact.getConfidence() * 0.5f);
            fact.setConfidence(newConf);
            factMapper.updateById(fact);
        }
        log.info("反馈联动: 点踩 → {} 条 Fact 降级, messageId={}", facts.size(), messageId);

        // 检查是否需要彻底清理
        purgeIfNeeded(userId);
    }

    /**
     * 用户点赞时调用：提升关联 Facts 的 confidence。
     */
    public void onLike(String messageId, String userId) {
        List<UserFactDO> facts = factMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserFactDO>()
                        .eq(UserFactDO::getSourceMsgId, messageId)
                        .eq(UserFactDO::getUserId, userId));

        for (UserFactDO fact : facts) {
            float newConf = Math.min(1.0f, fact.getConfidence() * 1.2f);
            fact.setConfidence(newConf);
            factMapper.updateById(fact);
        }
        if (!facts.isEmpty()) {
            log.debug("反馈联动: 点赞 → {} 条 Fact 置信度提升", facts.size());
        }
    }

    /**
     * 同类型频繁点踩 → 清理该类型所有低置信度 Fact。
     */
    private void purgeIfNeeded(String userId) {
        List<UserFactDO> all = factMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserFactDO>()
                        .eq(UserFactDO::getUserId, userId));

        // 按类型分组，统计低置信度 (<0.3) 的数量
        Map<String, Long> lowConfCounts = all.stream()
                .filter(f -> f.getConfidence() < 0.3f)
                .collect(Collectors.groupingBy(UserFactDO::getFactType, Collectors.counting()));

        for (var entry : lowConfCounts.entrySet()) {
            if (entry.getValue() >= DISLIKE_PURGE_THRESHOLD) {
                List<String> ids = all.stream()
                        .filter(f -> f.getFactType().equals(entry.getKey()) && f.getConfidence() < 0.3f)
                        .map(UserFactDO::getId).toList();
                factMapper.deleteBatchIds(ids);
                ids.forEach(factVectorMapper::deleteById); // 同步清理向量
                log.info("反馈联动: 清理 {} 条低置信度 Fact, type={}", ids.size(), entry.getKey());
            }
        }
    }
}
