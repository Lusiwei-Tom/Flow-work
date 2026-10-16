package com.flowwork.modules.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.project.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    /** DAG：查询一个项目内全部父子关系（用于循环检测） */
    @Select("SELECT id, parent_id FROM task WHERE project_id = #{projectId} AND parent_id IS NOT NULL")
    List<Map<String, Object>> selectEdges(@Param("projectId") Long projectId);

    @Select("SELECT id FROM task WHERE project_id = #{projectId} AND assignee_id = #{userId} AND status <> 3")
    List<Long> selectUnfinishedByAssignee(@Param("projectId") Long projectId, @Param("userId") Long userId);

    /** 逾期未完成任务 */
    @Select("SELECT * FROM task WHERE project_id = #{projectId} AND status IN (1,2) AND due_date < CURDATE() ORDER BY due_date")
    List<Task> selectOverdue(@Param("projectId") Long projectId);

    /** 临期（N 天内到期且未完成） */
    @Select("SELECT * FROM task WHERE project_id = #{projectId} AND status IN (1,2) " +
            "AND due_date BETWEEN CURDATE() AND #{deadline} ORDER BY due_date")
    List<Task> selectDueSoon(@Param("projectId") Long projectId, @Param("deadline") LocalDate deadline);

    @Update("UPDATE task SET column_id = #{columnId}, sort_order = #{sortOrder}, updated_at = NOW() WHERE id = #{taskId}")
    int updatePosition(@Param("taskId") Long taskId, @Param("columnId") Long columnId,
                       @Param("sortOrder") Integer sortOrder);

    // ---------- analytics 复用 ----------
    @Select("SELECT status, COUNT(*) cnt FROM task WHERE project_id = #{projectId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("projectId") Long projectId);

    @Select("SELECT priority, COUNT(*) cnt FROM task WHERE project_id = #{projectId} GROUP BY priority")
    List<Map<String, Object>> countByPriority(@Param("projectId") Long projectId);

    @Select("""
            SELECT u.id userId, u.nickname nickname,
                   COUNT(t.id) total,
                   SUM(CASE WHEN t.status = 3 THEN 1 ELSE 0 END) finished
            FROM sys_user u
            JOIN project_member pm ON pm.user_id = u.id AND pm.project_id = #{projectId}
            LEFT JOIN task t ON t.assignee_id = u.id AND t.project_id = #{projectId}
            GROUP BY u.id, u.nickname
            """)
    List<Map<String, Object>> memberTaskStats(@Param("projectId") Long projectId);

    /** 近 N 天每日完成任务数 */
    @Select("""
            SELECT DATE(updated_at) d, COUNT(*) cnt
            FROM task
            WHERE project_id = #{projectId} AND status = 3
              AND updated_at >= #{from}
            GROUP BY DATE(updated_at) ORDER BY d
            """)
    List<Map<String, Object>> dailyFinished(@Param("projectId") Long projectId,
                                            @Param("from") LocalDate from);

    /** 我的任务：跨项目聚合（进行中 > 待办 > 近7天已完成），按截止日期、优先级排序 */
    @Select("""
            SELECT t.id, t.project_id AS projectId, t.column_id AS columnId,
                   t.title, t.description, t.assignee_id AS assigneeId,
                   t.priority, t.status, t.estimated_hours AS estimatedHours,
                   t.due_date AS dueDate, t.parent_id AS parentId,
                   t.sort_order AS sortOrder, t.ai_generated AS aiGenerated,
                   t.ai_source AS aiSource, t.created_by AS createdBy,
                   p.name AS project_name
            FROM task t
            JOIN project p ON p.id = t.project_id
            WHERE t.assignee_id = #{userId} AND p.status = 1
              AND (t.status IN (1, 2) OR (t.status = 3 AND t.updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)))
            ORDER BY FIELD(t.status, 2, 1, 3), t.due_date IS NULL, t.due_date, t.priority DESC
            """)
    List<Map<String, Object>> selectMyTasks(@Param("userId") Long userId);
}
