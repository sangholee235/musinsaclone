package com.musinsaclone.review.service;

import com.musinsaclone.common.exception.BusinessException;
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

import java.util.List;

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
