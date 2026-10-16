package com.flowwork.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 项目列请求参数
 */
@Data
public class ColumnRequest {
    @NotBlank
    private String name;
    private Integer sortOrder;
}
