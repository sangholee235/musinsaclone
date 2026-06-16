package com.musinsaclone.payment.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.payment.service.PaymentService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/prepare")
    public ApiResponse<Void> prepare(@AuthenticationPrincipal Long userId,
                                     @RequestBody PrepareRequest request) {
        paymentService.preparePayment(userId, request.getOrderId(), request.getMethod());
        return ApiResponse.ok();
    }

    @PostMapping("/confirm")
    public ApiResponse<Void> confirm(@AuthenticationPrincipal Long userId,
                                     @RequestBody ConfirmRequest request) {
        paymentService.confirmPayment(userId, request.getOrderId(), request.getPgTxId());
        return ApiResponse.ok();
    }

    @GetMapping("/orders/{orderId}")
    public ApiResponse<PaymentService.PaymentResponse> getPayment(@AuthenticationPrincipal Long userId,
                                                                   @PathVariable Long orderId) {
        return ApiResponse.ok(paymentService.getPayment(userId, orderId));
    }

    @Getter
    static class PrepareRequest {
        private Long orderId;
        private String method;
    }

    @Getter
    static class ConfirmRequest {
        private Long orderId;
        private String pgTxId;
    }
}
