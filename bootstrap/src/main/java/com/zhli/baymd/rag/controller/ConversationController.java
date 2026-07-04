/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zhli.baymd.rag.controller;

import com.zhli.baymd.rag.controller.request.ConversationUpdateRequest;
import com.zhli.baymd.rag.controller.vo.ConversationMessageVO;
import com.zhli.baymd.rag.controller.vo.ConversationVO;
import com.zhli.baymd.framework.context.UserContext;
import com.zhli.baymd.framework.convention.Result;
import com.zhli.baymd.framework.web.Results;
import com.zhli.baymd.rag.dao.mapper.UserEpisodeMapper;
import com.zhli.baymd.rag.dao.mapper.UserEpisodeVectorMapper;
import com.zhli.baymd.rag.dao.mapper.UserFactMapper;
import com.zhli.baymd.rag.dao.mapper.UserFactVectorMapper;
import com.zhli.baymd.rag.enums.ConversationMessageOrder;
import com.zhli.baymd.rag.service.ConversationMessageService;
import com.zhli.baymd.rag.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话控制器
 * 提供会话相关的REST API接口，包括会话列表获取、重命名、删除以及会话消息列表获取等功能
 */
@RestController
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final ConversationMessageService conversationMessageService;
    private final UserFactMapper userFactMapper;
    private final UserFactVectorMapper userFactVectorMapper;
    private final UserEpisodeMapper userEpisodeMapper;
    private final UserEpisodeVectorMapper userEpisodeVectorMapper;

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<List<ConversationVO>> listConversations() {
        return Results.success(conversationService.listByUserId(UserContext.getUserId()));
    }

    /**
     * 重命名会话
     */
    @PutMapping("/conversations/{conversationId}")
    public Result<Void> rename(@PathVariable String conversationId,
                               @RequestBody ConversationUpdateRequest request) {
        conversationService.rename(conversationId, request);
        return Results.success();
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    public Result<Void> delete(@PathVariable String conversationId) {
        conversationService.delete(conversationId);
        return Results.success();
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public Result<List<ConversationMessageVO>> listMessages(@PathVariable String conversationId) {
        return Results.success(conversationMessageService.listMessages(conversationId, UserContext.getUserId(), null, ConversationMessageOrder.ASC));
    }

    /**
     * 导出会话为 JSON 格式（含消息 + 元数据）
     */
    @GetMapping("/conversations/{conversationId}/export")
    public Result<Map<String, Object>> exportConversation(@PathVariable String conversationId) {
        String userId = UserContext.getUserId();
        List<ConversationMessageVO> messages = conversationMessageService.listMessages(
                conversationId, userId, null, ConversationMessageOrder.ASC);
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("conversationId", conversationId);
        export.put("userId", userId);
        export.put("exportedAt", java.time.Instant.now().toString());
        export.put("messageCount", messages.size());
        export.put("messages", messages);
        return Results.success(export);
    }

    /**
     * 清空当前用户所有记忆数据（Facts + Episodes + 向量）
     */
    @DeleteMapping("/memory")
    public Result<Map<String, Object>> clearMemory() {
        String userId = UserContext.getUserId();
        int facts = clearFacts(userId);
        int episodes = clearEpisodes(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("deletedFacts", facts);
        result.put("deletedEpisodes", episodes);
        return Results.success(result);
    }

    private int clearFacts(String userId) {
        var facts = userFactMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhli.baymd.rag.dao.entity.UserFactDO>()
                        .eq(com.zhli.baymd.rag.dao.entity.UserFactDO::getUserId, userId));
        if (!facts.isEmpty()) {
            userFactVectorMapper.deleteByUserId(userId);
            userFactMapper.deleteBatchIds(facts.stream().map(f -> f.getId()).toList());
        }
        return facts.size();
    }

    private int clearEpisodes(String userId) {
        var episodes = userEpisodeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhli.baymd.rag.dao.entity.UserEpisodeDO>()
                        .eq(com.zhli.baymd.rag.dao.entity.UserEpisodeDO::getUserId, userId));
        if (!episodes.isEmpty()) {
            userEpisodeVectorMapper.deleteByUserId(userId);
            userEpisodeMapper.deleteBatchIds(episodes.stream().map(e -> e.getId()).toList());
        }
        return episodes.size();
    }
}
