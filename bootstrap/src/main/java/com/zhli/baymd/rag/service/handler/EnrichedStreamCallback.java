package com.zhli.baymd.rag.service.handler;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import com.zhli.baymd.infra.chat.StreamCallback;
import com.zhli.baymd.rag.core.eval.QualityEvaluator;
import com.zhli.baymd.rag.core.followup.CitationCollector;
import com.zhli.baymd.rag.core.followup.FollowUpGenerator;
import com.zhli.baymd.rag.core.memory.extract.MemoryExtractionOrchestrator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 增强型 StreamCallback 装饰器 — 在回答完成时自动追加引用和推荐追问。
 */
@Slf4j
public class EnrichedStreamCallback implements StreamCallback {

    private final StreamCallback delegate;
    private final FollowUpGenerator followUpGenerator;
    private final QualityEvaluator qualityEvaluator;
    private final MemoryExtractionOrchestrator memoryOrchestrator;
    private final String question;
    private final String userId;
    private final String conversationId;
    private final Map<String, List<RetrievedChunk>> intentChunks;
    private final StringBuilder answer = new StringBuilder();

    public EnrichedStreamCallback(StreamCallback delegate,
                                   FollowUpGenerator followUpGenerator,
                                   QualityEvaluator qualityEvaluator,
                                   MemoryExtractionOrchestrator memoryOrchestrator,
                                   String question,
                                   String userId,
                                   String conversationId,
                                   Map<String, List<RetrievedChunk>> intentChunks) {
        this.delegate = delegate;
        this.followUpGenerator = followUpGenerator;
        this.qualityEvaluator = qualityEvaluator;
        this.memoryOrchestrator = memoryOrchestrator;
        this.question = question;
        this.userId = userId;
        this.conversationId = conversationId;
        this.intentChunks = intentChunks;
    }

    @Override
    public void onContent(String content) {
        answer.append(content);
        delegate.onContent(content);
    }

    @Override
    public void onThinking(String content) {
        delegate.onThinking(content);
    }

    @Override
    public void onComplete() {
        String answerText = answer.toString();

        // 1. 收集引用
        CitationCollector collector = new CitationCollector();
        collector.collect(intentChunks);
        List<CitationCollector.Citation> citations = collector.toCitationList();

        // 2. 生成追问（同步，耗时短）
        List<String> followUps = List.of();
        if (followUpGenerator != null && !answerText.isBlank()) {
            try {
                followUps = followUpGenerator.generate(question, answerText, null);
            } catch (Exception e) {
                log.warn("追问生成失败", e);
            }
        }

        // 3. 异步质量评估
        if (qualityEvaluator != null && !answerText.isBlank()) {
            qualityEvaluator.evaluateAsync(question, answerText, null)
                    .thenAccept(score -> log.info("质量评估完成: {}", score.toSummary()));
        }

        // 4. 异步记忆提取（提取 Fact + Episode，不阻塞响应）
        if (memoryOrchestrator != null && !answerText.isBlank()) {
            memoryOrchestrator.extractAsync(question, answerText, userId, conversationId, null);
        }

        // 5. 委托原 onComplete（发送 FINISH + DONE）
        delegate.onComplete();
    }

    @Override
    public void onError(Throwable error) {
        delegate.onError(error);
    }
}
