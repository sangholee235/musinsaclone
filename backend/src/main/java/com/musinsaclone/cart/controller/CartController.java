package com.musinsaclone.cart.controller;

import com.musinsaclone.cart.service.CartService;
import com.musinsaclone.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<List<CartService.CartItemResponse>> getCart(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(cartService.getCart(userId));
    }

    @PostMapping
    public ApiResponse<Void> addItem(@AuthenticationPrincipal Long userId,
                                     @Valid @RequestBody AddItemRequest request) {
        cartService.addItem(userId, request.getProductOptionId(), request.getQuantity());
        return ApiResponse.ok();
    }

    @PatchMapping("/{cartItemId}")
    public ApiResponse<Void> updateQuantity(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long cartItemId,
                                            @RequestParam int quantity) {
        cartService.updateQuantity(userId, cartItemId, quantity);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{cartItemId}")
    public ApiResponse<Void> removeItem(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long cartItemId) {
        cartService.removeItem(userId, cartItemId);
        return ApiResponse.ok();
    }

    @Getter
    static class AddItemRequest {
        @NotNull
        private Long productOptionId;
        @Min(1)
        private int quantity;
    }
}
