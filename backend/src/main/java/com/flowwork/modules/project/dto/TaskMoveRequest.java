package com.flowwork.modules.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务任务移动请求参数
 */
@Data
public class TaskMoveRequest {
    @NotNull
    private Long taskId;
    @NotNull
    private Long targetColumnId;
    @NotNull
    private Integer sortOrder;
}
