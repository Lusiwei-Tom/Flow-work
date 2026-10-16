package com.flowwork.modules.knowledge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.knowledge.entity.KnowledgeDoc;
import com.flowwork.modules.knowledge.mapper.KnowledgeDocMapper;
import com.flowwork.modules.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务：上传 → 解析 → MD5 去重 → 分块 → 向量化 → VectorStore
 * RAG：向量检索 Top5 → 拼上下文 → ChatModel 回答
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeDocMapper docMapper;
    private final DocumentParser parser;
    private final TextChunker chunker;
    private final VectorStore vectorStore;
    private final ProjectAccessService accessService;

    private static final int TOP_K = 5;

    /** 上传文档并构建向量索引 */
    public KnowledgeDoc upload(Long projectId, MultipartFile file, Long userId) {
        accessService.requireWritable(projectId, userId);// 校验用户是否有写入权限
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件读取失败");
        }
        String hash = DigestUtils.md5DigestAsHex(bytes);// 计算文件内容的 MD5 哈希值

        // 同项目 + 同 hash 去重
        KnowledgeDoc dup = docMapper.selectOne(new LambdaQueryWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getProjectId, projectId)
                .eq(KnowledgeDoc::getContentHash, hash)
                .eq(KnowledgeDoc::getStatus, "READY").last("LIMIT 1"));
        if (dup != null) {
            throw new BusinessException(ResultCode.BIZ_CONFLICT, "相同内容文档已存在：" + dup.getFileName());
        }

        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setProjectId(projectId);
        doc.setFileName(fileName);
        doc.setTitle(fileName);
        doc.setContentHash(hash);
        doc.setSizeBytes((long) bytes.length);
        doc.setStatus("PROCESSING");
        doc.setVersion(1);
        doc.setCreatedBy(userId);
        docMapper.insert(doc);

        try {
            String text = parser.parse(fileName, bytes);
            if (text.isBlank()) throw new IllegalArgumentException("文档内容为空");
            List<String> chunks = chunker.split(text);

            List<Document> documents = new ArrayList<>();
            List<String> vectorIds = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String vectorId = "p" + projectId + "_d" + doc.getId() + "_c" + i;
                vectorIds.add(vectorId);
                documents.add(new Document(vectorId, chunks.get(i), Map.of(
                        "projectId", String.valueOf(projectId),
                        "docId", String.valueOf(doc.getId()),
                        "fileName", fileName,
                        "chunkIndex", i
                )));
            }
            vectorStore.add(documents);

            doc.setContentText(text.length() > 50000 ? text.substring(0, 50000) : text);
            doc.setVectorIds(String.join(",", vectorIds));
            doc.setChunkCount(chunks.size());
            doc.setStatus("READY");
            docMapper.updateById(doc);
            log.info("文档向量化完成 docId={} chunks={}", doc.getId(), chunks.size());
            return doc;
        } catch (Exception e) {
            log.error("文档处理失败 docId={}", doc.getId(), e);
            doc.setStatus("FAILED");
            doc.setErrorMsg(e.getMessage());
            docMapper.updateById(doc);
            throw new BusinessException(ResultCode.SERVER_ERROR, "文档处理失败: " + e.getMessage());
        }
    }

    public List<KnowledgeDoc> list(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return docMapper.selectList(new LambdaQueryWrapper<KnowledgeDoc>()
                .eq(KnowledgeDoc::getProjectId, projectId)
                .orderByDesc(KnowledgeDoc::getCreatedAt));
    }

    public void delete(Long docId, Long userId) {
        KnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) throw new BusinessException(ResultCode.NOT_FOUND, "文档不存在");
        accessService.requireWritable(doc.getProjectId(), userId);
        // 删除向量
        try {
            if (doc.getVectorIds() != null && !doc.getVectorIds().isBlank()) {
                vectorStore.delete(List.of(doc.getVectorIds().split(",")));
            }
        } catch (Exception e) {
            log.warn("向量删除失败 docId={}: {}", docId, e.getMessage());
        }
        docMapper.deleteById(docId);
    }

    /** 纯向量检索（供 AI Agent 的 RAG 工具复用），返回拼接好的上下文 */
    public String retrieveContext(Long projectId, String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .filterExpression("projectId == '" + projectId + "'")
                .build();
        List<Document> docs;
        try {
            docs = vectorStore.similaritySearch(request);// 校验向量检索是否成功
        } catch (Exception e) {
            log.error("向量检索失败", e);
            throw new BusinessException(ResultCode.AI_TIMEOUT, "知识库检索失败: " + e.getMessage());
        }
        if (docs == null || docs.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Document d : docs) {
            sb.append("【资料").append(i++).append("】").append(d.getText()).append("\n\n");
        }
        return sb.toString();
    }
}
