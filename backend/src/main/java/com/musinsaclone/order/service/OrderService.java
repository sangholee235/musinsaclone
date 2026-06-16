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
    private final PointService pointService;
    private final NotificationService notificationService;
    private final Optional<KafkaTemplate<String, String>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> BusinessException.notFound("주소를 찾을 수 없습니다."));

        int totalPrice = 0;
        List<OrderItemDto> itemDtos = request.getItems();

        for (OrderItemDto dto : itemDtos) {
            ProductOption option = productOptionRepository.findById(dto.getProductOptionId())
                    .orElseThrow(() -> BusinessException.notFound("상품 옵션을 찾을 수 없습니다."));
            option.decreaseStock(dto.getQuantity());
            totalPrice += (option.getProduct().getDiscountedPrice() + option.getExtraPrice()) * dto.getQuantity();
        }

        int discountPrice = 0;
        UserCoupon userCoupon = null;
        if (request.getUserCouponId() != null) {
            userCoupon = userCouponRepository.findById(request.getUserCouponId())
                    .orElseThrow(() -> BusinessException.notFound("쿠폰을 찾을 수 없습니다."));
            if (!userCoupon.getCoupon().isValid()) throw BusinessException.badRequest("만료된 쿠폰입니다.");
            discountPrice = userCoupon.getCoupon().calculateDiscount(totalPrice);
            userCoupon.use();
        }

        int pointUsed = request.getPointUsed();
        if (pointUsed > user.getPoint()) throw BusinessException.badRequest("포인트가 부족합니다.");
        if (pointUsed > 0) pointService.use(user, pointUsed, "주문 결제 시 사용");

        int finalPrice = totalPrice - discountPrice - pointUsed;

        Order order = orderRepository.save(Order.builder()
                .user(user)
                .address(address)
                .userCoupon(userCoupon)
                .totalPrice(totalPrice)
                .discountPrice(discountPrice)
                .pointUsed(pointUsed)
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
        if (order.getStatus() != Order.Status.PENDING) throw BusinessException.badRequest("취소할 수 없는 주문입니다.");

        order.updateStatus(Order.Status.CANCELLED);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            item.getProductOption().increaseStock(item.getQuantity());
        }

        if (order.getPointUsed() > 0) {
            pointService.earn(order.getUser(), order.getPointUsed(), "주문 취소 환불");
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
        private final int finalPrice;
        private final List<OrderItemResponse> items;

        public OrderDetailResponse(Order order, List<OrderItem> items) {
            this.orderId = order.getId();
            this.status = order.getStatus().name();
            this.totalPrice = order.getTotalPrice();
            this.discountPrice = order.getDiscountPrice();
            this.pointUsed = order.getPointUsed();
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
