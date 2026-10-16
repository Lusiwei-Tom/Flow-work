package com.flowwork.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档实体：映射 knowledge_doc 表。
 * <p>
 * 对应 RAG（检索增强生成）流程中的一份原始知识资料：文件上传后先落库为 PROCESSING 状态，
 * 随后经 PDFBox 等工具抽取纯文本、按块切分（chunk）并写入 PGVector 向量库，
 * 切分完成后把向量 ID 回写到 {@link #vectorIds}，文档状态变为 READY；任一步失败则置为 FAILED。
 * 文档按项目维度隔离（{@link #projectId}），AI 检索时只在所属项目的文档范围内做相似度匹配。
 */
@Data                       // Lombok：编译期自动生成 getter/setter/toString/equals/hashCode
@TableName("knowledge_doc") // MyBatis-Plus：指定该实体映射的数据库表名
public class KnowledgeDoc {

    /** 主键 ID，数据库自增 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属项目 ID，知识库的项目级隔离维度 */
    private Long projectId;

    /** 文档标题（通常取自上传文件名或用户填写） */
    private String title;

    /** 原始上传文件名（含扩展名），用于下载与展示 */
    private String fileName;

    /** 抽取后的纯文本正文，供切分与向量化使用 */
    private String contentText;

    /** 正文内容的哈希值，用于秒传/去重：相同内容重复上传时可跳过重复解析 */
    private String contentHash;

    /** VectorStore 文档 ID 列表，逗号分隔；每个切分块对应一个向量记录 ID，删除文档时据此清理向量 */
    private String vectorIds;

    /** 文本切分后的分块（chunk）数量 */
    private Integer chunkCount;

    /** 处理状态：PROCESSING（解析向量化中）/ READY（可检索）/ FAILED（处理失败） */
    private String status;

    /** 处理失败时的错误信息，READY 时为 null */
    private String errorMsg;

    /** 原始文件大小（字节） */
    private Long sizeBytes;

    /** 文档版本号，重复上传/更新内容时递增，用于区分同一文档的不同版本 */
    private Integer version;

    /** 上传人用户 ID（sys_user.id） */
    private Long createdBy;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;
}
