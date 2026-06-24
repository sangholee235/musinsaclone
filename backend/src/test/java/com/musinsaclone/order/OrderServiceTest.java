package com.musinsaclone.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.cart.repository.CartItemRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.entity.Coupon;
import com.musinsaclone.coupon.entity.UserCoupon;
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
    @Mock private com.musinsaclone.payment.repository.PaymentRepository paymentRepository;
    @Mock private PointService pointService;
    @Mock private NotificationService notificationService;
    @Mock private ObjectMapper objectMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderItemRepository, productOptionRepository,
                userRepository, addressRepository, userCouponRepository, cartItemRepository,
                paymentRepository, pointService, notificationService, Optional.empty(), objectMapper);
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
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option));
        when(productOptionRepository.getReferenceById(1L)).thenReturn(option);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.createOrder(1L, request(1000, 1));

        // 가격 계산 검증: 상품 9000(3만원 미만 → 배송비 3000) - 포인트 1000 = 11000
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertThat(saved.getTotalPrice()).isEqualTo(9000);
        assertThat(saved.getPointUsed()).isEqualTo(1000);
        assertThat(saved.getShippingFee()).isEqualTo(3000);
        assertThat(saved.getFinalPrice()).isEqualTo(11000);

        assertThat(option.getStock()).isEqualTo(9);                 // 재고 차감
        verify(pointService).use(user, 1000, "주문 결제 시 사용");   // 포인트 사용
        verify(orderItemRepository).save(any(OrderItem.class));     // 주문 항목 저장
        verify(cartItemRepository).deleteByUserId(1L);              // 장바구니 비움
    }

    @Test
    @DisplayName("결제금액이 3만원 이상이면 배송비가 무료다")
    void createOrder_freeShipping() {
        User user = user(0);
        ProductOption option = option(10); // 상품가 9000
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option));
        when(productOptionRepository.getReferenceById(1L)).thenReturn(option);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // 수량 4 → 36000원 (3만원 이상)
        orderService.createOrder(1L, request(0, 4));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getTotalPrice()).isEqualTo(36000);
        assertThat(saved.getShippingFee()).isEqualTo(0);
        assertThat(saved.getFinalPrice()).isEqualTo(36000);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 주문하면 예외가 발생한다")
    void createOrder_insufficientStock() {
        User user = user(10000);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option(1)));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(0, 5)))
                .isInstanceOf(BusinessException.class)
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
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option(10)));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(1000, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("포인트");
        verify(pointService, never()).use(any(), anyInt(), any());
        verify(orderRepository, never()).save(any());
    }

    private OrderService.CreateOrderRequest requestWithCoupon(Long userCouponId) {
        OrderService.OrderItemDto item = mock(OrderService.OrderItemDto.class);
        lenient().when(item.getProductOptionId()).thenReturn(1L);
        lenient().when(item.getQuantity()).thenReturn(1);
        OrderService.CreateOrderRequest req = mock(OrderService.CreateOrderRequest.class);
        lenient().when(req.getAddressId()).thenReturn(1L);
        lenient().when(req.getItems()).thenReturn(List.of(item));
        lenient().when(req.getUserCouponId()).thenReturn(userCouponId);
        lenient().when(req.getPointUsed()).thenReturn(0);
        return req;
    }

    private void stubUserAddressOption(User user) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option(10))); // 상품가 9000
    }

    @Test
    @DisplayName("본인의 쿠폰이 아니면 주문에 사용할 수 없다")
    void createOrder_othersCoupon() {
        User user = user(10000);
        stubUserAddressOption(user);
        UserCoupon uc = mock(UserCoupon.class);
        User other = User.builder().id(2L).email("o@o.com").password("p").name("o")
                .role(User.Role.USER).point(0).build();
        when(uc.getUser()).thenReturn(other);
        when(userCouponRepository.findById(5L)).thenReturn(Optional.of(uc));

        assertThatThrownBy(() -> orderService.createOrder(1L, requestWithCoupon(5L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("본인의 쿠폰");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("최소 주문금액 미달 시 쿠폰을 사용할 수 없고 쿠폰이 소진되지 않는다")
    void createOrder_couponMinOrderNotMet() {
        User user = user(10000);
        stubUserAddressOption(user);
        UserCoupon uc = mock(UserCoupon.class);
        when(uc.getUser()).thenReturn(user);
        when(uc.isUsed()).thenReturn(false);
        Coupon coupon = mock(Coupon.class);
        when(coupon.isValid()).thenReturn(true);
        when(coupon.getMinOrderPrice()).thenReturn(100_000); // 주문가 9000 미달
        when(uc.getCoupon()).thenReturn(coupon);
        when(userCouponRepository.findById(5L)).thenReturn(Optional.of(uc));

        assertThatThrownBy(() -> orderService.createOrder(1L, requestWithCoupon(5L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("최소 주문금액");
        verify(uc, never()).use();
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("판매 중이 아닌(숨김/품절) 상품은 주문할 수 없다")
    void createOrder_notOnSale() {
        User user = user(10000);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        Product hidden = Product.builder().id(1L).name("숨김상품").price(10000).discountRate(0)
                .status(Product.Status.HIDDEN).build();
        ProductOption opt = ProductOption.builder().id(1L).product(hidden).size("M").color("블랙")
                .stock(10).extraPrice(0).build();
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(opt));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(0, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("판매");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 사용자의 배송지로는 주문할 수 없다")
    void createOrder_othersAddress() {
        User owner = User.builder().id(2L).email("o@o.com").password("p").name("o")
                .role(User.Role.USER).point(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(10000)));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(owner).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));

        assertThatThrownBy(() -> orderService.createOrder(1L, request(0, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("배송지");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("포인트 사용액이 결제금액을 초과하면 예외가 발생한다")
    void createOrder_pointExceedsPayable() {
        User user = user(10000);              // 보유 포인트는 충분
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));
        when(productOptionRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(option(10)));

        // 상품가 9000 < 사용 포인트 9500 (보유 10000 이내)
        assertThatThrownBy(() -> orderService.createOrder(1L, request(9500, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("결제금액");
        verify(pointService, never()).use(any(), anyInt(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 항목이 비어 있으면 예외가 발생한다")
    void createOrder_emptyItems() {
        User user = user(10000);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(addressRepository.findById(1L)).thenReturn(Optional.of(
                Address.builder().id(1L).user(user).name("집").recipient("r").phone("p")
                        .zipcode("12345").address1("주소").isDefault(true).build()));

        OrderService.CreateOrderRequest req = mock(OrderService.CreateOrderRequest.class);
        when(req.getAddressId()).thenReturn(1L);
        when(req.getItems()).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("상품");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("주문 취소 시 재고·포인트·쿠폰이 모두 복구된다")
    void cancelOrder_restoresStockPointCoupon() {
        User user = user(0);
        UserCoupon coupon = mock(UserCoupon.class);
        Order order = Order.builder().id(1L).user(user).userCoupon(coupon)
                .totalPrice(9000).discountPrice(1000).pointUsed(500).finalPrice(7500)
                .status(Order.Status.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        ProductOption option = option(10);
        OrderItem item = mock(OrderItem.class);
        when(item.getProductOption()).thenReturn(option);
        when(item.getQuantity()).thenReturn(2);
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of(item));

        orderService.cancelOrder(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
        assertThat(option.getStock()).isEqualTo(12);                    // 재고 10 + 2 복구
        verify(pointService).earn(user, 500, "주문 취소 환불");          // 포인트 환불
        verify(coupon).restore();                                       // 쿠폰 복구
    }

    @Test
    @DisplayName("결제 완료 주문 취소 시 결제가 취소되고 적립 포인트가 보유 잔액 이내로 회수된다")
    void cancelOrder_paid_reclaimsEarnedPoint() {
        User user = user(1000);   // 보유 1000P
        Order order = Order.builder().id(1L).user(user)
                .totalPrice(50000).discountPrice(0).pointUsed(0).finalPrice(50000)
                .status(Order.Status.PAID).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderId(1L)).thenReturn(List.of());
        com.musinsaclone.payment.entity.Payment payment =
                mock(com.musinsaclone.payment.entity.Payment.class);
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment));

        orderService.cancelOrder(1L, 1L);

        assertThat(order.getStatus()).isEqualTo(Order.Status.CANCELLED);
        verify(payment).cancel();                                  // 결제 취소
        // 적립분 50000*1%=500 이지만 보유 1000P 이내이므로 500 회수
        verify(pointService).use(user, 500, "주문 취소 - 적립 포인트 회수");
    }

    @Test
    @DisplayName("배송 중 주문은 취소할 수 없다")
    void cancelOrder_shipping_rejected() {
        User user = user(0);
        Order order = Order.builder().id(1L).user(user)
                .totalPrice(9000).discountPrice(0).pointUsed(0).finalPrice(9000)
                .status(Order.Status.SHIPPING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("취소할 수 없");
        verify(orderItemRepository, never()).findByOrderId(any());
    }

    @Test
    @DisplayName("다른 사용자의 주문은 취소할 수 없다")
    void cancelOrder_othersOrder() {
        User owner = User.builder().id(2L).email("o@o.com").password("p").name("o")
                .role(User.Role.USER).point(0).build();
        Order order = Order.builder().id(1L).user(owner)
                .totalPrice(9000).discountPrice(0).pointUsed(0).finalPrice(9000)
                .status(Order.Status.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
        verify(pointService, never()).earn(any(), anyInt(), any());
    }
}
