package com.musinsaclone.order.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> createOrder(@AuthenticationPrincipal Long userId,
                                                       @RequestBody OrderService.CreateOrderRequest request) {
        Long orderId = orderService.createOrder(userId, request);
        return ApiResponse.ok(Map.of("orderId", orderId));
    }

    @GetMapping
    public ApiResponse<Page<OrderService.OrderSummaryResponse>> getOrders(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(orderService.getOrders(userId, pageable));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderService.OrderDetailResponse> getOrder(@AuthenticationPrincipal Long userId,
                                                                   @PathVariable Long orderId) {
        return ApiResponse.ok(orderService.getOrder(userId, orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long orderId) {
        orderService.cancelOrder(userId, orderId);
        return ApiResponse.ok();
    }
}
