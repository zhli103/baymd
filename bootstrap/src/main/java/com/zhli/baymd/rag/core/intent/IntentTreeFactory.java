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

package com.zhli.baymd.rag.core.intent;

import com.zhli.baymd.rag.enums.IntentKind;

import java.util.ArrayList;
import java.util.List;

import static com.zhli.baymd.rag.enums.IntentLevel.CATEGORY;
import static com.zhli.baymd.rag.enums.IntentLevel.DOMAIN;
import static com.zhli.baymd.rag.enums.IntentLevel.TOPIC;

/**
 * 医学健康意图树工厂
 * <p>
 * 构建面向 C 端用户的医学问答助手意图识别树，覆盖科室推荐、症状自查、药物查询等场景。
 * 意图树通过 {@code IntentTreeServiceImpl.initFromFactory()} 写入数据库，
 * 启动后可通过 {@code /intent-tree} API 动态管理。
 * </p>
 */
public class IntentTreeFactory {

    /**
     * 医学科知识库 ID（需与数据库中 t_knowledge_base.id 一致）
     */
    private static final String KB_ID_MEDICAL = "9100000000000000001";

    /**
     * 构建医学健康意图树
     */
    public static List<IntentNode> buildIntentTree() {
        List<IntentNode> roots = new ArrayList<>();

        // ========== 1. 医学健康 ==========
        IntentNode medical = IntentNode.builder()
                .id("medical")
                .kbId(KB_ID_MEDICAL)
                .name("医学健康")
                .level(DOMAIN)
                .kind(IntentKind.KB)
                .build();

        // ---------- 1.1 科室推荐 ----------
        IntentNode deptRecommend = IntentNode.builder()
                .id("medical-dept")
                .kbId(KB_ID_MEDICAL)
                .name("科室推荐")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("根据用户描述的症状，推荐适合的就诊科室")
                .examples(List.of(
                        "头疼应该挂什么科？",
                        "胃不舒服看哪个科室？",
                        "腰疼挂什么科？",
                        "皮肤长红疹去哪个科？"
                ))
                .build();

        IntentNode deptInternal = IntentNode.builder()
                .id("medical-dept-internal")
                .kbId(KB_ID_MEDICAL)
                .name("内科症状")
                .level(TOPIC)
                .parentId(deptRecommend.getId())
                .kind(IntentKind.KB)
                .description("头痛、发热、咳嗽、胸闷、心悸、腹痛、腹泻、恶心、呕吐、便秘、头晕、失眠等内科常见症状对应的科室推荐")
                .examples(List.of(
                        "头疼头晕挂什么科？",
                        "咳嗽胸闷看哪个科室？",
                        "胃疼拉肚子挂什么科？",
                        "心慌心悸去哪个科？"
                ))
                .build();

        IntentNode deptSurgery = IntentNode.builder()
                .id("medical-dept-surgery")
                .kbId(KB_ID_MEDICAL)
                .name("外科症状")
                .level(TOPIC)
                .parentId(deptRecommend.getId())
                .kind(IntentKind.KB)
                .description("外伤、骨折、扭伤、急性腹痛、腰腿痛、甲状腺结节等外科常见症状对应的科室推荐")
                .examples(List.of(
                        "脚扭伤了挂什么科？",
                        "腰疼腿麻去哪个科？",
                        "摔伤骨折挂什么科？"
                ))
                .build();

        IntentNode deptSkin = IntentNode.builder()
                .id("medical-dept-skin")
                .kbId(KB_ID_MEDICAL)
                .name("皮肤与五官症状")
                .level(TOPIC)
                .parentId(deptRecommend.getId())
                .kind(IntentKind.KB)
                .description("皮疹、瘙痒、红肿、脱发、痤疮、耳鸣、鼻塞、喉咙痛、视力模糊等皮肤与五官科症状对应的科室推荐")
                .examples(List.of(
                        "皮肤瘙痒起红疹去哪个科？",
                        "耳鸣听力下降挂什么科？",
                        "脸上长痘痘看什么科？"
                ))
                .build();

        IntentNode deptObgynPed = IntentNode.builder()
                .id("medical-dept-obgyn-ped")
                .kbId(KB_ID_MEDICAL)
                .name("妇产儿症状")
                .level(TOPIC)
                .parentId(deptRecommend.getId())
                .kind(IntentKind.KB)
                .description("月经不调、痛经、孕产相关、小儿发热、小儿咳嗽、小儿腹泻等妇产科和儿科常见症状对应的科室推荐")
                .examples(List.of(
                        "月经不调挂什么科？",
                        "孩子发烧去哪个科？",
                        "孕检挂什么科？"
                ))
                .build();

        deptRecommend.setChildren(List.of(deptInternal, deptSurgery, deptSkin, deptObgynPed));
        medical.setChildren(List.of(deptRecommend));
        roots.add(medical);

        // ========== 2. 系统交互 ==========
        IntentNode sys = IntentNode.builder()
                .id("medical-sys")
                .name("系统交互")
                .level(DOMAIN)
                .kind(IntentKind.SYSTEM)
                .build();

        IntentNode welcome = IntentNode.builder()
                .id("medical-sys-welcome")
                .name("欢迎与问候")
                .level(CATEGORY)
                .parentId(sys.getId())
                .kind(IntentKind.SYSTEM)
                .description("用户与健康助手打招呼，如：你好、早上好、hi、在吗等")
                .examples(List.of(
                        "你好",
                        "hello",
                        "早上好",
                        "在吗",
                        "嗨"
                ))
                .build();

        IntentNode aboutBot = IntentNode.builder()
                .id("medical-sys-about")
                .name("关于助手")
                .level(CATEGORY)
                .parentId(sys.getId())
                .kind(IntentKind.SYSTEM)
                .description("询问助手是做什么的、能提供什么帮助等")
                .examples(List.of(
                        "你是谁",
                        "你能做什么",
                        "你能帮我什么",
                        "你是什么AI"
                ))
                .build();

        sys.setChildren(List.of(welcome, aboutBot));
        roots.add(sys);

        // 填充全路径
        fillFullPath(roots, null);
        return roots;
    }

    private static void fillFullPath(List<IntentNode> nodes, IntentNode parent) {
        for (IntentNode node : nodes) {
            if (parent == null) {
                node.setFullPath(node.getName());
            } else {
                node.setFullPath(parent.getFullPath() + " > " + node.getName());
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                fillFullPath(node.getChildren(), node);
            }
        }
    }
}
