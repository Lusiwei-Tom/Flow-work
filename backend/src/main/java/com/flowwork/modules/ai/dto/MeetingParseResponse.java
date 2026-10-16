package com.flowwork.modules.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * AI 会议纪要结构化输出
 */
@Data
public class MeetingParseResponse {

    private String summary;

    @JsonProperty(required = true)//用于指定 Java 属性与 JSON 字段之间的映射关系。
    private List<MeetingTask> tasks;

    @Data
    public static class MeetingTask {
        private String title;
        private String description;
        /** 负责人姓名（系统按昵称匹配，可空） */
        private String assigneeName;
        private LocalDate dueDate;
        private Integer priority;
    }
}
