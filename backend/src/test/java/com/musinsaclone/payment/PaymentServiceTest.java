package com.musinsaclone.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.notification.service.NotificationService;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.repository.OrderRepository;
import com.musinsaclone.payment.entity.Payment;
import com.musinsaclone.payment.repository.PaymentRepository;
import com.musinsaclone.payment.service.PaymentService;
import com.musinsaclone.point.service.PointService;
import com.musinsaclone.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PointService pointService;
    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper objectMapper;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository, orderRepository, pointService,
                notificationService, Optional.empty(), objectMapper);
    }

    private User user(Long id) {
        return User.builder().id(id).email("t@t.com").password("p").name("t")
                .role(User.Role.USER).point(0).build();
    }

    private Order order(User user, Order.Status status) {
        return Order.builder().id(1L).user(user).totalPrice(35100).discountPrice(0)
                .pointUsed(0).finalPrice(35100).status(status).build();
    }

    @Test
    @DisplayName("결제 확정 시 주문이 PAID로 바뀌고 1% 포인트가 적립된다")
    void confirmPayment_success() {
        User user = user(1L);
        Order order = order(user, Order.Status.PENDING);
        Payment payment = Payment.builder().id(1L).order(order).method("CARD")
                .amount(35100).status(Payment.Status.READY).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        paymentService.confirmPayment(1L, 1L, "TX-1");

        org.assertj.core.api.Assertions.assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
        org.assertj.core.api.Assertions.assertThat(payment.getStatus()).isEqualTo(Payment.Status.DONE);
        verify(pointService).earn(eq(user), eq(351), anyString());   // 35100 * 1% = 351
    }

    @Test
    @DisplayName("이미 결제된 주문을 재확정하면 예외가 발생하고 포인트가 중복 적립되지 않는다")
    void confirmPayment_idempotent() {
        User user = user(1L);
        Order order = order(user, Order.Status.PAID);   // 이미 결제 완료
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, 1L, "TX-DUP"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 처리");
        verify(pointService, never()).earn(any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("다른 사용자의 주문은 결제 확정할 수 없다")
    void confirmPayment_othersOrder() {
        Order order = order(user(2L), Order.Status.PENDING);  // 소유자는 2L
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.confirmPayment(1L, 1L, "TX-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
        verify(pointService, never()).earn(any(), anyInt(), anyString());
    }
}
