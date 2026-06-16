package com.musinsaclone.admin.service;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.notification.service.NotificationService;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.repository.OrderRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(AdminOrderResponse::new);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        Order.Status newStatus;
        try {
            newStatus = Order.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("유효하지 않은 주문 상태입니다.");
        }
        order.updateStatus(newStatus);
        // 상태 변경을 주문자에게 알림으로 통지 (Kafka 비활성 환경에서도 동작)
        notificationService.notifyOrderStatus(order.getUser(), statusMessage(newStatus));
    }

    private String statusMessage(Order.Status status) {
        return switch (status) {
            case PAID -> "결제가 완료되었습니다.";
            case SHIPPING -> "상품이 배송 중입니다.";
            case DELIVERED -> "상품이 배송 완료되었습니다.";
            case CANCELLED -> "주문이 취소되었습니다.";
            default -> "주문 상태가 변경되었습니다.";
        };
    }

    @Getter
    public static class AdminOrderResponse {
        private final Long orderId;
        private final String userName;
        private final String userEmail;
        private final int finalPrice;
        private final String status;
        private final String createdAt;

        public AdminOrderResponse(Order order) {
            this.orderId = order.getId();
            this.userName = order.getUser().getName();
            this.userEmail = order.getUser().getEmail();
            this.finalPrice = order.getFinalPrice();
            this.status = order.getStatus().name();
            this.createdAt = order.getCreatedAt().toString();
        }
    }
}
