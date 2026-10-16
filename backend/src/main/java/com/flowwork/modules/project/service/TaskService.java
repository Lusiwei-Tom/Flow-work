package com.flowwork.modules.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.annotation.WriteOperation;
import com.flowwork.common.exception.BusinessException;
import com.flowwork.common.result.ResultCode;
import com.flowwork.modules.project.dto.TaskMoveRequest;
import com.flowwork.modules.project.dto.TaskRequest;
import com.flowwork.modules.project.entity.BoardColumn;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.BoardColumnMapper;
import com.flowwork.modules.project.mapper.TaskMapper;
import com.flowwork.modules.realtime.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final BoardColumnMapper columnMapper;
    private final ProjectAccessService accessService;
    private final TaskLockService lockService;
    private final ActivityService activityService;
    private final com.flowwork.modules.realtime.service.NotificationService notificationService;

    /** 一个成员同项目未完成任务上限 */
    private static final int MAX_UNFINISHED = 10;

    // ---------- 查询 ----------

    /** 我的任务：跨项目聚合（个人数据，无需项目权限） */
    public List<Map<String, Object>> myTasks(Long userId) {
        return taskMapper.selectMyTasks(userId);
    }

    public List<Task> listByProject(Long projectId, Long userId, String keyword,
                                    Long assigneeId, Integer priority, Integer status) {
        accessService.requireMember(projectId, userId);
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .ne(Task::getStatus, 4)
                .and(hasKeyword, q -> q.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword))
                .eq(assigneeId != null, Task::getAssigneeId, assigneeId)
                .eq(priority != null, Task::getPriority, priority)
                .eq(status != null, Task::getStatus, status)
                .orderByAsc(Task::getSortOrder);
        return taskMapper.selectList(w);
    }

    public Task detail(Long taskId, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireMember(t.getProjectId(), userId);
        return t;
    }

    // ---------- 创建 ----------

    @Transactional
    @WriteOperation(type = "TASK_CREATED", content = "创建了任务 {title}")
    public Task create(Long projectId, TaskRequest req, Long userId) {
        accessService.requireWritable(projectId, userId);
        BoardColumn column = resolveColumn(projectId, req.getColumnId());
        checkAssignee(projectId, req.getAssigneeId());
        if (req.getParentId() != null) {
            mustExist(req.getParentId());
        }
        Task t = new Task();
        t.setProjectId(projectId);
        t.setColumnId(column.getId());
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setAssigneeId(req.getAssigneeId());
        t.setPriority(req.getPriority() == null ? 2 : req.getPriority());
        t.setEstimatedHours(req.getEstimatedHours());
        t.setDueDate(req.getDueDate());
        t.setParentId(req.getParentId());
        t.setStatus(statusByColumnName(column.getName()));
        t.setSortOrder(nextSort(column.getId()));
        t.setAiGenerated(Boolean.TRUE.equals(req.getAiGenerated()) ? 1 : 0);
        t.setAiSource(req.getAiSource());
        t.setCreatedBy(userId);
        taskMapper.insert(t);
        return t;
    }

    /** AI 批量任务拆解入库（不逐个走锁，循环依赖批量校验） */
    @Transactional
    public List<Task> createAiBatch(Long projectId, List<TaskRequest> tasks, Long userId, String aiSource) {
        accessService.requireWritable(projectId, userId);
        List<Task> saved = new ArrayList<>();
        for (TaskRequest req : tasks) {
            BoardColumn column = resolveColumn(projectId, req.getColumnId());
            Task t = new Task();
            t.setProjectId(projectId);
            t.setColumnId(column.getId());
            t.setTitle(req.getTitle());
            t.setDescription(req.getDescription());
            t.setAssigneeId(req.getAssigneeId());
            t.setPriority(req.getPriority() == null ? 2 : req.getPriority());
            t.setEstimatedHours(req.getEstimatedHours());
            t.setDueDate(req.getDueDate());
            t.setStatus(1);
            t.setSortOrder(nextSort(column.getId()));
            t.setAiGenerated(1);
            t.setAiSource(aiSource);
            t.setCreatedBy(userId);
            taskMapper.insert(t);
            saved.add(t);
        }
        return saved;
    }

    // ---------- 更新 ----------

    @WriteOperation(type = "TASK_UPDATED", content = "更新了任务 {title}")
    public Task update(Long taskId, TaskRequest req, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        lockService.withLock(taskId, userId, () -> null);
        if (req.getParentId() != null && !req.getParentId().equals(t.getParentId())) {
            checkCycle(t.getId(), req.getParentId(), t.getProjectId());
            t.setParentId(req.getParentId());
        }
        boolean assigneeChanged = false;
        if (req.getAssigneeId() != null && !req.getAssigneeId().equals(t.getAssigneeId())) {
            checkAssignee(t.getProjectId(), req.getAssigneeId());
            t.setAssigneeId(req.getAssigneeId());
            assigneeChanged = true;
        }
        t.setTitle(req.getTitle());
        t.setDescription(req.getDescription());
        t.setPriority(req.getPriority());
        t.setEstimatedHours(req.getEstimatedHours());
        t.setDueDate(req.getDueDate());
        taskMapper.updateById(t);
        // 编辑中改派负责人时通知新负责人（排除操作者本人）
        if (assigneeChanged && t.getAssigneeId() != null && !t.getAssigneeId().equals(userId)) {
            notificationService.push(t.getAssigneeId(), "TASK_ASSIGNED", "新任务分配",
                    "「" + t.getTitle() + "」已分配给你", t.getProjectId(), t.getId());
        }
        return t;
    }

    @WriteOperation(type = "TASK_PRIORITY_CHANGED", content = "调整了任务 {title} 的优先级")
    public Task changePriority(Long taskId, Integer priority, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        t.setPriority(priority);
        taskMapper.updateById(t);
        return t;
    }

    @WriteOperation(type = "TASK_ASSIGNED", content = "分配了任务 {title}")
    public Task assign(Long taskId, Long assigneeId, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        checkAssignee(t.getProjectId(), assigneeId);
        Long oldAssignee = t.getAssigneeId();
        t.setAssigneeId(assigneeId);
        taskMapper.updateById(t);
        // 通知被分配人（排除操作者本人与重复分配）
        if (assigneeId != null && !assigneeId.equals(userId) && !assigneeId.equals(oldAssignee)) {
            notificationService.push(assigneeId, "TASK_ASSIGNED", "新任务分配",
                    "「" + t.getTitle() + "」已分配给你", t.getProjectId(), t.getId());
        }
        return t;
    }

    /** 看板拖拽：乐观重试 + 分布式锁 + 列驱动状态机，高频事件只广播不落库 */
    @WriteOperation(type = "KANBAN_DRAG", persist = false, content = "拖拽了任务 {title}")
    public Task move(TaskMoveRequest req, Long userId) {
        Task t = mustExist(req.getTaskId());
        accessService.requireWritable(t.getProjectId(), userId);
        BoardColumn target = columnMapper.selectById(req.getTargetColumnId());
        if (target == null || !target.getProjectId().equals(t.getProjectId())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "目标列不存在");
        }
        return lockService.withLock(t.getId(), userId, () -> {
            int affected = taskMapper.updatePosition(t.getId(), target.getId(), req.getSortOrder());
            if (affected == 0) {
                throw new BusinessException(ResultCode.LOCK_CONFLICT, "任务已被他人移动，请刷新");
            }
            Task fresh = taskMapper.selectById(t.getId());
            Integer newStatus = statusByColumnName(target.getName());
            if (!newStatus.equals(fresh.getStatus())) {
                fresh.setStatus(newStatus);
                taskMapper.updateById(fresh);
                // 状态真正变化时补一条落库动态（拖拽本身只广播）
                boolean ai = com.flowwork.common.ai.AiContextHolder.isAiContext();
                activityService.record(fresh.getProjectId(), fresh.getId(), "TASK_STATUS_CHANGED",
                        userId, null, "将任务「" + fresh.getTitle() + "」移动到「" + target.getName() + "」", ai);
            }
            return fresh;
        });
    }

    @WriteOperation(type = "TASK_STATUS_CHANGED", content = "变更了任务 {title} 的状态")
    public Task changeStatus(Long taskId, Integer status, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        t.setStatus(status);
        // 状态与看板列联动：把卡片同步移动到名称语义匹配的列（与拖拽 move 的列驱动状态机保持一致），
        // 否则“我的任务”改状态后，看板卡片仍停留在原列
        BoardColumn targetCol = columnMapper.selectList(new LambdaQueryWrapper<BoardColumn>()
                        .eq(BoardColumn::getProjectId, t.getProjectId())
                        .eq(BoardColumn::getStatus, 1)
                        .orderByAsc(BoardColumn::getSortOrder)).stream()
                .filter(c -> status.equals(statusByColumnName(c.getName())))
                .findFirst().orElse(null);
        if (targetCol != null && !targetCol.getId().equals(t.getColumnId())) {
            t.setColumnId(targetCol.getId());
            t.setSortOrder(nextSort(targetCol.getId()));
        }
        taskMapper.updateById(t);
        return t;
    }

    public void delete(Long taskId, Long userId) {
        Task t = mustExist(taskId);
        accessService.requireWritable(t.getProjectId(), userId);
        t.setStatus(4);
        taskMapper.updateById(t);
    }

    // ---------- 校验 ----------

    private Task mustExist(Long taskId) {
        Task t = taskMapper.selectById(taskId);
        if (t == null || t.getStatus() == 4) {
            throw new BusinessException(ResultCode.NOT_FOUND, "任务不存在");
        }
        return t;
    }

    private void checkAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) return;
        List<Long> unfinished = taskMapper.selectUnfinishedByAssignee(projectId, assigneeId);
        if (unfinished.size() >= MAX_UNFINISHED) {
            throw new BusinessException(ResultCode.LIMIT_EXCEEDED,
                    "该成员未完成任务已达 " + MAX_UNFINISHED + " 个上限");
        }
    }

    /**
     * DAG 循环依赖检测：newParent 不能是 taskId 自身或其子孙
     */
    private void checkCycle(Long taskId, Long newParentId, Long projectId) {
        if (taskId.equals(newParentId)) {
            throw new BusinessException(ResultCode.CYCLE_DEPENDENCY, "不能将自身设为父任务");
        }
        // 构建 children 邻接表
        List<Map<String, Object>> edges = taskMapper.selectEdges(projectId);
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Map<String, Object> e : edges) {
            Long child = ((Number) e.get("id")).longValue();
            Object pid = e.get("parent_id");
            Long parent = pid == null ? null : ((Number) pid).longValue();
            if (parent != null) {
                childrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
            }
        }
        // DFS：若 newParentId 出现在 taskId 子树中，则成环
        Deque<Long> stack = new ArrayDeque<>();
        stack.push(taskId);
        while (!stack.isEmpty()) {
            Long cur = stack.pop();
            if (cur.equals(newParentId)) {
                throw new BusinessException(ResultCode.CYCLE_DEPENDENCY, "存在循环依赖");
            }
            for (Long child : childrenMap.getOrDefault(cur, List.of())) {
                stack.push(child);
            }
        }
    }

    private BoardColumn resolveColumn(Long projectId, Long columnId) {
        if (columnId != null) {
            BoardColumn col = columnMapper.selectById(columnId);
            if (col != null && col.getProjectId().equals(projectId) && col.getStatus() == 1) {
                return col;
            }
        }
        return columnMapper.selectList(new LambdaQueryWrapper<BoardColumn>()
                        .eq(BoardColumn::getProjectId, projectId)
                        .eq(BoardColumn::getStatus, 1)
                        .orderByAsc(BoardColumn::getSortOrder).last("LIMIT 1"))
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND, "项目看板列不存在"));
    }

    private Integer nextSort(Long columnId) {
        Long count = taskMapper.selectCount(new LambdaQueryWrapper<Task>().eq(Task::getColumnId, columnId));
        return count == null ? 0 : count.intValue();
    }

    /** 列名驱动状态：含"完成"=3，"待办/待处理"=1，其余=2 进行中 */
    private Integer statusByColumnName(String name) {
        if (name == null) return 1;
        if (name.contains("完成") || name.contains("已结束")) return 3;
        if (name.contains("待办") || name.contains("待处理") || name.equals("Todo")) return 1;
        return 2;
    }
}
