package com.musinsaclone.coupon.repository;

import com.musinsaclone.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId AND uc.isUsed = false")
    List<UserCoupon> findAvailableByUserId(@Param("userId") Long userId);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon WHERE uc.user.id = :userId ORDER BY uc.issuedAt DESC")
    List<UserCoupon> findAllByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndCouponId(Long userId, Long couponId);

    long countByCouponId(Long couponId);
}
