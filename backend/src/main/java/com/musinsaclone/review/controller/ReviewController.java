package com.musinsaclone.review.controller;

import com.musinsaclone.common.response.ApiResponse;
import com.musinsaclone.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/products/{productId}")
    public ApiResponse<Page<ReviewService.ReviewResponse>> getReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.ok(reviewService.getReviews(productId, pageable));
    }

    @GetMapping("/products/{productId}/rating")
    public ApiResponse<Map<String, Double>> getRating(@PathVariable Long productId) {
        return ApiResponse.ok(Map.of("averageRating", reviewService.getAverageRating(productId)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> createReview(@AuthenticationPrincipal Long userId,
                                          @RequestBody ReviewService.CreateReviewRequest request) {
        reviewService.createReview(userId, request);
        return ApiResponse.ok();
    }
}
