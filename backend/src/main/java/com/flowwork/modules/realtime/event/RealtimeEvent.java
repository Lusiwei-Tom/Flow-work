package com.flowwork.modules.realtime.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket 实时事件（STOMP 广播体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent {
    private String type;
    private Long projectId;
    private Long taskId;
    private Long actorId;
    private String actorName;
    private String content;
    private Boolean aiGenerated;
    private Object payload;
    private LocalDateTime timestamp;
}
