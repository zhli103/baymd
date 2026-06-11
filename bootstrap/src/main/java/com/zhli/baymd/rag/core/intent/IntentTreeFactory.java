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
    private static final String KB_ID_MEDICAL = "2062148529471643648";

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

        // ---------- 1.2 症状自查 ----------
        IntentNode symptomCheck = IntentNode.builder()
                .id("medical-symptom")
                .kbId(KB_ID_MEDICAL)
                .name("症状自查")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("根据用户描述的症状，分析可能的原因、严重程度评估及就医建议")
                .examples(List.of(
                        "头晕乏力是什么原因？",
                        "胸口闷疼是怎么回事？",
                        "肚子疼拉稀可能是什么病？",
                        "反复发烧是什么原因？"
                ))
                .build();

        IntentNode symptomInternal = IntentNode.builder()
                .id("medical-symptom-internal")
                .kbId(KB_ID_MEDICAL)
                .name("内科症状自查")
                .level(TOPIC)
                .parentId(symptomCheck.getId())
                .kind(IntentKind.KB)
                .description("发热、咳嗽、头痛、头晕、胸闷、心悸、腹痛、腹泻、恶心呕吐、乏力、失眠等内科常见症状的病因分析及就医建议")
                .examples(List.of(
                        "反复低烧是什么毛病？",
                        "头晕乏力没精神怎么回事？",
                        "胃疼拉肚子可能是什么病？"
                ))
                .build();

        IntentNode symptomSurgery = IntentNode.builder()
                .id("medical-symptom-surgery")
                .kbId(KB_ID_MEDICAL)
                .name("外科症状自查")
                .level(TOPIC)
                .parentId(symptomCheck.getId())
                .kind(IntentKind.KB)
                .description("急性腹痛、外伤肿痛、腰腿疼痛、关节肿胀、颈部肿块等外科常见症状的可能病因分析")
                .examples(List.of(
                        "腰疼是什么原因引起的？",
                        "膝盖肿了疼是什么问题？",
                        "脖子摸到肿块可能是什么？"
                ))
                .build();

        IntentNode symptomSkin = IntentNode.builder()
                .id("medical-symptom-skin")
                .kbId(KB_ID_MEDICAL)
                .name("皮肤与五官症状自查")
                .level(TOPIC)
                .parentId(symptomCheck.getId())
                .kind(IntentKind.KB)
                .description("皮疹、瘙痒、红肿、脱发、口腔溃疡、视力模糊、耳鸣、鼻塞流涕等皮肤五官症状的可能病因分析")
                .examples(List.of(
                        "全身起红疹很痒是什么原因？",
                        "突然耳鸣怎么回事？",
                        "嘴巴里反复长溃疡是什么病？"
                ))
                .build();

        IntentNode symptomEmergency = IntentNode.builder()
                .id("medical-symptom-emergency")
                .kbId(KB_ID_MEDICAL)
                .name("急症识别")
                .level(TOPIC)
                .parentId(symptomCheck.getId())
                .kind(IntentKind.KB)
                .description("突发剧烈头痛、胸痛伴呼吸困难、大出血、意识丧失、急性中毒等需要紧急就医的危重症状识别")
                .examples(List.of(
                        "突然剧烈头痛是不是中风？",
                        "胸口剧痛出冷汗是什么问题？",
                        "突然看不清听不见是什么急症？"
                ))
                .build();

        symptomCheck.setChildren(List.of(symptomInternal, symptomSurgery, symptomSkin, symptomEmergency));

        // ---------- 1.3 药物查询 ----------
        IntentNode drugQuery = IntentNode.builder()
                .id("medical-drug")
                .kbId(KB_ID_MEDICAL)
                .name("药物查询")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("查询药品的功效、用法用量、不良反应、禁忌症、注意事项等药物信息")
                .examples(List.of(
                        "布洛芬有什么副作用？",
                        "阿莫西林怎么吃？",
                        "高血压药有什么注意事项？"
                ))
                .build();

        IntentNode drugWestern = IntentNode.builder()
                .id("medical-drug-western")
                .kbId(KB_ID_MEDICAL)
                .name("西药查询")
                .level(TOPIC)
                .parentId(drugQuery.getId())
                .kind(IntentKind.KB)
                .description("感冒药、退烧药、止痛药、抗生素、降压药、降糖药等西药的功效、用法、不良反应、禁忌及相互作用")
                .examples(List.of(
                        "布洛芬一天吃几次？",
                        "阿莫西林和头孢有什么区别？",
                        "降压药需要长期吃吗？"
                ))
                .build();

        IntentNode drugChinese = IntentNode.builder()
                .id("medical-drug-chinese")
                .kbId(KB_ID_MEDICAL)
                .name("中药与中成药")
                .level(TOPIC)
                .parentId(drugQuery.getId())
                .kind(IntentKind.KB)
                .description("板蓝根、连花清瘟、六味地黄丸、逍遥丸等中成药和中药的功效、用法及注意事项")
                .examples(List.of(
                        "板蓝根治什么？",
                        "六味地黄丸有什么功效？",
                        "连花清瘟怎么吃？"
                ))
                .build();

        drugQuery.setChildren(List.of(drugWestern, drugChinese));

        // ---------- 1.4 饮食建议 ----------
        IntentNode dietAdvice = IntentNode.builder()
                .id("medical-diet")
                .kbId(KB_ID_MEDICAL)
                .name("饮食建议")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("根据健康状况提供针对性的饮食调理建议，包括宜食、忌食、食疗方案等")
                .examples(List.of(
                        "高血压吃什么好？",
                        "糖尿病人不能吃什么？",
                        "胃炎吃什么食物养胃？"
                ))
                .build();

        IntentNode dietChronic = IntentNode.builder()
                .id("medical-diet-chronic")
                .kbId(KB_ID_MEDICAL)
                .name("慢病饮食")
                .level(TOPIC)
                .parentId(dietAdvice.getId())
                .kind(IntentKind.KB)
                .description("高血压、高血脂、糖尿病、痛风、肾病等慢性疾病的饮食原则和食谱建议")
                .examples(List.of(
                        "高血压饮食要注意什么？",
                        "痛风不能吃什么？",
                        "糖尿病人水果能吃吗？"
                ))
                .build();

        IntentNode dietDigest = IntentNode.builder()
                .id("medical-diet-digest")
                .kbId(KB_ID_MEDICAL)
                .name("消化调理")
                .level(TOPIC)
                .parentId(dietAdvice.getId())
                .kind(IntentKind.KB)
                .description("胃炎、胃溃疡、便秘、腹泻、消化不良等消化系统问题的饮食调理方案")
                .examples(List.of(
                        "胃炎吃什么食物养胃？",
                        "便秘吃什么能通便？",
                        "胃酸过多不能吃什么？"
                ))
                .build();

        IntentNode dietGeneral = IntentNode.builder()
                .id("medical-diet-general")
                .kbId(KB_ID_MEDICAL)
                .name("日常营养")
                .level(TOPIC)
                .parentId(dietAdvice.getId())
                .kind(IntentKind.KB)
                .description("免疫力提升、减肥、孕期营养、儿童营养、老年人饮食等日常饮食建议")
                .examples(List.of(
                        "吃什么能提高免疫力？",
                        "孕妇不能吃什么？",
                        "减肥期间怎么吃？"
                ))
                .build();

        dietAdvice.setChildren(List.of(dietChronic, dietDigest, dietGeneral));

        // ---------- 1.5 中医辨证 ----------
        IntentNode tcmDiagnosis = IntentNode.builder()
                .id("medical-tcm")
                .kbId(KB_ID_MEDICAL)
                .name("中医辨证")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("从中医角度对症状进行辨证分析，包括八纲辨证、脏腑辨证、气血津液辨证等，给出中医调理建议")
                .examples(List.of(
                        "中医怎么看失眠？",
                        "手脚冰凉是什么虚？",
                        "老是上火怎么回事？",
                        "面色发黄、乏力中医怎么说？"
                ))
                .build();

        IntentNode tcmConstitution = IntentNode.builder()
                .id("medical-tcm-constitution")
                .kbId(KB_ID_MEDICAL)
                .name("体质辨识")
                .level(TOPIC)
                .parentId(tcmDiagnosis.getId())
                .kind(IntentKind.KB)
                .description("平和质、气虚质、阳虚质、阴虚质、痰湿质、湿热质、血瘀质、气郁质、特禀质九种体质的辨识特征及调养方法")
                .examples(List.of(
                        "怎么判断自己是什么体质？",
                        "手脚冰凉虚是什么体质？",
                        "总是上火是阴虚吗？"
                ))
                .build();

        IntentNode tcmSymptom = IntentNode.builder()
                .id("medical-tcm-symptom")
                .kbId(KB_ID_MEDICAL)
                .name("常见症状辨证")
                .level(TOPIC)
                .parentId(tcmDiagnosis.getId())
                .kind(IntentKind.KB)
                .description("失眠、乏力、头晕、口干口苦、便秘、月经不调等常见症状的中医辨证分型、病因病机和调理建议")
                .examples(List.of(
                        "中医怎么看失眠多梦？",
                        "老是口干口苦是什么原因？",
                        "便秘中医怎么调理？"
                ))
                .build();

        IntentNode tcmDiet = IntentNode.builder()
                .id("medical-tcm-diet")
                .kbId(KB_ID_MEDICAL)
                .name("中医食疗")
                .level(TOPIC)
                .parentId(tcmDiagnosis.getId())
                .kind(IntentKind.KB)
                .description("药食同源原则、四季食疗、体质食疗、常见药膳推荐等中医饮食调理知识")
                .examples(List.of(
                        "冬天吃什么补身体？",
                        "气虚体质吃什么好？",
                        "湿气重要吃什么？"
                ))
                .build();

        tcmDiagnosis.setChildren(List.of(tcmConstitution, tcmSymptom, tcmDiet));

        // ---------- 1.6 报告解读 ----------
        IntentNode reportInterp = IntentNode.builder()
                .id("medical-report")
                .kbId(KB_ID_MEDICAL)
                .name("报告解读")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("分析解读体检报告中的各项指标，包括血常规、肝功能、肾功能、血脂、血糖、尿常规等，提醒异常指标和潜在健康风险")
                .examples(List.of(
                        "帮我看一下这个体检报告",
                        "转氨酶升高是什么意思？",
                        "血脂报告怎么看？",
                        "尿酸偏高是什么问题？"
                ))
                .build();

        IntentNode reportBlood = IntentNode.builder()
                .id("medical-report-blood")
                .kbId(KB_ID_MEDICAL)
                .name("血常规")
                .level(TOPIC)
                .parentId(reportInterp.getId())
                .kind(IntentKind.KB)
                .description("白细胞、红细胞、血红蛋白、血小板、中性粒细胞等血常规指标的解读和异常分析")
                .examples(List.of(
                        "白细胞偏高是什么原因？",
                        "血红蛋白偏低怎么回事？",
                        "血小板减少是什么问题？"
                ))
                .build();

        IntentNode reportBiochem = IntentNode.builder()
                .id("medical-report-biochem")
                .kbId(KB_ID_MEDICAL)
                .name("生化指标")
                .level(TOPIC)
                .parentId(reportInterp.getId())
                .kind(IntentKind.KB)
                .description("肝功能（ALT、AST、GGT）、肾功能（肌酐、尿素）、血脂（胆固醇、甘油三酯）、血糖、尿酸等生化指标的解读")
                .examples(List.of(
                        "转氨酶升高是什么意思？",
                        "肌酐偏高怎么回事？",
                        "总胆固醇高了怎么办？"
                ))
                .build();

        IntentNode reportUrine = IntentNode.builder()
                .id("medical-report-urine")
                .kbId(KB_ID_MEDICAL)
                .name("尿常规与其他")
                .level(TOPIC)
                .parentId(reportInterp.getId())
                .kind(IntentKind.KB)
                .description("尿常规、甲状腺功能、肿瘤标志物、心电图等常见检查项目的指标解读")
                .examples(List.of(
                        "尿蛋白阳性什么意思？",
                        "促甲状腺激素偏高是什么问题？",
                        "肿瘤标志物升高是癌症吗？"
                ))
                .build();

        reportInterp.setChildren(List.of(reportBlood, reportBiochem, reportUrine));

        // ---------- 1.7 医院推荐 ----------
        IntentNode hospitalRec = IntentNode.builder()
                .id("medical-hospital")
                .kbId(KB_ID_MEDICAL)
                .name("医院推荐")
                .level(CATEGORY)
                .parentId(medical.getId())
                .kind(IntentKind.KB)
                .description("根据疾病类型、就诊需求帮助用户了解如何选择适合的医院，包括医院等级、专科优势、就诊流程等")
                .examples(List.of(
                        "心脏病去什么医院好？",
                        "三甲医院和二甲医院有什么区别？",
                        "去大医院看病流程是怎样的？"
                ))
                .build();

        IntentNode hospitalLevel = IntentNode.builder()
                .id("medical-hospital-level")
                .kbId(KB_ID_MEDICAL)
                .name("医院等级与选择")
                .level(TOPIC)
                .parentId(hospitalRec.getId())
                .kind(IntentKind.KB)
                .description("三甲、三乙、二甲等医院等级划分标准，各级医院服务能力说明，以及根据不同病情选择合适等级医院的建议")
                .examples(List.of(
                        "三甲医院是什么意思？",
                        "二甲医院能看什么病？",
                        "小病去社区医院还是大医院？"
                ))
                .build();

        IntentNode hospitalDept = IntentNode.builder()
                .id("medical-hospital-dept")
                .kbId(KB_ID_MEDICAL)
                .name("专科医院推荐")
                .level(TOPIC)
                .parentId(hospitalRec.getId())
                .kind(IntentKind.KB)
                .description("心血管、肿瘤、骨科、神经、儿科、妇产等各专科领域知名医院及选择建议")
                .examples(List.of(
                        "心脏病去哪个医院好？",
                        "肿瘤治疗哪家医院强？",
                        "骨科手术去什么医院？"
                ))
                .build();

        IntentNode hospitalVisit = IntentNode.builder()
                .id("medical-hospital-visit")
                .kbId(KB_ID_MEDICAL)
                .name("就诊指南")
                .level(TOPIC)
                .parentId(hospitalRec.getId())
                .kind(IntentKind.KB)
                .description("挂号方式、就诊流程、医保使用、入院准备、异地就医等就医实用指南")
                .examples(List.of(
                        "去医院看病要带什么？",
                        "怎么挂专家号？",
                        "异地就医医保怎么报销？"
                ))
                .build();

        hospitalRec.setChildren(List.of(hospitalLevel, hospitalDept, hospitalVisit));

        medical.setChildren(List.of(deptRecommend, symptomCheck, drugQuery, dietAdvice, tcmDiagnosis, reportInterp, hospitalRec));
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
