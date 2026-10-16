package com.flowwork.modules.knowledge.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.knowledge.entity.KnowledgeDoc;
import com.flowwork.modules.knowledge.service.KnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "6-知识库")
@RestController
@RequestMapping("/api/projects/{projectId}/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    @Operation(summary = "上传文档（txt/md/pdf，自动分块向量化）")
    @PostMapping("/docs")
    @RequirePermission("knowledge:doc:upload")
    public Result<KnowledgeDoc> upload(@PathVariable Long projectId,
                                       @RequestParam("file") MultipartFile file) {
        return Result.ok(knowledgeService.upload(projectId, file, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "文档列表")
    @GetMapping("/docs")
    @RequirePermission("knowledge:doc:read")
    public Result<List<KnowledgeDoc>> list(@PathVariable Long projectId) {
        return Result.ok(knowledgeService.list(projectId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "删除文档（同时删除向量）")
    @DeleteMapping("/docs/{docId}")
    @RequirePermission("knowledge:doc:upload")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long docId) {
        knowledgeService.delete(docId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
