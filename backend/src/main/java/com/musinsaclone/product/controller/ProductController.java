package com.musinsaclone.product.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.product.dto.ProductResponse;
import com.musinsaclone.product.dto.ProductSummaryResponse;
import com.musinsaclone.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> getProduct(@PathVariable Long productId) {
        return ApiResponse.ok(productService.getProduct(productId));
    }

    @GetMapping
    public ApiResponse<Page<ProductSummaryResponse>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "0") int minPrice,
            @RequestParam(defaultValue = "10000000") int maxPrice,
            @RequestParam(required = false) Boolean sale,
            @RequestParam(required = false) String sort,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(productService.getProducts(categoryId, brandId, minPrice, maxPrice,
                Boolean.TRUE.equals(sale), sort, pageable));
    }

    @GetMapping("/search")
    public ApiResponse<Page<ProductSummaryResponse>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(productService.search(keyword, pageable));
    }
}
