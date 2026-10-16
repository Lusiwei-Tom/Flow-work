package com.flowwork.modules.ai.service;

import com.flowwork.modules.ai.dto.RiskReport;
import com.flowwork.modules.project.entity.Project;
import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目风险规则引擎（规则兜底，AI 负责综合解读）
 */
@Service
@RequiredArgsConstructor
public class RiskRuleService {

    private final TaskMapper taskMapper;

    public List<RiskReport.RiskItem> evaluate(Project project) {
        Long projectId = project.getId();
        List<RiskReport.RiskItem> items = new ArrayList<>();
        List<Task> all = taskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Task>()
                        .eq(Task::getProjectId, projectId).ne(Task::getStatus, 4));

        // 规则1：逾期未完成
        List<Task> overdue = taskMapper.selectOverdue(projectId);
        if (!overdue.isEmpty()) {
            items.add(RiskReport.RiskItem.builder().rule("OVERDUE_TASK").level("HIGH")
                    .detail("有 " + overdue.size() + " 个任务已逾期：" +
                            overdue.stream().limit(5).map(Task::getTitle)
                                    .collect(Collectors.joining("、")))
                    .build());
        }

        // 规则2：3 天内临期
        List<Task> dueSoon = taskMapper.selectDueSoon(projectId, LocalDate.now().plusDays(3));
        if (!dueSoon.isEmpty()) {
            items.add(RiskReport.RiskItem.builder().rule("DUE_SOON").level("MEDIUM")
                    .detail("有 " + dueSoon.size() + " 个任务将在 3 天内到期")
                    .build());
        }

        // 规则3：成员负载过高（未完成 >= 8）
        Map<Long, List<Task>> byAssignee = all.stream()
                .filter(t -> t.getAssigneeId() != null && t.getStatus() != 3)
                .collect(Collectors.groupingBy(Task::getAssigneeId));
        byAssignee.forEach((uid, tasks) -> {
            if (tasks.size() >= 8) {
                items.add(RiskReport.RiskItem.builder().rule("MEMBER_OVERLOADED").level("MEDIUM")
                        .detail("成员 #" + uid + " 有 " + tasks.size() + " 个未完成任务，可能负载过高")
                        .build());
            }
        });

        // 规则4：高/紧急优先级任务无负责人
        long noOwner = all.stream()
                .filter(t -> t.getAssigneeId() == null && (t.getPriority() == 3 || t.getPriority() == 4))
                .count();
        if (noOwner > 0) {
            items.add(RiskReport.RiskItem.builder().rule("HIGH_PRIORITY_NO_OWNER").level("MEDIUM")
                    .detail("有 " + noOwner + " 个高优先级任务尚未分配负责人")
                    .build());
        }

        // 规则5：项目进度过半但完成率低
        if (project.getStartDate() != null && project.getEndDate() != null) {
            long total = ChronoUnit.DAYS.between(project.getStartDate(), project.getEndDate());
            long elapsed = ChronoUnit.DAYS.between(project.getStartDate(), LocalDate.now());
            if (total > 0 && elapsed > total / 2 && !all.isEmpty()) {
                long finished = all.stream().filter(t -> t.getStatus() == 3).count();
                double rate = (double) finished / all.size();
                if (rate < 0.3) {
                    items.add(RiskReport.RiskItem.builder().rule("LOW_PROGRESS").level("HIGH")
                            .detail(String.format("项目周期已过 %.0f%%，但任务完成率仅 %.0f%%",
                                    100.0 * elapsed / total, rate * 100))
                            .build());
                }
            }
        }

        // 规则6：高优先级任务占比过高（>40%）
        if (!all.isEmpty()) {
            long highCount = all.stream().filter(t -> t.getPriority() >= 3 && t.getStatus() != 3).count();
            if ((double) highCount / all.size() > 0.4) {
                items.add(RiskReport.RiskItem.builder().rule("TOO_MANY_HIGH_PRIORITY").level("LOW")
                        .detail("高优先级任务占比超过 40%，优先级可能失真")
                        .build());
            }
        }
        return items;
    }

    public String levelOf(List<RiskReport.RiskItem> items) {
        if (items.stream().anyMatch(i -> "HIGH".equals(i.getLevel()))) return "HIGH";
        if (items.stream().anyMatch(i -> "MEDIUM".equals(i.getLevel()))) return "MEDIUM";
        return "LOW";
    }

    public int scoreOf(List<RiskReport.RiskItem> items) {
        int score = 100;
        for (RiskReport.RiskItem i : items) {
            score += switch (i.getLevel()) {
                case "HIGH" -> -20;
                case "MEDIUM" -> -10;
                default -> -5;
            };
        }
        return Math.max(0, score);
    }
}
