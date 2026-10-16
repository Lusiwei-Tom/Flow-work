package com.flowwork.modules.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
/**
 * 项目请求参数
 */
@Data
public class ProjectRequest {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 128)
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}
