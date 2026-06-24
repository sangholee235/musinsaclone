package com.musinsaclone.wishlist.service;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.repository.ProductRepository;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import com.musinsaclone.wishlist.entity.Wishlist;
import com.musinsaclone.wishlist.repository.WishlistRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean toggle(Long userId, Long productId) {
        return wishlistRepository.findByUserIdAndProductId(userId, productId)
                .map(w -> { wishlistRepository.delete(w); return false; })
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
                    wishlistRepository.save(Wishlist.builder().user(user).product(product).build());
                    return true;
                });
    }

    @Transactional(readOnly = true)
    public Page<WishlistResponse> getWishlist(Long userId, Pageable pageable) {
        return wishlistRepository.findByUserId(userId, pageable).map(WishlistResponse::new);
    }

    @Getter
    public static class WishlistResponse {
        private final Long wishlistId;
        private final Long productId;
        private final String productName;
        private final String brandName;
        private final int discountedPrice;
        private final String status;
        private final boolean available;

        public WishlistResponse(Wishlist wishlist) {
            this.wishlistId = wishlist.getId();
            var product = wishlist.getProduct();
            this.productId = product.getId();
            this.productName = product.getName();
            this.brandName = product.getBrand().getName();
            this.discountedPrice = product.getDiscountedPrice();
            this.status = product.getStatus().name();
            this.available = product.getStatus() == com.musinsaclone.product.entity.Product.Status.ON_SALE;
        }
    }
}
