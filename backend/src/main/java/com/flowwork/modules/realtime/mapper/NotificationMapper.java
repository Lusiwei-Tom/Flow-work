package com.flowwork.modules.realtime.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.realtime.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /** 同一天内同任务同类型只提醒一次（定时任务去重用） */
    @Select("""
            SELECT COUNT(*) FROM notification
            WHERE user_id = #{userId} AND task_id = #{taskId} AND type = #{type}
              AND created_at >= CURDATE()
            """)
    int countToday(@Param("userId") Long userId, @Param("taskId") Long taskId, @Param("type") String type);
}
