package com.flowwork.modules.realtime.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.ProjectMapper;
import com.flowwork.modules.project.mapper.TaskMapper;
import com.flowwork.modules.realtime.entity.Notification;
import com.flowwork.modules.realtime.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 站内通知：落库 + WebSocket 个人频道（/user/queue/notifications）实时推送
 * 触发点：任务分配 / 任务评论 / 成员加入 / 每日临期逾期扫描
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final SimpMessagingTemplate messagingTemplate;

    /** 写入通知并尝试实时推送（推送失败不影响业务） */
    public void push(Long userId, String type, String title, String content, Long projectId, Long taskId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setContent(content);
        n.setProjectId(projectId);
        n.setTaskId(taskId);
        n.setIsRead(0);
        notificationMapper.insert(n);
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId),
                    "/queue/notifications", n);
        } catch (Exception e) {
            log.debug("通知实时推送失败(用户可能不在线): {}", e.getMessage());
        }
    }

    public List<Notification> list(Long userId, boolean onlyUnread) {
        return notificationMapper.selectList(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(onlyUnread, Notification::getIsRead, 0)
                .orderByDesc(Notification::getId)
                .last("LIMIT 30"));
    }

    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    public void markRead(Long userId, Long id) {
        int affected = notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
        if (affected == 0) throw new BusinessException(ResultCode.NOT_FOUND, "通知不存在");
    }

    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    /**
     * 每天早上 9 点扫描所有活跃项目：临期（明天内截止）与逾期任务，
     * 通知负责人（优先）或创建者；同任务同类型同一天只提醒一次
     */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Shanghai")
    public void dailyDueReminder() {
        List<Project> projects = projectMapper.selectList(new LambdaQueryWrapper<Project>()
                .eq(Project::getStatus, 1));
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        int sent = 0;
        for (Project p : projects) {
            for (Task t : taskMapper.selectDueSoon(p.getId(), tomorrow)) {
                sent += remindDue(t, "TASK_DUE_SOON", "任务即将到期");
            }
            for (Task t : taskMapper.selectOverdue(p.getId())) {
                sent += remindDue(t, "TASK_OVERDUE", "任务已逾期");
            }
        }
        if (sent > 0) log.info("临期/逾期提醒已发送 {} 条", sent);
    }

    private int remindDue(Task t, String type, String title) {
        Long target = t.getAssigneeId() != null ? t.getAssigneeId() : t.getCreatedBy();
        if (target == null) return 0;
        if (notificationMapper.countToday(target, t.getId(), type) > 0) return 0;
        String content = "TASK_OVERDUE".equals(type)
                ? "「" + t.getTitle() + "」已超过截止日期，请尽快处理"
                : "「" + t.getTitle() + "」将于 " + t.getDueDate() + " 截止";
        push(target, type, title, content, t.getProjectId(), t.getId());
        return 1;
    }
}
