package com.flowwork.modules.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本滑动分块：按字符长度切块，块间保留重叠
 * 设计文档参数：chunkSize=800 字符，overlap=100
 */
@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 800;
    private static final int OVERLAP = 100;

    public List<String> split(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;
        String normalized = text.replaceAll("\\r", "").trim();
        int len = normalized.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(start + CHUNK_SIZE, len);
            // 尽量在换行/句号处切分，避免截断语义
            if (end < len) {
                int boundary = Math.max(
                        normalized.lastIndexOf("\n\n", end),
                        Math.max(normalized.lastIndexOf("。", end), normalized.lastIndexOf(". ", end)));
                if (boundary > start + CHUNK_SIZE / 2) {
                    end = boundary + 1;
                }
            }
            chunks.add(normalized.substring(start, end).trim());
            if (end >= len) break;
            start = Math.max(0, end - OVERLAP);
        }
        return chunks;
    }
}
