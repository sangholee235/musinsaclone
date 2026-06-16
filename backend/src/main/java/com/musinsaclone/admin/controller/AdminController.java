package com.musinsaclone.admin.controller;

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
}
