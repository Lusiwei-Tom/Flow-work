package com.flowwork.modules.realtime.controller;

import com.flowwork.common.result.Result;
import com.flowwork.common.security.SecurityUtils;
import com.flowwork.modules.realtime.entity.Notification;
import com.flowwork.modules.realtime.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 站内通知（个人数据，登录即可访问自己的）
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<List<Notification>> list(@RequestParam(defaultValue = "false") boolean onlyUnread) {
        return Result.ok(notificationService.list(SecurityUtils.getCurrentUserId(), onlyUnread));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Long>> unreadCount() {
        return Result.ok(Map.of("count", notificationService.unreadCount(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
