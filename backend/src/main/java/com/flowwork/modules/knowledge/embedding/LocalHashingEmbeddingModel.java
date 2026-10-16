package com.flowwork.modules.knowledge.embedding;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地哈希向量化模型（离线可用，无需任何外部 API）。
 * 背景：DeepSeek 官方 API 不提供 embedding 接口，知识库 RAG 改用本地实现。
 * 原理：分词（中文按单字、英文按词）→ 双哈希映射到固定维度 → TF 加权 → L2 归一化。
 * 效果：对关键词级语义检索有效，可支撑 SimpleVectorStore 的相似度排序。
 */
@Primary
@Component
public class LocalHashingEmbeddingModel implements EmbeddingModel {

    private static final int DIM = 512;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> instructions = request.getInstructions();
        List<Embedding> results = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            results.add(new Embedding(embed(instructions.get(i)), i));
        }
        return new EmbeddingResponse(results);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIM];
        if (text == null || text.isBlank()) {
            return vector;
        }
        for (String token : tokenize(text)) {
            // 主哈希 + 辅助哈希降低碰撞影响
            int h1 = Math.floorMod(token.hashCode(), DIM);
            vector[h1] += 1f;
            int h2 = Math.floorMod((token + "#2").hashCode(), DIM);
            if (h2 != h1) {
                vector[h2] += 0.5f;
            }
        }
        // L2 归一化，保证余弦相似度可比
        double norm = 0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < DIM; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    /** 分词：CJK 字符按单字切，连续字母/数字作为词，统一小写 */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                if (word.length() > 0) {
                    tokens.add(word.toString().toLowerCase());
                    word.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else if (word.length() > 0) {
                tokens.add(word.toString().toLowerCase());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            tokens.add(word.toString().toLowerCase());
        }
        return tokens;
    }
}
