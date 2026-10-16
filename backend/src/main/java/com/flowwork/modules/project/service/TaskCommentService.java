package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.entity.TaskComment;
import com.flowwork.modules.project.mapper.TaskCommentMapper;
import com.flowwork.modules.project.mapper.TaskMapper;
import com.flowwork.modules.realtime.service.ActivityService;
import com.flowwork.modules.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskCommentService {

    private final TaskCommentMapper commentMapper;
    private final TaskMapper taskMapper;
    private final ProjectAccessService accessService;
    private final ActivityService activityService;
    private final NotificationService notificationService;

    public List<TaskComment> list(Long taskId, Long userId) {
        Task t = taskMapper.selectById(taskId);
        accessService.requireMember(t.getProjectId(), userId);
        return commentMapper.selectList(new LambdaQueryWrapper<TaskComment>()
                .eq(TaskComment::getTaskId, taskId)
                .orderByDesc(TaskComment::getCreatedAt));
    }

    public TaskComment create(Long taskId, String content, String mentionsJson, Long userId) {
        Task t = taskMapper.selectById(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        TaskComment c = new TaskComment();
        c.setTaskId(taskId);
        c.setUserId(userId);
        c.setContent(content);
        c.setMentionsJson(mentionsJson);
        commentMapper.insert(c);
        activityService.record(t.getProjectId(), taskId, "TASK_COMMENTED",
                userId, null, "评论了任务「" + t.getTitle() + "」", false);
        // 通知任务负责人与创建者（排除评论者本人）
        Set<Long> targets = new HashSet<>();
        if (t.getAssigneeId() != null) targets.add(t.getAssigneeId());
        if (t.getCreatedBy() != null) targets.add(t.getCreatedBy());
        targets.remove(userId);
        String preview = content.length() > 50 ? content.substring(0, 50) + "…" : content;
        for (Long uid : targets) {
            notificationService.push(uid, "TASK_COMMENTED", "任务新评论",
                    "「" + t.getTitle() + "」有新评论：" + preview, t.getProjectId(), t.getId());
        }
        return c;
    }
}
