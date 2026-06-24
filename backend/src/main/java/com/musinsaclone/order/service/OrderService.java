package com.musinsaclone.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musinsaclone.cart.repository.CartItemRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.entity.UserCoupon;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.entity.OrderItem;
import com.musinsaclone.order.event.OrderCreatedEvent;
import com.musinsaclone.order.repository.OrderItemRepository;
import com.musinsaclone.order.repository.OrderRepository;
import com.musinsaclone.notification.service.NotificationService;
import com.musinsaclone.payment.entity.Payment;
import com.musinsaclone.payment.repository.PaymentRepository;
import com.musinsaclone.point.service.PointService;
import com.musinsaclone.product.entity.ProductOption;
import com.musinsaclone.product.repository.ProductOptionRepository;
import com.musinsaclone.user.entity.Address;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.AddressRepository;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserCouponRepository userCouponRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final int EARN_RATE_PERCENT = 1; // 결제 시 적립률 (PaymentService 와 동일)
    private static final int FREE_SHIPPING_THRESHOLD = 30_000; // 무료배송 기준 금액
    private static final int SHIPPING_FEE = 3_000; // 기준 미만 시 배송비

    @Transactional
    public Long createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (request.getAddressId() == null) throw BusinessException.badRequest("배송지를 선택해주세요.");
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> BusinessException.notFound("주소를 찾을 수 없습니다."));
        if (!address.getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("본인의 배송지가 아닙니다.");
        }

        List<OrderItemDto> itemDtos = request.getItems();
        if (itemDtos == null || itemDtos.isEmpty()) {
            throw BusinessException.badRequest("주문할 상품이 없습니다.");
        }

        int totalPrice = 0;
        for (OrderItemDto dto : itemDtos) {
            if (dto.getQuantity() <= 0) throw BusinessException.badRequest("수량은 1개 이상이어야 합니다.");
            // 동시 주문 시 오버셀을 막기 위해 비관적 락으로 옵션을 조회한다.
            ProductOption option = productOptionRepository.findByIdForUpdate(dto.getProductOptionId())
                    .orElseThrow(() -> BusinessException.notFound("상품 옵션을 찾을 수 없습니다."));
            if (option.getProduct().getStatus() != com.musinsaclone.product.entity.Product.Status.ON_SALE) {
                throw BusinessException.badRequest(option.getProduct().getName() + " 은(는) 현재 판매 중이 아닙니다.");
            }
            if (option.getStock() < dto.getQuantity()) {
                throw BusinessException.badRequest(
                        option.getProduct().getName() + " 옵션의 재고가 부족합니다. (남은 수량: " + option.getStock() + ")");
            }
            option.decreaseStock(dto.getQuantity());
            totalPrice += (option.getProduct().getDiscountedPrice() + option.getExtraPrice()) * dto.getQuantity();
        }

        int discountPrice = 0;
        UserCoupon userCoupon = null;
        if (request.getUserCouponId() != null) {
            userCoupon = userCouponRepository.findById(request.getUserCouponId())
                    .orElseThrow(() -> BusinessException.notFound("쿠폰을 찾을 수 없습니다."));
            if (!userCoupon.getUser().getId().equals(userId)) {
                throw BusinessException.forbidden("본인의 쿠폰이 아닙니다.");
            }
            if (userCoupon.isUsed()) throw BusinessException.badRequest("이미 사용한 쿠폰입니다.");
            var coupon = userCoupon.getCoupon();
            if (!coupon.isValid()) throw BusinessException.badRequest("사용 기간이 아닌 쿠폰입니다.");
            if (totalPrice < coupon.getMinOrderPrice()) {
                throw BusinessException.badRequest(
                        String.format("최소 주문금액 %,d원 이상부터 사용할 수 있는 쿠폰입니다.", coupon.getMinOrderPrice()));
            }
            discountPrice = coupon.calculateDiscount(totalPrice);
            userCoupon.use();
        }

        int pointUsed = request.getPointUsed();
        if (pointUsed < 0) throw BusinessException.badRequest("포인트 사용액이 올바르지 않습니다.");
        if (pointUsed > user.getPoint()) throw BusinessException.badRequest("포인트가 부족합니다.");
        if (pointUsed > totalPrice - discountPrice) {
            throw BusinessException.badRequest("포인트 사용액이 결제금액을 초과할 수 없습니다.");
        }
        if (pointUsed > 0) pointService.use(user, pointUsed, "주문 결제 시 사용");

        // 배송비 정책: 상품 결제금액(할인 후)이 기준 미만이면 배송비 부과.
        int shippingFee = (totalPrice - discountPrice) >= FREE_SHIPPING_THRESHOLD ? 0 : SHIPPING_FEE;
        int finalPrice = totalPrice - discountPrice - pointUsed + shippingFee;

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .address(address)
                .userCoupon(userCoupon)
                .totalPrice(totalPrice)
                .discountPrice(discountPrice)
                .pointUsed(pointUsed)
                .shippingFee(shippingFee)
                .finalPrice(finalPrice)
                .status(Order.Status.PENDING)
                .build());

        for (OrderItemDto dto : itemDtos) {
            ProductOption option = productOptionRepository.getReferenceById(dto.getProductOptionId());
            int price = (option.getProduct().getDiscountedPrice() + option.getExtraPrice()) * dto.getQuantity();
            orderItemRepository.save(OrderItem.builder()
                    .order(order)
                    .productOption(option)
                    .quantity(dto.getQuantity())
                    .price(price)
                    .build());
        }

        cartItemRepository.deleteByUserId(userId);

        if (kafkaTemplate.isPresent()) {
            try {
                String event = objectMapper.writeValueAsString(
                        new OrderCreatedEvent(order.getId(), userId, finalPrice));
                kafkaTemplate.get().send("order.created", event);
            } catch (JsonProcessingException ignored) {}
        } else {
            notificationService.notifyOrderStatus(user,
                    String.format("주문이 완료되었습니다. 결제금액: %,d원", finalPrice));
        }

        return order.getId();
    }

    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(OrderSummaryResponse::new);
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        if (!order.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return new OrderDetailResponse(order, items);
    }

    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("주문을 찾을 수 없습니다."));
        if (!order.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        // 결제 전(PENDING)·결제 완료(PAID, 배송 전)까지만 취소 가능. 배송 시작 이후는 불가.
        if (order.getStatus() != Order.Status.PENDING && order.getStatus() != Order.Status.PAID) {
            throw BusinessException.badRequest("취소할 수 없는 주문입니다.");
        }
        boolean wasPaid = order.getStatus() == Order.Status.PAID;
        User user = order.getUser();

        order.updateStatus(Order.Status.CANCELLED);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            item.getProductOption().increaseStock(item.getQuantity());
        }

        if (order.getPointUsed() > 0) {
            pointService.earn(user, order.getPointUsed(), "주문 취소 환불");
        }

        // 사용했던 쿠폰을 미사용 상태로 복구한다 (취소 시 쿠폰 소멸 방지).
        if (order.getUserCoupon() != null) {
            order.getUserCoupon().restore();
        }

        // 결제 완료된 주문이면 결제를 취소하고, 결제 시 적립됐던 포인트를 회수한다.
        // (적립 포인트를 이미 사용했을 수 있으므로 보유 잔액 이내로만 회수해 취소가 막히지 않게 한다.)
        if (wasPaid) {
            paymentRepository.findByOrderId(orderId).ifPresent(Payment::cancel);
            int earned = order.getFinalPrice() * EARN_RATE_PERCENT / 100;
            int reclaim = Math.min(earned, user.getPoint());
            if (reclaim > 0) {
                pointService.use(user, reclaim, "주문 취소 - 적립 포인트 회수");
            }
        }

        if (kafkaTemplate.isPresent()) {
            try {
                String event = objectMapper.writeValueAsString(
                        Map.of("userId", userId, "orderId", orderId, "status", "CANCELLED"));
                kafkaTemplate.get().send("order.status.changed", event);
            } catch (JsonProcessingException ignored) {}
        } else {
            notificationService.notifyOrderStatus(order.getUser(), "주문이 취소되었습니다.");
        }
    }

    @Getter
    public static class CreateOrderRequest {
        private Long addressId;
        private List<OrderItemDto> items;
        private Long userCouponId;
        private int pointUsed;
    }

    @Getter
    public static class OrderItemDto {
        private Long productOptionId;
        private int quantity;
    }

    @Getter
    public static class OrderSummaryResponse {
        private final Long orderId;
        private final String status;
        private final int finalPrice;
        private final String createdAt;

        public OrderSummaryResponse(Order order) {
            this.orderId = order.getId();
            this.status = order.getStatus().name();
            this.finalPrice = order.getFinalPrice();
            this.createdAt = order.getCreatedAt().toString();
        }
    }

    @Getter
    public static class OrderDetailResponse {
        private final Long orderId;
        private final String status;
        private final int totalPrice;
        private final int discountPrice;
        private final int pointUsed;
        private final int shippingFee;
        private final int finalPrice;
        private final List<OrderItemResponse> items;

        public OrderDetailResponse(Order order, List<OrderItem> items) {
            this.orderId = order.getId();
            this.status = order.getStatus().name();
            this.totalPrice = order.getTotalPrice();
            this.discountPrice = order.getDiscountPrice();
            this.pointUsed = order.getPointUsed();
            this.shippingFee = order.getShippingFee();
            this.finalPrice = order.getFinalPrice();
            this.items = items.stream().map(OrderItemResponse::new).toList();
        }
    }

    @Getter
    public static class OrderItemResponse {
        private final Long orderItemId;
        private final Long productId;
        private final String productName;
        private final String size;
        private final String color;
        private final int quantity;
        private final int price;

        public OrderItemResponse(OrderItem item) {
            this.orderItemId = item.getId();
            this.productId = item.getProductOption().getProduct().getId();
            this.productName = item.getProductOption().getProduct().getName();
            this.size = item.getProductOption().getSize();
            this.color = item.getProductOption().getColor();
            this.quantity = item.getQuantity();
            this.price = item.getPrice();
        }
    }
}
