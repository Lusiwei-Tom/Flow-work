package com.flowwork.modules.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 任务实体
 */
@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long columnId;
    private String title;
    private String description;
    private Long assigneeId;
    /** 1低 2中 3高 4紧急 */
    private Integer priority;
    private BigDecimal estimatedHours;
    private BigDecimal actualHours;
    private LocalDate dueDate;
    /** 1待办 2进行中 3已完成 4已取消 */
    private Integer status;
    private Long parentId;
    private Integer sortOrder;
    private Integer aiGenerated;
    private String aiSource;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
