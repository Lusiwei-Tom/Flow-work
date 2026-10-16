package com.flowwork.modules.realtime.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("activity")
public class Activity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long projectId;
    private Long taskId;
    private String type;
    private Long actorId;
    private String actorName;
    private String content;
    private Integer aiGenerated;
    private LocalDateTime createdAt;
}
