package com.flowwork.config;

import com.flowwork.modules.knowledge.embedding.LocalHashingEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 相关 Bean
 * SimpleVectorStore：内存向量存储（零外部依赖，重启丢失）
 * Embedding 使用本地实现（DeepSeek 不提供 embedding API）
 * 生产可替换为 MilvusVectorStore + 云端 embedding
 */
@Configuration
public class AiConfig {

    /**
     * 向量存储：基于本地 Hashing Embedding 模型构建内存向量库，供 RAG 知识库检索使用
     */
    @Bean
    public VectorStore vectorStore(LocalHashingEmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
