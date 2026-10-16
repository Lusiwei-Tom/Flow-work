package com.flowwork.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.auth.entity.SysUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Insert("INSERT INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} LIMIT 1")
    Long selectRoleIdByCode(@Param("roleCode") String roleCode);

    @Select("SELECT r.role_code FROM sys_role r " +
            "JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    /** 角色权限 + 用户白名单 - 用户黑名单 */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            WHERE p.id IN (
                SELECT rp.permission_id FROM sys_role_permission rp
                JOIN sys_user_role ur ON ur.role_id = rp.role_id
                WHERE ur.user_id = #{userId}
                UNION
                SELECT up.permission_id FROM sys_user_permission up
                WHERE up.user_id = #{userId} AND up.type = 1
            )
            AND p.id NOT IN (
                SELECT up2.permission_id FROM sys_user_permission up2
                WHERE up2.user_id = #{userId} AND up2.type = 2
            )
            """)
    List<String> selectPermissionCodes(@Param("userId") Long userId);

    @Select("SELECT id FROM sys_user WHERE username = #{account} OR phone = #{account} OR email = #{account} LIMIT 1")
    Long findAccountId(@Param("account") String account);

    /** 邀请成员时的用户搜索（仅返回必要字段） */
    @Select("""
            SELECT id, username, nickname FROM sys_user
            WHERE status = 1 AND (username LIKE CONCAT('%', #{kw}, '%') OR nickname LIKE CONCAT('%', #{kw}, '%'))
            LIMIT 10
            """)
    List<Map<String, Object>> searchUsers(@Param("kw") String kw);
}
