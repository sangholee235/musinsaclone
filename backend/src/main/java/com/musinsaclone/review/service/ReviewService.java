package com.musinsaclone.review.service;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.entity.OrderItem;
import com.musinsaclone.order.repository.OrderItemRepository;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.review.entity.Review;
import com.musinsaclone.review.entity.ReviewImage;
import com.musinsaclone.review.repository.ReviewRepository;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReview(Long userId, CreateReviewRequest request) {
        if (reviewRepository.existsByOrderItemId(request.getOrderItemId())) {
            throw BusinessException.badRequest("이미 리뷰를 작성했습니다.");
        }

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> BusinessException.notFound("주문 항목을 찾을 수 없습니다."));
        if (!orderItem.getOrder().getUser().getId().equals(userId)) {
            throw BusinessException.forbidden("권한이 없습니다.");
        }
        // 실제 결제(구매)가 완료된 주문에 대해서만 리뷰를 허용한다. (결제 전·취소 주문 차단)
        Order.Status status = orderItem.getOrder().getStatus();
        if (status != Order.Status.PAID && status != Order.Status.SHIPPING
                && status != Order.Status.DELIVERED) {
            throw BusinessException.badRequest("구매 확정된 상품만 리뷰를 작성할 수 있습니다.");
        }

        User user = userRepository.getReferenceById(userId);
        // 상품은 주문 항목에서 직접 유도하여 클라이언트 입력과의 불일치를 방지한다.
        Product product = orderItem.getProductOption().getProduct();

        Review review = Review.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.getRating())
                .content(request.getContent())
                .build();

        reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(ReviewResponse::new);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Long productId) {
        Double avg = reviewRepository.findAverageRatingByProductId(productId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Transactional(readOnly = true)
    public RatingResponse getRatingDetail(Long productId) {
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) distribution.put(star, 0L);
        long total = 0;
        for (Object[] row : reviewRepository.countGroupByRating(productId)) {
            int rating = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.put(rating, count);
            total += count;
        }
        return new RatingResponse(getAverageRating(productId), total, distribution);
    }

    @Getter
    public static class RatingResponse {
        private final double averageRating;
        private final long totalCount;
        private final Map<Integer, Long> distribution;

        public RatingResponse(double averageRating, long totalCount, Map<Integer, Long> distribution) {
            this.averageRating = averageRating;
            this.totalCount = totalCount;
            this.distribution = distribution;
        }
    }

    @Getter
    public static class CreateReviewRequest {
        private Long productId;
        private Long orderItemId;
        private int rating;
        private String content;
        private List<String> imageUrls;
    }

    @Getter
    public static class ReviewResponse {
        private final Long reviewId;
        private final String userName;
        private final int rating;
        private final String content;
        private final String createdAt;

        public ReviewResponse(Review review) {
            this.reviewId = review.getId();
            this.userName = review.getUser().getName();
            this.rating = review.getRating();
            this.content = review.getContent();
            this.createdAt = review.getCreatedAt().toString();
        }
    }
}
