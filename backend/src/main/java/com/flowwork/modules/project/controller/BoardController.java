package com.flowwork.modules.project.controller;

import com.flowwork.common.annotation.RequirePermission;
import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.project.dto.ColumnRequest;
import com.flowwork.modules.project.entity.BoardColumn;
import com.flowwork.modules.project.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "3-看板")
@RestController
@RequestMapping("/api/projects/{projectId}/columns")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @Operation(summary = "看板列列表")
    @GetMapping
    @RequirePermission("project:project:read")
    public Result<List<BoardColumn>> list(@PathVariable Long projectId) {
        return Result.ok(boardService.list(projectId, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "新建列")
    @PostMapping
    @RequirePermission("project:kanban:manage")
    public Result<BoardColumn> create(@PathVariable Long projectId, @Valid @RequestBody ColumnRequest req) {
        return Result.ok(boardService.create(projectId, req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "重命名列")
    @PutMapping("/{columnId}")
    @RequirePermission("project:kanban:manage")
    public Result<BoardColumn> rename(@PathVariable Long projectId, @PathVariable Long columnId,
                                      @RequestBody Map<String, String> body) {
        return Result.ok(boardService.rename(projectId, columnId, body.get("name"),
                SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "删除列")
    @DeleteMapping("/{columnId}")
    @RequirePermission("project:kanban:manage")
    public Result<Void> delete(@PathVariable Long projectId, @PathVariable Long columnId) {
        boardService.delete(projectId, columnId, SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
