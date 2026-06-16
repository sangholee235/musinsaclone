package com.musinsaclone.coupon.service;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.entity.Coupon;
import com.musinsaclone.coupon.entity.UserCoupon;
import com.musinsaclone.coupon.repository.CouponRepository;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserRepository userRepository;

    /** 발급 가능한 쿠폰 목록(현재 유효 + 아직 미보유). */
    @Transactional(readOnly = true)
    public List<CouponResponse> getClaimableCoupons(Long userId) {
        Set<Long> ownedCouponIds = userCouponRepository.findAllByUserId(userId).stream()
                .map(uc -> uc.getCoupon().getId())
                .collect(Collectors.toSet());

        return couponRepository.findAll().stream()
                .filter(Coupon::isValid)
                .filter(c -> !ownedCouponIds.contains(c.getId()))
                .map(CouponResponse::new)
                .toList();
    }

    /** 쿠폰 발급(다운로드). */
    @Transactional
    public void claim(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> BusinessException.notFound("쿠폰을 찾을 수 없습니다."));
        if (!coupon.isValid()) throw BusinessException.badRequest("발급 기간이 아닌 쿠폰입니다.");
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw BusinessException.badRequest("이미 발급받은 쿠폰입니다.");
        }
        User user = userRepository.getReferenceById(userId);
        userCouponRepository.save(UserCoupon.builder()
                .user(user)
                .coupon(coupon)
                .isUsed(false)
                .build());
    }

    /** 내 쿠폰함(사용/미사용 전체). */
    @Transactional(readOnly = true)
    public List<MyCouponResponse> getMyCoupons(Long userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
                .map(MyCouponResponse::new)
                .toList();
    }

    @Getter
    public static class CouponResponse {
        private final Long couponId;
        private final String name;
        private final String discountType;
        private final int discountValue;
        private final int minOrderPrice;
        private final String expiredAt;

        public CouponResponse(Coupon coupon) {
            this.couponId = coupon.getId();
            this.name = coupon.getName();
            this.discountType = coupon.getDiscountType().name();
            this.discountValue = coupon.getDiscountValue();
            this.minOrderPrice = coupon.getMinOrderPrice();
            this.expiredAt = coupon.getExpiredAt().toString();
        }
    }

    @Getter
    public static class MyCouponResponse {
        private final Long userCouponId;
        private final Long couponId;
        private final String name;
        private final String discountType;
        private final int discountValue;
        private final int minOrderPrice;
        private final boolean used;
        private final boolean expired;
        private final String expiredAt;

        public MyCouponResponse(UserCoupon uc) {
            Coupon coupon = uc.getCoupon();
            this.userCouponId = uc.getId();
            this.couponId = coupon.getId();
            this.name = coupon.getName();
            this.discountType = coupon.getDiscountType().name();
            this.discountValue = coupon.getDiscountValue();
            this.minOrderPrice = coupon.getMinOrderPrice();
            this.used = uc.isUsed();
            this.expired = coupon.getExpiredAt().isBefore(LocalDateTime.now());
            this.expiredAt = coupon.getExpiredAt().toString();
        }
    }
}
