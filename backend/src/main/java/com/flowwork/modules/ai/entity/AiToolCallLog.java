package com.flowwork.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_tool_call_log")// 定义数据库表名
public class AiToolCallLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long userId;
    private String toolName;
    private String toolParams;
    private String toolResult;
    private Integer durationMs;
    private Integer success;
    private String errorMsg;
    private LocalDateTime createdAt;
}
