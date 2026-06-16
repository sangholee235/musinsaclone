package com.musinsaclone.order.entity;

import com.musinsaclone.common.entity.BaseEntity;
import com.musinsaclone.coupon.entity.UserCoupon;
import com.musinsaclone.user.entity.Address;
import com.musinsaclone.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_coupon_id")
    private UserCoupon userCoupon;

    @Column(nullable = false)
    private int totalPrice;

    @Column(nullable = false)
    private int discountPrice;

    @Column(nullable = false)
    private int pointUsed;

    @Column(nullable = false)
    private int finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        PENDING, PAID, SHIPPING, DELIVERED, CANCELLED
    }

    public void updateStatus(Status status) {
        this.status = status;
    }
}
