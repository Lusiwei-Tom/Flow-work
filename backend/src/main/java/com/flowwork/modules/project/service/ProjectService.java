package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.flowwork.common.annotation.WriteOperation;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.project.dto.ProjectRequest;
import com.flowwork.modules.project.entity.BoardColumn;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.ProjectMember;
import com.flowwork.modules.project.mapper.BoardColumnMapper;
import com.flowwork.modules.project.mapper.ProjectMapper;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final BoardColumnMapper columnMapper;
    private final ProjectAccessService accessService;

    private static final List<String> DEFAULT_COLUMNS = List.of("待办", "进行中", "评审中", "已完成");

    @Transactional
    @WriteOperation(type = "PROJECT_CREATED", content = "创建了项目 {name}")
    public Project create(ProjectRequest req, Long userId) {
        Long dup = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
                .eq(Project::getName, req.getName()).eq(Project::getOwnerId, userId));
        if (dup != null && dup > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_NAME, "你已创建过同名项目");
        }
        Project p = new Project();
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setOwnerId(userId);
        p.setStatus(1);
        projectMapper.insert(p);

        ProjectMember m = new ProjectMember();
        m.setProjectId(p.getId());
        m.setUserId(userId);
        m.setProjectRole("MANAGER");
        memberMapper.insert(m);

        int order = 0;
        for (String name : DEFAULT_COLUMNS) {
            BoardColumn col = new BoardColumn();
            col.setProjectId(p.getId());
            col.setName(name);
            col.setSortOrder(order++);
            col.setStatus(1);
            columnMapper.insert(col);
        }
        return p;
    }

    public IPage<Project> listMine(Long userId, int page, int size) {
        List<Long> ids = memberMapper.selectProjectIds(userId);
        Page<Project> p = new Page<>(Math.max(1, page), Math.max(1, Math.min(size, 100)));
        if (ids.isEmpty()) return p;
        return projectMapper.selectPage(p, new LambdaQueryWrapper<Project>()
                .in(Project::getId, ids)
                .ne(Project::getStatus, 3)
                .orderByDesc(Project::getUpdatedAt));
    }

    public Project detail(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return accessService.requireProject(projectId);
    }

    @WriteOperation(type = "PROJECT_UPDATED", content = "更新了项目 {name}")
    public Project update(Long projectId, ProjectRequest req, Long userId) {
        accessService.requireManager(projectId, userId);
        Project p = accessService.requireProject(projectId);
        p.setName(req.getName());
        p.setDescription(req.getDescription());
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        projectMapper.updateById(p);
        return p;
    }

    @WriteOperation(type = "PROJECT_ARCHIVED", content = "归档了项目 {name}")
    public Project archive(Long projectId, Long userId) {
        accessService.requireManager(projectId, userId);
        Project p = accessService.requireProject(projectId);
        p.setStatus(2);
        projectMapper.updateById(p);
        return p;
    }

    public void delete(Long projectId, Long userId) {
        accessService.requireManager(projectId, userId);
        Project p = accessService.requireProject(projectId);
        p.setStatus(3);
        projectMapper.updateById(p);
    }
}
