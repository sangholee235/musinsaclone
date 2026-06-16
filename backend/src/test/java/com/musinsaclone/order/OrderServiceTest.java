package com.musinsaclone.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.cart.repository.CartItemRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.entity.OrderItem;
import com.musinsaclone.order.repository.OrderItemRepository;
import com.musinsaclone.order.repository.OrderRepository;
import com.musinsaclone.order.service.OrderService;
import com.musinsaclone.notification.service.NotificationService;
import com.musinsaclone.point.service.PointService;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductOption;
import com.musinsaclone.product.repository.ProductOptionRepository;
import com.musinsaclone.user.entity.Address;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.AddressRepository;
import com.musinsaclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private ProductOptionRepository productOptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private PointService pointService;
    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper objectMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, productOptionRepository,
                userRepository, addressRepository, userCouponRepository, cartItemRepository,
                pointService, notificationService, Optional.empty(), objectMapper);
    }

    private User user(int point) {
        return User.builder().id(1L).email("t@t.com").password("p").name("t")
                .role(User.Role.USER).point(point).build();
    }

    private ProductOption option(int stock) {
        Product product = Product.builder().id(1L).name("상품").price(10000).discountRate(10)
                .status(Product.Status.ON_SALE).build(); // discountedPrice = 9000
        return ProductOption.builder().id(1L).product(product).size("M").color("블랙")
                .stock(stock).extraPrice(0).build();
    }

    private OrderService.CreateOrderRequest request(int pointUsed, int quantity) {
        OrderService.OrderItemDto item = mock(OrderService.OrderItemDto.class);
        lenient().when(item.getProductOptionId()).thenReturn(1L);
        lenient().when(item.getQuantity()).thenReturn(quantity);
        OrderService.CreateOrderRequest req = mock(OrderService.CreateOrderRequest.class);
        lenient().when(req.getAddressId()).thenReturn(1L);
        lenient().when(req.getItems()).thenReturn(List.of(item));
        lenient().when(req.getUserCouponId()).thenReturn(null);
        lenient().when(req.getPointUsed()).thenReturn(pointUsed);
        return req;
    }

    @Test
    @DisplayName("주문 생성 시 금액이 계산되고 재고 차감·포인트 사용·장바구니 비움이 일어난다")
    void createOrder_success() {
        User user = user(10000);
        ProductOption option = option(10);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(option));
        when(productOptionRepository.getReferenceById(1L)).thenReturn(option);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(1L, request(1000, 1));

        // 가격 계산 검증: 상품 9000 * 1 - 포인트 1000 = 8000
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualTo(9000);
        assertThat(saved.getPointUsed()).isEqualTo(1000);
        assertThat(saved.getFinalPrice()).isEqualTo(8000);

        assertThat(option.getStock()).isEqualTo(9);                 // 재고 차감
        verify(pointService).use(user, 1000, "주문 결제 시 사용");   // 포인트 사용
        verify(orderItemRepository).save(any(OrderItem.class));     // 주문 항목 저장
        verify(cartItemRepository).deleteByUserId(1L);              // 장바구니 비움
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 예외가 발생한다")
    void createOrder_insufficientStock() {
        User user = user(10000);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(option(1)));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(0, 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("재고");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("보유 포인트보다 많이 사용하면 예외가 발생한다")
    void createOrder_insufficientPoint() {
        User user = user(500);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findById(1L)).thenReturn(Optional.of(option(10)));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(1000, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("포인트");
        verify(pointService, never()).use(any(), anyInt(), any());
        verify(orderRepository, never()).save(any());
    }
}
