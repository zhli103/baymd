package com.zhli.baymd.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 证据预算配置属性
 *
 * <p>在 application.yaml 中配置：</p>
 * <pre>
 * rag.evidence:
 *   max-tokens: 4096
 *   min-confidence-score: 0.3
 *   allocate-by-intent: true
 *   min-chunks-per-intent: 1
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "rag.evidence")
public class EvidenceBudgetProperties {

    /** 证据上下文的 Token 总预算（默认 4096 约等于 8K 中文） */
    private int maxTokens = 4096;

    /** 检索结果最低置信度阈值，低于此分值的 chunk 将被丢弃 */
    private double minConfidenceScore = 0.3;

    /** 是否按意图比例分配预算（false = 按分数排序统一截断） */
    private boolean allocateByIntent = true;

    /** 每个意图至少保留的 chunk 数量（ensure minimum coverage） */
    private int minChunksPerIntent = 1;

    /** 无证据时的短路响应文本 */
    private String noEvidenceResponse = "抱歉，知识库中未检索到与您问题相关的信息，无法回答此问题。";
}
