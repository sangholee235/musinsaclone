package com.musinsaclone.review;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.order.entity.Order;
import com.musinsaclone.order.entity.OrderItem;
import com.musinsaclone.order.repository.OrderItemRepository;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductOption;
import com.musinsaclone.review.entity.Review;
import com.musinsaclone.review.repository.ReviewRepository;
import com.musinsaclone.review.service.ReviewService;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private UserRepository userRepository;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository, orderItemRepository, userRepository);
    }

    private OrderItem orderItem(Long ownerId, Order.Status status) {
        User owner = User.builder().id(ownerId).email("t@t.com").password("p").name("t")
                .role(User.Role.USER).point(0).build();
        Order order = Order.builder().id(1L).user(owner).totalPrice(0).discountPrice(0)
                .pointUsed(0).finalPrice(0).status(status).build();
        Product product = Product.builder().id(1L).name("상품").price(10000).discountRate(0)
                .status(Product.Status.ON_SALE).build();
        ProductOption option = ProductOption.builder().id(1L).product(product)
                .size("M").color("블랙").stock(10).extraPrice(0).build();
        OrderItem item = mock(OrderItem.class);
        lenient().when(item.getOrder()).thenReturn(order);
        lenient().when(item.getProductOption()).thenReturn(option);
        return item;
    }

    private ReviewService.CreateReviewRequest request() {
        ReviewService.CreateReviewRequest req = mock(ReviewService.CreateReviewRequest.class);
        lenient().when(req.getOrderItemId()).thenReturn(1L);
        lenient().when(req.getRating()).thenReturn(5);
        lenient().when(req.getContent()).thenReturn("좋은 상품입니다");
        return req;
    }

    @Test
    @DisplayName("평점 분포는 1~5점 전체 키를 채우고 없는 점수는 0으로 둔다")
    void getRatingDetail_buildsFullDistribution() {
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(4.5);
        when(reviewRepository.countGroupByRating(1L)).thenReturn(java.util.List.of(
                new Object[]{5, 3L}, new Object[]{4, 1L}));

        ReviewService.RatingResponse r = reviewService.getRatingDetail(1L);

        org.assertj.core.api.Assertions.assertThat(r.getTotalCount()).isEqualTo(4);
        org.assertj.core.api.Assertions.assertThat(r.getAverageRating()).isEqualTo(4.5);
        org.assertj.core.api.Assertions.assertThat(r.getDistribution().get(5)).isEqualTo(3L);
        org.assertj.core.api.Assertions.assertThat(r.getDistribution().get(4)).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(r.getDistribution().get(3)).isEqualTo(0L);
        org.assertj.core.api.Assertions.assertThat(r.getDistribution()).containsKeys(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("결제 완료된 주문 항목에는 리뷰를 작성할 수 있다")
    void createReview_success() {
        OrderItem item = orderItem(1L, Order.Status.PAID);
        when(reviewRepository.existsByOrderItemId(1L)).thenReturn(false);
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.getReferenceById(1L)).thenReturn(
                User.builder().id(1L).email("t@t.com").password("p").name("t")
                        .role(User.Role.USER).point(0).build());

        reviewService.createReview(1L, request());

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("결제 전(PENDING) 주문 항목에는 리뷰를 작성할 수 없다")
    void createReview_notPaid() {
        OrderItem item = orderItem(1L, Order.Status.PENDING);
        when(reviewRepository.existsByOrderItemId(1L)).thenReturn(false);
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> reviewService.createReview(1L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("구매 확정");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("취소된 주문 항목에는 리뷰를 작성할 수 없다")
    void createReview_cancelled() {
        OrderItem item = orderItem(1L, Order.Status.CANCELLED);
        when(reviewRepository.existsByOrderItemId(1L)).thenReturn(false);
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> reviewService.createReview(1L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("구매 확정");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 리뷰가 있으면 중복 작성할 수 없다")
    void createReview_duplicate() {
        when(reviewRepository.existsByOrderItemId(1L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 리뷰");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 사용자의 주문 항목에는 리뷰를 작성할 수 없다")
    void createReview_othersOrder() {
        OrderItem item = orderItem(2L, Order.Status.PAID);
        when(reviewRepository.existsByOrderItemId(1L)).thenReturn(false);
        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> reviewService.createReview(1L, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("권한");
        verify(reviewRepository, never()).save(any());
    }
}
