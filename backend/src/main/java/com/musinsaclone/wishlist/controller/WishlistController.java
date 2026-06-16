package com.musinsaclone.wishlist.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/{productId}")
    public ApiResponse<Map<String, Boolean>> toggle(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long productId) {
        boolean added = wishlistService.toggle(userId, productId);
        return ApiResponse.ok(Map.of("added", added));
    }

    @GetMapping
    public ApiResponse<Page<WishlistService.WishlistResponse>> getWishlist(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(wishlistService.getWishlist(userId, pageable));
    }
}
