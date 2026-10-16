package com.flowwork.modules.ai.tools;

import com.flowwork.common.annotation.AiTool;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * AI 项目管家 —— 写操作工具（真正修改业务数据，触发审计 + 动态广播）
 * <p>
 * 项目 ID 通过 Spring AI ToolContext 由调用方（AiService）随调用链注入，
 * 替代原 ThreadLocal 方案（工具在框架线程池执行时 ThreadLocal 会丢值导致越权或查空）。
 */
@Component
@RequiredArgsConstructor
public class TaskOperationTool {

    private final TaskMapper taskMapper;

    /** 从 ToolContext 取出当前项目 ID（AiService 通过 .toolContext() 传入） */
    private static Long projectId(ToolContext tc) {
        return (Long) tc.getContext().get("projectId");
    }

    @Tool(description = "修改指定任务的优先级。priority 取值：1=低，2=中，3=高，4=紧急")
    @AiTool("updateTaskPriority")
    public String updateTaskPriority(
            @ToolParam(description = "任务ID") Long taskId,
            @ToolParam(description = "新优先级 1低 2中 3高 4紧急") Integer priority,
            ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        Task t = taskMapper.selectById(taskId);
        if (t == null || !t.getProjectId().equals(projectId)) {
            return "任务不存在或不属于当前项目";
        }
        if (priority < 1 || priority > 4) return "优先级必须在 1-4 之间";
        t.setPriority(priority);
        taskMapper.updateById(t);
        return String.format("已将任务「%s」的优先级调整为 %d", t.getTitle(), priority);
    }

    @Tool(description = "将任务指派给项目成员，传入成员的用户ID")
    @AiTool("assignTask")
    public String assignTask(
            @ToolParam(description = "任务ID") Long taskId,
            @ToolParam(description = "负责人用户ID") Long assigneeId,
            ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        Task t = taskMapper.selectById(taskId);
        if (t == null || !t.getProjectId().equals(projectId)) {
            return "任务不存在或不属于当前项目";
        }
        t.setAssigneeId(assigneeId);
        taskMapper.updateById(t);
        return String.format("已将任务「%s」指派给用户 #%d", t.getTitle(), assigneeId);
    }

    @Tool(description = "更新任务状态：1=待办，2=进行中，3=已完成，4=已取消")
    @AiTool("updateTaskStatus")
    public String updateTaskStatus(
            @ToolParam(description = "任务ID") Long taskId,
            @ToolParam(description = "新状态 1待办 2进行中 3已完成 4已取消") Integer status,
            ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        Task t = taskMapper.selectById(taskId);
        if (t == null || !t.getProjectId().equals(projectId)) {
            return "任务不存在或不属于当前项目";
        }
        t.setStatus(status);
        taskMapper.updateById(t);
        return String.format("任务「%s」状态已更新为 %d", t.getTitle(), status);
    }
}
