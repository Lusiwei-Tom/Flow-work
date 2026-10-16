package com.flowwork.modules.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.flowwork.modules.ai.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
