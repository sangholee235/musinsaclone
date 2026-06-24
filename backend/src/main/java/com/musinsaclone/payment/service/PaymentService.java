package com.musinsaclone.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.notification.service.NotificationService;
import com.musinsaclone.order.repository.OrderRepository;
import com.musinsaclone.payment.entity.Payment;
import com.musinsaclone.payment.repository.PaymentRepository;
import com.musinsaclone.point.service.PointService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int EARN_RATE_PERCENT = 1; // 결제 금액의 1% 적립

    @Transactional
    public void preparePayment(Long userId, Long orderId, String method) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        if (!order.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        if (order.getStatus() != Order.Status.PENDING) throw BusinessException.badRequest("결제할 수 없는 주문입니다.");

        paymentRepository.save(Payment.builder()
                .order(order)
                .method(method)
                .amount(order.getFinalPrice())
                .status(Payment.Status.READY)
                .build());
    }

    @Transactional
    public void confirmPayment(Long userId, Long orderId, String pgTxId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        if (!order.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        // 멱등성 보장: 이미 결제 완료/취소된 주문에 재확정 시 포인트 중복 적립을 막는다.
        if (order.getStatus() != Order.Status.PENDING) {
            throw BusinessException.badRequest("이미 처리된 주문입니다.");
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("결제 정보를 찾을 수 없습니다."));

        payment.complete(pgTxId);
        order.updateStatus(Order.Status.PAID);

        int earned = order.getFinalPrice() * EARN_RATE_PERCENT / 100;
        pointService.earn(order.getUser(), earned, "구매 적립");

        if (kafkaTemplate.isPresent()) {
            try {
                String event = objectMapper.writeValueAsString(
                        Map.of("userId", userId, "orderId", orderId, "status", "PAID"));
                kafkaTemplate.get().send("order.status.changed", event);
            } catch (JsonProcessingException ignored) {}
        } else {
            notificationService.notifyOrderStatus(order.getUser(), "결제가 완료되었습니다.");
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        if (!order.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> BusinessException.notFound("결제 정보를 찾을 수 없습니다."));
        return new PaymentResponse(payment);
    }

    @Getter
    public static class PaymentResponse {
        private final Long paymentId;
        private final String method;
        private final int amount;
        private final String status;
        private final String paidAt;

        public PaymentResponse(Payment payment) {
            this.paymentId = payment.getId();
            this.method = payment.getMethod();
            this.amount = payment.getAmount();
            this.status = payment.getStatus().name();
            this.paidAt = payment.getPaidAt() != null ? payment.getPaidAt().toString() : null;
        }
    }
}
