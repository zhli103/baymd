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

import com.zhli.baymd.ingestion.service.IntentTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 医学意图树启动初始化器
 * <p>
 * 应用启动时自动检查意图树是否已初始化，若未初始化则从 {@code IntentTreeFactory} 构建医学意图树并写入数据库。
 * 可通过 {@code app.medical.intent-tree.auto-init} 配置项控制是否启用（默认启用）。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalIntentTreeInitializer implements CommandLineRunner {

    private final IntentTreeService intentTreeService;

    /**
     * 是否启用自动初始化（可通过配置覆盖）
     */
    @org.springframework.beans.factory.annotation.Value("${app.medical.intent-tree.auto-init:true}")
    private boolean autoInit;

    @Override
    public void run(String... args) {
        if (!autoInit) {
            log.info("医学意图树自动初始化已禁用（app.medical.intent-tree.auto-init=false）");
            return;
        }

        try {
            int created = intentTreeService.initFromFactory();
            if (created > 0) {
                log.info("医学意图树初始化完成，新增 {} 个节点", created);
            } else {
                log.info("医学意图树已存在，跳过初始化");
            }
        } catch (Exception e) {
            log.error("医学意图树初始化失败，可稍后通过 API 手动初始化", e);
        }
    }
}
