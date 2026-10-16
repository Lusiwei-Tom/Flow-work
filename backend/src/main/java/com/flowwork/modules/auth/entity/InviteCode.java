package com.flowwork.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 邀请码实体
 */
@Data
@TableName("invite_code")
public class InviteCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    /** 注册后授予的系统角色：admin / project_manager / member */
    private String presetRole;
    /** 绑定项目（可空） */
    private Long presetProjectId;
    /** 项目内角色：MANAGER / MEMBER / OBSERVER */
    private String presetProjectRole;
    /** 最大使用次数，0 = 无限 */
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Long createdBy;
    /** active / disabled */
    private String status;
    private LocalDateTime createdAt;
}
