package com.flowwork.modules.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flowwork.common.annotation.AiTool;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 项目管家 —— 查询类工具
 * <p>
 * 项目 ID 通过 Spring AI ToolContext 由调用方（AiService）随调用链注入，
 * 替代原 ThreadLocal 方案（工具在框架线程池执行时 ThreadLocal 会丢值导致查空）。
 */
@Component
@RequiredArgsConstructor
public class ProjectQueryTool {

    private final TaskMapper taskMapper;

    /** 从 ToolContext 取出当前项目 ID（AiService 通过 .toolContext() 传入） */
    private static Long projectId(ToolContext tc) {
        return (Long) tc.getContext().get("projectId");
    }

    @Tool(description = "查询当前项目下的任务列表，可按状态过滤：status 传 1=待办, 2=进行中, 3=已完成，不传查全部未完成任务")
    @AiTool("queryProjectTasks")
    public String queryProjectTasks(
            @ToolParam(description = "任务状态：1待办 2进行中 3已完成，可空") Integer status,
            ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        LambdaQueryWrapper<Task> w = new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .ne(Task::getStatus, 4)
                .eq(status != null, Task::getStatus, status)
                .orderByAsc(Task::getPriority);
        List<Task> tasks = taskMapper.selectList(w);
        if (tasks.isEmpty()) return "当前项目没有符合条件的任务";
        return tasks.stream().map(t -> String.format("#%d [%s] %s（优先级:%d, 截止:%s）",
                t.getId(), statusText(t.getStatus()), t.getTitle(), t.getPriority(),
                t.getDueDate() == null ? "无" : t.getDueDate())).collect(Collectors.joining("\n"));
    }

    @Tool(description = "查询当前项目所有已逾期（超过截止日期且未完成）的任务")
    @AiTool("queryOverdueTasks")
    public String queryOverdueTasks(ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        List<Task> overdue = taskMapper.selectOverdue(projectId);
        if (overdue.isEmpty()) return "当前没有逾期任务";
        return overdue.stream().map(t -> String.format("#%d %s（截止:%s）",
                t.getId(), t.getTitle(), t.getDueDate())).collect(Collectors.joining("\n"));
    }

    @Tool(description = "根据关键词搜索当前项目的任务标题或描述")
    @AiTool("searchTasks")
    public String searchTasks(@ToolParam(description = "搜索关键词") String keyword,
                              ToolContext toolContext) {
        Long projectId = projectId(toolContext);
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getProjectId, projectId)
                .ne(Task::getStatus, 4)
                .and(w -> w.like(Task::getTitle, keyword).or().like(Task::getDescription, keyword)));
        if (tasks.isEmpty()) return "未找到包含「" + keyword + "」的任务";
        return tasks.stream().map(t -> "#" + t.getId() + " " + t.getTitle())
                .collect(Collectors.joining("\n"));
    }

    private String statusText(Integer s) {
        return switch (s) {
            case 1 -> "待办";
            case 2 -> "进行中";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }
}
