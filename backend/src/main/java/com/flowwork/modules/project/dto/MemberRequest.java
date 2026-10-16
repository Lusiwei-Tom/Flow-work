package com.flowwork.modules.project.dto;

import lombok.Data;

/**
 * 项目成员请求参数
 */
@Data
public class MemberRequest {
    /** 邀请时的目标用户名 */
    private String username;
    /** MANAGER / MEMBER / OBSERVER */
    private String projectRole;
}
