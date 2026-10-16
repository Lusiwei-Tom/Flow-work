package com.flowwork.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.auth.entity.InviteCode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InviteCodeMapper extends BaseMapper<InviteCode> {

    /** 原子递增已用次数 */
    @Update("UPDATE invite_code SET used_count = used_count + 1 WHERE id = #{id}")
    int incrementUsedCount(@Param("id") Long id);
}
