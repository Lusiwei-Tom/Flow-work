package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.ProjectMember;
import com.flowwork.modules.project.mapper.ProjectMapper;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 项目访问控制：校验用户在项目内的角色
 * MANAGER > MEMBER > OBSERVER；项目 owner 拥有 MANAGER 权限
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;

    public Project requireProject(Long projectId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) throw new BusinessException(ResultCode.NOT_FOUND, "项目不存在");
        return p;
    }

    /** 要求是项目成员 */
    public String requireMember(Long projectId, Long userId) {
        Project p = requireProject(projectId);
        if (p.getOwnerId().equals(userId)) return "MANAGER";//
        String role = memberMapper.selectRole(projectId, userId);
        if (role == null) throw new BusinessException(ResultCode.FORBIDDEN, "不是项目成员");
        return role;
    }

    /** 要求是管理员（owner 或 MANAGER） */
    public void requireManager(Long projectId, Long userId) {
        String role = requireMember(projectId, userId);// 校验用户是否是项目成员
        if (!"MANAGER".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "需要项目管理员权限");
        }
    }

    public boolean isManager(Long projectId, Long userId) {
        Project p = projectMapper.selectById(projectId);
        if (p == null) return false;
        if (p.getOwnerId().equals(userId)) return true;
        return "MANAGER".equals(memberMapper.selectRole(projectId, userId));
    }

    public void requireWritable(Long projectId, Long userId) {
        Project p = requireProject(projectId);
        if (p.getStatus() != null && p.getStatus() == 2) {
            throw new BusinessException(ResultCode.PROJECT_ARCHIVED);
        }
        String role = requireMember(projectId, userId);
        if ("OBSERVER".equals(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "观察者为只读角色");
        }
    }

    public long countMembers(Long projectId) {
        return memberMapper.selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId));
    }
}
