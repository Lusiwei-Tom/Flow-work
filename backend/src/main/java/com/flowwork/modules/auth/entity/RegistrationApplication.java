package com.flowwork.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
/**
 * 注册申请实体
 */
@Data
@TableName("registration_application")
public class RegistrationApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String applicantName;
    private String phone;
    private String email;
    private String reason;
    /** pending / approved / rejected */
    private String status;
    private String reviewNote;
    /** 审批通过后生成的邀请码 */
    private String inviteCode;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
