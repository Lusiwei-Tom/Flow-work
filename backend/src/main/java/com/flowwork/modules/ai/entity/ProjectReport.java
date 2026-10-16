package com.flowwork.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_report")
public class ProjectReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    /** WEEKLY / RISK / CUSTOM */
    private String type;
    private String title;
    private String content;
    private Integer aiGenerated;
    private Long createdBy;
    private LocalDateTime createdAt;
}
