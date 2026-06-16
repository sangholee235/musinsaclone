package com.musinsaclone.brand.controller;

import com.musinsaclone.brand.service.BrandService;
import com.musinsaclone.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    public ApiResponse<Page<BrandService.BrandResponse>> getBrands(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(brandService.getBrands(pageable));
    }

    @GetMapping("/{brandId}")
    public ApiResponse<BrandService.BrandResponse> getBrand(@PathVariable Long brandId) {
        return ApiResponse.ok(brandService.getBrand(brandId));
    }

    @PostMapping("/{brandId}/follow")
    public ApiResponse<Map<String, Boolean>> toggleFollow(@AuthenticationPrincipal Long userId,
                                                          @PathVariable Long brandId) {
        boolean followed = brandService.toggleFollow(userId, brandId);
        return ApiResponse.ok(Map.of("followed", followed));
    }
}
