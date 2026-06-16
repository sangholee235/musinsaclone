package com.musinsaclone.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.notification.entity.Notification;
import com.musinsaclone.notification.repository.NotificationRepository;
import com.musinsaclone.order.event.OrderCreatedEvent;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers", matchIfMissing = false)
public class NotificationConsumer {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "musinsa-group")
    public void handleOrderCreated(String message) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
            User user = userRepository.findById(event.getUserId()).orElseThrow();

            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(Notification.Type.ORDER_STATUS)
                    .message(String.format("주문이 완료되었습니다. 결제금액: %,d원", event.getFinalPrice()))
                    .isRead(false)
                    .build());
        } catch (Exception e) {
            log.error("알림 처리 실패: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "order.status.changed", groupId = "musinsa-group")
    public void handleOrderStatusChanged(String message) {
        try {
            var node = objectMapper.readTree(message);
            Long userId = node.get("userId").asLong();
            String status = node.get("status").asText();

            User user = userRepository.findById(userId).orElseThrow();
            String notificationMessage = switch (status) {
                case "SHIPPING" -> "상품이 배송 중입니다.";
                case "DELIVERED" -> "상품이 배송 완료되었습니다.";
                case "CANCELLED" -> "주문이 취소되었습니다.";
                default -> "주문 상태가 변경되었습니다.";
            };

            notificationRepository.save(Notification.builder()
                    .user(user)
                    .type(Notification.Type.ORDER_STATUS)
                    .message(notificationMessage)
                    .isRead(false)
                    .build());
        } catch (Exception e) {
            log.error("주문 상태 알림 처리 실패: {}", e.getMessage());
        }
    }
}
