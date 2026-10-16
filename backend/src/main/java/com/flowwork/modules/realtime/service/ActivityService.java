package com.flowwork.modules.realtime.service;

import com.flowwork.modules.realtime.entity.Activity;
import com.flowwork.modules.realtime.event.RealtimeEvent;
import com.flowwork.modules.realtime.mapper.ActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 动态服务：落库 activity + WebSocket 广播
 * 看板高频事件（KANBAN_DRAG/MEMBER_PRESENCE）只推广播，不落库，由 WriteOperationAspect 控制
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityMapper activityMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redis;

    public static final String PROJECT_PREFIX = "/topic/project.";
    private static final String ONLINE_KEY = "flowwork:online:project:";

    /** 记录动态：落库 + 广播 */
    public void record(Long projectId, Long taskId, String type, Long actorId,
                       String actorName, String content, boolean aiGenerated) {
        try {
            Activity activity = Activity.builder()
                    .projectId(projectId)
                    .taskId(taskId)
                    .type(type)
                    .actorId(actorId == null ? 0L : actorId)
                    .actorName(actorName == null ? "" : actorName)
                    .content(content)
                    .aiGenerated(aiGenerated ? 1 : 0)
                    .createdAt(LocalDateTime.now())
                    .build();
            activityMapper.insert(activity);
            broadcast(RealtimeEvent.builder()
                    .type(type).projectId(projectId).taskId(taskId)
                    .actorId(activity.getActorId()).actorName(activity.getActorName())
                    .content(content).aiGenerated(aiGenerated)
                    .payload(activity.getId() == null ? null : activity.getId())
                    .timestamp(activity.getCreatedAt())
                    .build());
        } catch (Exception e) {
            log.error("记录动态失败 project={} type={}", projectId, type, e);
        }
    }

    /** 仅广播（拖拽/在线状态等高频事件，不落库） */
    public void broadcast(RealtimeEvent event) {
        if (event.getTimestamp() == null) event.setTimestamp(LocalDateTime.now());
        messagingTemplate.convertAndSend(PROJECT_PREFIX + event.getProjectId(), event);
    }

    // ---------- 在线成员（Redis Set + TTL 心跳） ----------

    public void heartbeat(Long projectId, Long userId) {
        String key = ONLINE_KEY + projectId;
        redis.opsForSet().add(key, String.valueOf(userId));
    }

    public void leave(Long projectId, Long userId) {
        redis.opsForSet().remove(ONLINE_KEY + projectId, String.valueOf(userId));
    }

    public Set<String> onlineMembers(Long projectId) {
        return redis.opsForSet().members(ONLINE_KEY + projectId);
    }
}
