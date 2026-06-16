package com.musinsaclone.coupon.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    public ApiResponse<List<CouponService.CouponResponse>> getClaimable(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(couponService.getClaimableCoupons(userId));
    }

    @PostMapping("/{couponId}/claim")
    public ApiResponse<Void> claim(@AuthenticationPrincipal Long userId,
                                   @PathVariable Long couponId) {
        couponService.claim(userId, couponId);
        return ApiResponse.ok();
    }

    @GetMapping("/my")
    public ApiResponse<List<CouponService.MyCouponResponse>> getMyCoupons(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(couponService.getMyCoupons(userId));
    }
}
