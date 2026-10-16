package com.flowwork.modules.ai.service;

import com.flowwork.modules.project.entity.Task;
import com.flowwork.modules.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 周报/分析数据聚合：纯数据库统计，供 AI 生成内容使用
 */
@Service
@RequiredArgsConstructor
public class ProjectDataGatherService {

    private final TaskMapper taskMapper;

    public Map<String, Object> gather(Long projectId) {
        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> byStatus = taskMapper.countByStatus(projectId);
        long total = 0, finished = 0, doing = 0, todo = 0;
        for (Map<String, Object> row : byStatus) {
            long cnt = ((Number) row.get("cnt")).longValue();
            int st = ((Number) row.get("status")).intValue();
            total += cnt;
            if (st == 3) finished += cnt;
            else if (st == 2) doing += cnt;
            else if (st == 1) todo += cnt;
        }
        data.put("totalTasks", total);
        data.put("finished", finished);
        data.put("doing", doing);
        data.put("todo", todo);
        data.put("completionRate", total == 0 ? 0 : Math.round(100.0 * finished / total));

        // 近 7 天完成趋势
        data.put("dailyFinished", taskMapper.dailyFinished(projectId, LocalDate.now().minusDays(7)));

        // 逾期 / 临期
        List<Task> overdue = taskMapper.selectOverdue(projectId);
        data.put("overdueCount", overdue.size());
        data.put("overdueTasks", overdue.stream().map(Task::getTitle).limit(10).toList());
        data.put("dueSoonCount", taskMapper.selectDueSoon(projectId, LocalDate.now().plusDays(3)).size());

        // 成员任务统计
        data.put("memberStats", taskMapper.memberTaskStats(projectId));
        return data;
    }
}
