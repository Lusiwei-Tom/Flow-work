package com.flowwork.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.project.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("SELECT project_role FROM project_member WHERE project_id = #{projectId} AND user_id = #{userId} LIMIT 1")
    String selectRole(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Select("SELECT project_id FROM project_member WHERE user_id = #{userId}")
    List<Long> selectProjectIds(@Param("userId") Long userId);

    /** 成员列表（含用户信息） */
    @Select("""
            SELECT u.id AS userId, u.username AS username, u.nickname AS nickname,
                   pm.project_role AS role, pm.joined_at AS joinedAt
            FROM project_member pm
            JOIN sys_user u ON u.id = pm.user_id
            WHERE pm.project_id = #{projectId}
            ORDER BY pm.joined_at
            """)
    List<Map<String, Object>> selectMembersWithUser(@Param("projectId") Long projectId);
}
