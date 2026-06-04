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

package com.zhli.baymd.rag.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhli.baymd.knowledge.dao.entity.KnowledgeBaseDO;
import com.zhli.baymd.knowledge.dao.mapper.KnowledgeBaseMapper;
import com.zhli.baymd.rag.dao.entity.IntentNodeDO;
import com.zhli.baymd.rag.dao.mapper.IntentNodeMapper;
import com.zhli.baymd.ingestion.service.IntentTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 医学意图树启动初始化器
 * <p>
 * 应用启动时自动检查意图树是否已初始化，若未初始化则从 {@code IntentTreeFactory} 构建医学意图树并写入数据库。
 * 同时将新节点的 kbId 修正为数据库中实际存在的医学科知识库 ID。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalIntentTreeInitializer implements CommandLineRunner {

    private static final String PLACEHOLDER_KB_ID = "9100000000000000001";

    private final IntentTreeService intentTreeService;
    private final IntentNodeMapper intentNodeMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @org.springframework.beans.factory.annotation.Value("${app.medical.intent-tree.auto-init:true}")
    private boolean autoInit;

    @Override
    public void run(String... args) {
        if (!autoInit) {
            log.info("医学意图树自动初始化已禁用");
            return;
        }

        try {
            int created = intentTreeService.initFromFactory();
            if (created > 0) {
                log.info("医学意图树初始化完成，新增 {} 个节点", created);
            } else {
                log.info("医学意图树无新增节点");
            }
            fixPlaceholderKbIds();
        } catch (Exception e) {
            log.error("医学意图树初始化失败", e);
        }
    }

    /**
     * 将工厂占位 KB ID 修正为数据库中实际存在的医学科知识库 ID
     */
    private void fixPlaceholderKbIds() {
        // 查找真实KB
        KnowledgeBaseDO realKb = knowledgeBaseMapper.selectList(
                new LambdaQueryWrapper<KnowledgeBaseDO>()
                        .eq(KnowledgeBaseDO::getDeleted, 0)
                        .isNotNull(KnowledgeBaseDO::getId)
                        .last("LIMIT 1")
        ).stream().findFirst().orElse(null);

        if (realKb == null) {
            log.debug("无可用知识库，跳过 kbId 修正");
            return;
        }

        // 更新占位 KB ID
        int updated = intentNodeMapper.update(
                null,
                new LambdaUpdateWrapper<IntentNodeDO>()
                        .eq(IntentNodeDO::getKbId, PLACEHOLDER_KB_ID)
                        .eq(IntentNodeDO::getDeleted, 0)
                        .set(IntentNodeDO::getKbId, realKb.getId())
                        .set(IntentNodeDO::getCollectionName, realKb.getCollectionName())
        );
        if (updated > 0) {
            log.info("已将 {} 个意图节点的 kbId 从占位 {} 修正为 {}", updated, PLACEHOLDER_KB_ID, realKb.getId());
        }
    }
}
