package com.flowwork.modules.realtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知：任务分配 / 评论 / 临期逾期提醒 / 成员加入
 */
@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** TASK_ASSIGNED / TASK_COMMENTED / TASK_DUE_SOON / TASK_OVERDUE / MEMBER_JOINED */
    private String type;
    private String title;
    private String content;
    private Long projectId;
    private Long taskId;
    @TableField("is_read")
    private Integer isRead;
    private LocalDateTime createdAt;
}
