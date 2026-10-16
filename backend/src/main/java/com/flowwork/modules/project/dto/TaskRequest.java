package com.flowwork.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 任务请求参数
 */
@Data
public class TaskRequest {
    private Long columnId;
    @NotBlank(message = "任务标题不能为空")
    private String title;
    private String description;
    private Long assigneeId;
    private Integer priority = 2;
    private BigDecimal estimatedHours;
    private LocalDate dueDate;
    private Long parentId;
    /** AI 任务拆解入库时使用 */
    private Boolean aiGenerated = false;
    private String aiSource;
}
