package com.musinsaclone.point.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.point.service.PointService;
import com.musinsaclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<Page<PointService.PointHistoryResponse>> getHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(pointService.getHistory(userId, pageable));
    }

    @GetMapping("/balance")
    public ApiResponse<Map<String, Integer>> getBalance(@AuthenticationPrincipal Long userId) {
        int balance = userRepository.findById(userId).map(u -> u.getPoint()).orElse(0);
        return ApiResponse.ok(Map.of("balance", balance));
    }
}
