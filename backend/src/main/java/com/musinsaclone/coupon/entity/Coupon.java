package com.musinsaclone.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType discountType;

    @Column(nullable = false)
    private int discountValue;

    @Column(nullable = false)
    private int minOrderPrice;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    public enum DiscountType {
        FIXED, RATE
    }

    public boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(startedAt) && now.isBefore(expiredAt);
    }

    public int calculateDiscount(int orderPrice) {
        if (orderPrice < minOrderPrice) return 0;
        if (discountType == DiscountType.FIXED) return discountValue;
        return orderPrice * discountValue / 100;
    }
}
