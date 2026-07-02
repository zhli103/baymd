package com.zhli.baymd.rag.service.pipeline;

import com.zhli.baymd.rag.core.guidance.GuidanceDecision;
import com.zhli.baymd.rag.core.guidance.IntentGuidanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 歧义澄清执行器 — 用户问题不够明确时，生成追问引导而非直接回答。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClarificationExecutor implements ConversationExecutor {

    private final IntentGuidanceService guidanceService;

    @Override
    public ExecutionMode getMode() {
        return ExecutionMode.CLARIFICATION;
    }

    @Override
    public boolean supports(StreamChatContext ctx) {
        GuidanceDecision decision = guidanceService.detectAmbiguity(
                ctx.getRewriteResult() != null ? ctx.getRewriteResult().rewrittenQuestion() : ctx.getQuestion(),
                ctx.getSubIntents()
        );
        return decision.isPrompt();
    }

    @Override
    public void execute(StreamChatContext ctx) {
        GuidanceDecision decision = guidanceService.detectAmbiguity(
                ctx.getRewriteResult() != null ? ctx.getRewriteResult().rewrittenQuestion() : ctx.getQuestion(),
                ctx.getSubIntents()
        );
        log.info("歧义澄清执行器: prompt={}", decision.getPrompt() != null
                ? decision.getPrompt().substring(0, Math.min(50, decision.getPrompt().length())) : "null");
        ctx.getCallback().onContent(decision.getPrompt());
        ctx.getCallback().onComplete();
    }
}
