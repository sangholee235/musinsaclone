package com.musinsaclone.notification.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<Page<NotificationService.NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(notificationService.getNotifications(userId, pageable));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> readNotification(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long notificationId) {
        notificationService.readNotification(userId, notificationId);
        return ApiResponse.ok();
    }

    @PatchMapping("/read-all")
    public ApiResponse<Void> readAll(@AuthenticationPrincipal Long userId) {
        notificationService.readAll(userId);
        return ApiResponse.ok();
    }
}
