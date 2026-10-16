package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.auth.entity.SysUser;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.ProjectMember;
import com.flowwork.modules.project.mapper.ProjectMapper;
import com.flowwork.modules.project.mapper.ProjectMemberMapper;
import com.flowwork.modules.realtime.event.RealtimeEvent;
import com.flowwork.modules.realtime.service.ActivityService;
import com.flowwork.modules.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 项目成员管理：列表 / 邀请 / 改角色 / 移除
 * 操作权限 = 项目内 MANAGER（owner 天然是 MANAGER）
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private static final Set<String> VALID_ROLES = Set.of("MANAGER", "MEMBER", "OBSERVER");

    private final ProjectMemberMapper memberMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService accessService;
    private final SysUserMapper userMapper;
    private final NotificationService notificationService;
    private final ActivityService activityService;

    public List<Map<String, Object>> listMembers(Long projectId, Long userId) {
        accessService.requireMember(projectId, userId);
        return memberMapper.selectMembersWithUser(projectId);
    }

    /** 按用户名邀请成员 */
    public Map<String, Object> addMember(Long projectId, String username, String projectRole, Long operatorId) {
        accessService.requireManager(projectId, operatorId);
        Project project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ResultCode.NOT_FOUND, "项目不存在");
        if (projectRole == null || !VALID_ROLES.contains(projectRole)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色必须是 MANAGER/MEMBER/OBSERVER");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (user == null) throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        if (project.getOwnerId().equals(user.getId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "项目创建者已是项目管理员，无需添加");
        }
        if (memberMapper.selectRole(projectId, user.getId()) != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该用户已是项目成员");
        }
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(projectId);
        pm.setUserId(user.getId());
        pm.setProjectRole(projectRole);
        pm.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(pm);

        Map<String, Object> vo = Map.of("userId", user.getId(), "username", user.getUsername(),
                "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname(),
                "role", projectRole);
        notificationService.push(user.getId(), "MEMBER_JOINED", "加入项目",
                "你已被加入项目「" + project.getName() + "」", projectId, null);
        activityService.broadcast(RealtimeEvent.builder()
                .type("MEMBER_JOINED").projectId(projectId)
                .actorId(operatorId)
                .content("「" + user.getNickname() + "」加入了项目")
                .aiGenerated(false).build());
        return vo;
    }

    public void updateRole(Long projectId, Long targetUserId, String projectRole, Long operatorId) {
        accessService.requireManager(projectId, operatorId);
        if (projectRole == null || !VALID_ROLES.contains(projectRole)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "角色必须是 MANAGER/MEMBER/OBSERVER");
        }
        ProjectMember pm = mustGetMember(projectId, targetUserId);
        pm.setProjectRole(projectRole);
        memberMapper.updateById(pm);
    }

    public void removeMember(Long projectId, Long targetUserId, Long operatorId) {
        accessService.requireManager(projectId, operatorId);
        ProjectMember pm = mustGetMember(projectId, targetUserId);
        memberMapper.deleteById(pm.getId());
        activityService.broadcast(RealtimeEvent.builder()
                .type("MEMBER_LEFT").projectId(projectId)
                .actorId(operatorId)
                .content("有成员离开了项目")
                .aiGenerated(false).build());
    }

    private ProjectMember mustGetMember(Long projectId, Long targetUserId) {
        ProjectMember pm = memberMapper.selectOne(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, targetUserId)
                .last("LIMIT 1"));
        if (pm == null) throw new BusinessException(ResultCode.NOT_FOUND, "该用户不是项目成员");
        return pm;
    }
}
