package com.musinsaclone.user.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.coupon.entity.UserCoupon;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.point.entity.PointHistory;
import com.musinsaclone.point.repository.PointHistoryRepository;
import com.musinsaclone.user.service.UserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserCouponRepository userCouponRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @GetMapping("/me")
    public ApiResponse<UserService.UserProfileResponse> getProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<Void> updateProfile(@AuthenticationPrincipal Long userId,
                                           @RequestBody UserService.UpdateProfileRequest request) {
        userService.updateProfile(userId, request);
        return ApiResponse.ok();
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal Long userId,
                                            @RequestBody UserService.ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ApiResponse.ok();
    }

    @GetMapping("/me/addresses")
    public ApiResponse<List<UserService.AddressResponse>> getAddresses(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.getAddresses(userId));
    }

    @PostMapping("/me/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addAddress(@AuthenticationPrincipal Long userId,
                                        @RequestBody UserService.AddressRequest request) {
        userService.addAddress(userId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ApiResponse<Void> deleteAddress(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long addressId) {
        userService.deleteAddress(userId, addressId);
        return ApiResponse.ok();
    }

    @PutMapping("/me/addresses/{addressId}/default")
    public ApiResponse<Void> setDefaultAddress(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long addressId) {
        userService.setDefaultAddress(userId, addressId);
        return ApiResponse.ok();
    }

    @GetMapping("/me/coupons")
    public ApiResponse<List<CouponSummary>> getCoupons(@AuthenticationPrincipal Long userId) {
        List<CouponSummary> coupons = userCouponRepository.findAvailableByUserId(userId).stream()
                .map(CouponSummary::new).toList();
        return ApiResponse.ok(coupons);
    }

    @GetMapping("/me/points")
    public ApiResponse<Page<PointSummary>> getPoints(@AuthenticationPrincipal Long userId,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        Page<PointSummary> points = pointHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PointSummary::new);
        return ApiResponse.ok(points);
    }

    @Getter
    static class CouponSummary {
        private final Long userCouponId;
        private final String name;
        private final String discountType;
        private final int discountValue;
        private final String expiredAt;

        CouponSummary(UserCoupon uc) {
            this.userCouponId = uc.getId();
            this.name = uc.getCoupon().getName();
            this.discountType = uc.getCoupon().getDiscountType().name();
            this.discountValue = uc.getCoupon().getDiscountValue();
            this.expiredAt = uc.getCoupon().getExpiredAt().toString();
        }
    }

    @Getter
    static class PointSummary {
        private final int amount;
        private final String reason;
        private final String createdAt;

        PointSummary(PointHistory ph) {
            this.amount = ph.getAmount();
            this.reason = ph.getReason();
            this.createdAt = ph.getCreatedAt().toString();
        }
    }
}
