package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.project.dto.ColumnRequest;
import com.flowwork.modules.project.entity.BoardColumn;
import com.flowwork.modules.project.mapper.BoardColumnMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardColumnMapper columnMapper;
    private final ProjectAccessService accessService;

    public List<BoardColumn> list(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return columnMapper.selectList(new LambdaQueryWrapper<BoardColumn>()
                .eq(BoardColumn::getProjectId, projectId)
                .eq(BoardColumn::getStatus, 1)
                .orderByAsc(BoardColumn::getSortOrder));
    }

    public BoardColumn create(Long projectId, ColumnRequest req, Long userId) {
        accessService.requireWritable(projectId, userId);
        BoardColumn col = new BoardColumn();
        col.setProjectId(projectId);
        col.setName(req.getName());
        col.setSortOrder(req.getSortOrder() == null ? nextOrder(projectId) : req.getSortOrder());
        col.setStatus(1);
        columnMapper.insert(col);
        return col;
    }

    public BoardColumn rename(Long projectId, Long columnId, String name, Long userId) {
        accessService.requireWritable(projectId, userId);
        BoardColumn col = mustOwn(projectId, columnId);
        col.setName(name);
        columnMapper.updateById(col);
        return col;
    }

    public void delete(Long projectId, Long columnId, Long userId) {
        accessService.requireManager(projectId, userId);
        BoardColumn col = mustOwn(projectId, columnId);
        col.setStatus(0);
        columnMapper.updateById(col);
    }

    private BoardColumn mustOwn(Long projectId, Long columnId) {
        BoardColumn col = columnMapper.selectById(columnId);
        if (col == null || !col.getProjectId().equals(projectId)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "看板列不存在");
        }
        return col;
    }

    private Integer nextOrder(Long projectId) {
        Long count = columnMapper.selectCount(new LambdaQueryWrapper<BoardColumn>()
                .eq(BoardColumn::getProjectId, projectId));
        return count == null ? 0 : count.intValue();
    }
}
