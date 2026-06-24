package com.musinsaclone.coupon;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.entity.Coupon;
import com.musinsaclone.coupon.entity.UserCoupon;
import com.musinsaclone.coupon.repository.CouponRepository;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.coupon.service.CouponService;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock private CouponRepository couponRepository;
    @Mock private UserCouponRepository userCouponRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private CouponService couponService;

    private Coupon coupon(Long id, boolean valid) {
        LocalDateTime now = LocalDateTime.now();
        return Coupon.builder()
                .id(id).name("쿠폰" + id)
                .discountType(Coupon.DiscountType.FIXED).discountValue(5000).minOrderPrice(10000)
                .startedAt(now.minusDays(1))
                .expiredAt(valid ? now.plusDays(1) : now.minusDays(1))
                .build();
    }

    @Test
    @DisplayName("발급 가능 목록은 유효하고 미보유한 쿠폰만 노출한다")
    void claimable_excludesOwnedAndExpired() {
        Coupon owned = coupon(1L, true);
        Coupon available = coupon(2L, true);
        Coupon expired = coupon(3L, false);
        when(userCouponRepository.findAllByUserId(1L))
                .thenReturn(List.of(UserCoupon.builder().coupon(owned).build()));
        when(couponRepository.findAll()).thenReturn(List.of(owned, available, expired));

        List<CouponService.CouponResponse> result = couponService.getClaimableCoupons(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCouponId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("쿠폰 발급에 성공하면 UserCoupon 이 저장된다")
    void claim_success() {
        when(couponRepository.findById(2L)).thenReturn(Optional.of(coupon(2L, true)));
        when(userCouponRepository.existsByUserIdAndCouponId(1L, 2L)).thenReturn(false);
        when(userRepository.getReferenceById(1L)).thenReturn(User.builder().id(1L).build());

        couponService.claim(1L, 2L);

        verify(userCouponRepository).save(any(UserCoupon.class));
    }

    @Test
    @DisplayName("이미 발급받은 쿠폰은 다시 발급할 수 없다")
    void claim_duplicate() {
        when(couponRepository.findById(2L)).thenReturn(Optional.of(coupon(2L, true)));
        when(userCouponRepository.existsByUserIdAndCouponId(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> couponService.claim(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 발급");
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("발급 한도가 소진된 쿠폰은 발급할 수 없다")
    void claim_soldOut() {
        LocalDateTime now = LocalDateTime.now();
        Coupon limited = Coupon.builder().id(4L).name("선착순")
                .discountType(Coupon.DiscountType.FIXED).discountValue(3000).minOrderPrice(10000)
                .startedAt(now.minusDays(1)).expiredAt(now.plusDays(1))
                .totalQuantity(1).build();
        when(couponRepository.findById(4L)).thenReturn(Optional.of(limited));
        when(userCouponRepository.existsByUserIdAndCouponId(1L, 4L)).thenReturn(false);
        when(userCouponRepository.countByCouponId(4L)).thenReturn(1L); // 한도 1, 이미 1개 발급

        assertThatThrownBy(() -> couponService.claim(1L, 4L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("소진");
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("만료된 쿠폰은 발급할 수 없다")
    void claim_expired() {
        when(couponRepository.findById(3L)).thenReturn(Optional.of(coupon(3L, false)));

        assertThatThrownBy(() -> couponService.claim(1L, 3L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("발급 기간");
        verify(userCouponRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰 발급 시 예외")
    void claim_notFound() {
        when(couponRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.claim(1L, 99L))
                .isInstanceOf(BusinessException.class);
    }
}
