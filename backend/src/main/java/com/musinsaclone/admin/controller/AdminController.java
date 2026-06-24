package com.musinsaclone.admin.controller;

import com.musinsaclone.admin.service.AdminProductService;
import com.musinsaclone.admin.service.AdminService;
import com.musinsaclone.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final AdminProductService adminProductService;

    @GetMapping("/orders")
    public ApiResponse<Page<AdminService.AdminOrderResponse>> getOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminService.getAllOrders(pageable));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ApiResponse<Void> updateStatus(@PathVariable Long orderId,
                                          @RequestBody StatusRequest request) {
        adminService.updateOrderStatus(orderId, request.status());
        return ApiResponse.ok();
    }

    record StatusRequest(String status) {}

    // ===== 상품 관리 =====

    @GetMapping("/products")
    public ApiResponse<Page<AdminProductService.AdminProductResponse>> getProducts(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(adminProductService.getProducts(pageable));
    }

    @GetMapping("/products/meta")
    public ApiResponse<AdminProductService.MetaResponse> getProductMeta() {
        return ApiResponse.ok(adminProductService.getMeta());
    }

    @PostMapping("/products")
    public ApiResponse<Long> createProduct(@RequestBody AdminProductService.ProductRequest request) {
        return ApiResponse.ok(adminProductService.createProduct(request));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<Void> updateProduct(@PathVariable Long productId,
                                           @RequestBody AdminProductService.ProductRequest request) {
        adminProductService.updateProduct(productId, request);
        return ApiResponse.ok();
    }

    @PatchMapping("/products/{productId}/status")
    public ApiResponse<Void> updateProductStatus(@PathVariable Long productId,
                                                 @RequestBody StatusRequest request) {
        adminProductService.updateStatus(productId, request.status());
        return ApiResponse.ok();
    }

    // ===== 상품 옵션 관리 =====

    @GetMapping("/products/{productId}/options")
    public ApiResponse<java.util.List<AdminProductService.OptionResponse>> getOptions(
            @PathVariable Long productId) {
        return ApiResponse.ok(adminProductService.getOptions(productId));
    }

    @PostMapping("/products/{productId}/options")
    public ApiResponse<Long> addOption(@PathVariable Long productId,
                                       @RequestBody AdminProductService.OptionRequest request) {
        return ApiResponse.ok(adminProductService.addOption(productId, request));
    }

    @PutMapping("/options/{optionId}")
    public ApiResponse<Void> updateOption(@PathVariable Long optionId,
                                          @RequestBody AdminProductService.OptionRequest request) {
        adminProductService.updateOption(optionId, request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/options/{optionId}")
    public ApiResponse<Void> deleteOption(@PathVariable Long optionId) {
        adminProductService.deleteOption(optionId);
        return ApiResponse.ok();
    }
}
