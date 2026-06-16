package com.musinsaclone.notification.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.notification.entity.Notification;
import com.musinsaclone.notification.repository.NotificationRepository;
import com.musinsaclone.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationResponse::new);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    @Transactional
    public void readNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> BusinessException.notFound("알림을 찾을 수 없습니다."));
        if (!notification.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        notification.read();
    }

    @Transactional
    public void readAll(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // Kafka 가 비활성(local/test)일 때 주문 흐름에서 직접 호출해 알림을 생성한다.
    @Transactional
    public void notifyOrderStatus(User user, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(Notification.Type.ORDER_STATUS)
                .message(message)
                .isRead(false)
                .build());
    }

    @Getter
    public static class NotificationResponse {
        private final Long notificationId;
        private final String type;
        private final String message;
        private final boolean isRead;
        private final String createdAt;

        public NotificationResponse(Notification notification) {
            this.notificationId = notification.getId();
            this.type = notification.getType().name();
            this.message = notification.getMessage();
            this.isRead = notification.isRead();
            this.createdAt = notification.getCreatedAt().toString();
        }

        @JsonProperty("isRead")
        public boolean isRead() {
            return isRead;
        }
    }
}
