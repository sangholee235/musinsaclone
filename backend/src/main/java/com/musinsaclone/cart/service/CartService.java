package com.musinsaclone.cart.service;

import com.musinsaclone.cart.entity.CartItem;
import com.musinsaclone.cart.repository.CartItemRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.product.entity.ProductOption;
import com.musinsaclone.product.repository.ProductOptionRepository;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addItem(Long userId, Long productOptionId, int quantity) {
        cartItemRepository.findByUserIdAndProductOptionId(userId, productOptionId)
                .ifPresentOrElse(
                        item -> item.updateQuantity(item.getQuantity() + quantity),
                        () -> {
                            User user = userRepository.getReferenceById(userId);
                            ProductOption option = productOptionRepository.findById(productOptionId)
                                    .orElseThrow(() -> BusinessException.notFound("옵션을 찾을 수 없습니다."));
                            if (option.getProduct().getStatus() != com.musinsaclone.product.entity.Product.Status.ON_SALE) {
                                throw BusinessException.badRequest("현재 판매 중이 아닌 상품입니다.");
                            }
                            cartItemRepository.save(CartItem.builder()
                                    .user(user)
                                    .productOption(option)
                                    .quantity(quantity)
                                    .build());
                        }
                );
    }

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(Long userId) {
        return cartItemRepository.findByUserIdWithProduct(userId).stream()
                .map(CartItemResponse::new)
                .toList();
    }

    @Transactional
    public void updateQuantity(Long userId, Long cartItemId, int quantity) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> BusinessException.notFound("장바구니 항목을 찾을 수 없습니다."));
        if (!item.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.updateQuantity(quantity);
        }
    }

    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> BusinessException.notFound("장바구니 항목을 찾을 수 없습니다."));
        if (!item.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        cartItemRepository.delete(item);
    }

    @Getter
    public static class CartItemResponse {
        private final Long cartItemId;
        private final Long productId;
        private final String productName;
        private final String brandName;
        private final Long optionId;
        private final String size;
        private final String color;
        private final int price;
        private final int quantity;
        private final int totalPrice;
        private final int stock;
        private final String status;
        private final boolean available;

        public CartItemResponse(CartItem item) {
            this.cartItemId = item.getId();
            var option = item.getProductOption();
            var product = option.getProduct();
            this.productId = product.getId();
            this.productName = product.getName();
            this.brandName = product.getBrand().getName();
            this.optionId = option.getId();
            this.size = option.getSize();
            this.color = option.getColor();
            this.price = product.getDiscountedPrice() + option.getExtraPrice();
            this.quantity = item.getQuantity();
            this.totalPrice = this.price * this.quantity;
            this.stock = option.getStock();
            this.status = product.getStatus().name();
            // 판매중이고 재고가 담은 수량 이상일 때만 주문 가능
            this.available = product.getStatus() == com.musinsaclone.product.entity.Product.Status.ON_SALE
                    && option.getStock() >= item.getQuantity();
        }
    }
}
