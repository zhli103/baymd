package com.zhli.baymd.rag.core.followup;

import com.zhli.baymd.framework.convention.RetrievedChunk;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 引用来源收集器 — 从检索结果中提取引用列表，在 SSE 结束帧中返回。
 */
@Getter
public class CitationCollector {

    private final Map<String, RetrievedChunk> sources = new LinkedHashMap<>();

    /**
     * 从检索结果中添加引用
     */
    public void collect(Map<String, List<RetrievedChunk>> intentChunks) {
        if (intentChunks == null) return;
        for (List<RetrievedChunk> chunks : intentChunks.values()) {
            for (RetrievedChunk chunk : chunks) {
                if (chunk.getId() != null && !sources.containsKey(chunk.getId())) {
                    sources.put(chunk.getId(), chunk);
                }
            }
        }
    }

    /**
     * 生成本次回答的引用列表
     */
    public List<Citation> toCitationList() {
        int idx = 1;
        List<Citation> list = new ArrayList<>();
        for (RetrievedChunk chunk : sources.values()) {
            String snippet = chunk.getText() != null
                    ? chunk.getText().substring(0, Math.min(100, chunk.getText().length()))
                    : "";
            list.add(new Citation(idx++, chunk.getId(), snippet,
                    chunk.getScore() != null ? chunk.getScore() : 0f));
        }
        return list;
    }

    public boolean isEmpty() { return sources.isEmpty(); }
    public int size() { return sources.size(); }

    public record Citation(int index, String id, String snippet, float score) {}
}
