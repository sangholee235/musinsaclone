package com.musinsaclone.coupon;

import com.musinsaclone.coupon.entity.Coupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CouponDomainTest {

    private Coupon coupon(Coupon.DiscountType type, int value, int minOrder,
                          LocalDateTime start, LocalDateTime end) {
        return Coupon.builder()
                .name("테스트 쿠폰")
                .discountType(type)
                .discountValue(value)
                .minOrderPrice(minOrder)
                .startedAt(start)
                .expiredAt(end)
                .build();
    }

    @Test
    @DisplayName("정액 쿠폰은 고정 금액을 할인한다")
    void fixedDiscount() {
        Coupon c = coupon(Coupon.DiscountType.FIXED, 5000, 30000,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertThat(c.calculateDiscount(50000)).isEqualTo(5000);
    }

    @Test
    @DisplayName("정률 쿠폰은 비율만큼 할인한다")
    void rateDiscount() {
        Coupon c = coupon(Coupon.DiscountType.RATE, 10, 10000,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertThat(c.calculateDiscount(50000)).isEqualTo(5000);
    }

    @Test
    @DisplayName("최소 주문금액 미만이면 할인 0")
    void belowMinOrder() {
        Coupon c = coupon(Coupon.DiscountType.FIXED, 5000, 30000,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        assertThat(c.calculateDiscount(20000)).isZero();
    }

    @Test
    @DisplayName("기간 내 쿠폰은 유효, 만료된 쿠폰은 무효")
    void validity() {
        Coupon valid = coupon(Coupon.DiscountType.FIXED, 5000, 0,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1));
        Coupon expired = coupon(Coupon.DiscountType.FIXED, 5000, 0,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        assertThat(valid.isValid()).isTrue();
        assertThat(expired.isValid()).isFalse();
    }
}
