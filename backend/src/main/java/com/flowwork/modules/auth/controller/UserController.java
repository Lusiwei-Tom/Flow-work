package com.flowwork.modules.auth.controller;

import com.flowwork.common.result.Result;
import com.flowwork.modules.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户搜索（邀请成员用，登录即可调用，只返回 id/username/nickname）
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;

    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam("keyword") String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return Result.ok(List.of());
        }
        return Result.ok(userMapper.searchUsers(kw));
    }
}
