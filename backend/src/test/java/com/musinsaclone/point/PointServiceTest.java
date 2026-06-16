package com.musinsaclone.point;

import com.musinsaclone.point.entity.PointHistory;
import com.musinsaclone.point.repository.PointHistoryRepository;
import com.musinsaclone.point.service.PointService;
import com.musinsaclone.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock private PointHistoryRepository pointHistoryRepository;
    @InjectMocks private PointService pointService;

    private User user(int point) {
        return User.builder().id(1L).email("t@t.com").password("p").name("t")
                .role(User.Role.USER).point(point).build();
    }

    @Test
    @DisplayName("포인트 사용 시 잔액이 차감되고 음수 이력이 기록된다")
    void use_recordsNegativeHistory() {
        User u = user(1000);

        pointService.use(u, 300, "주문 사용");

        assertThat(u.getPoint()).isEqualTo(700);
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(-300);
        assertThat(captor.getValue().getReason()).isEqualTo("주문 사용");
    }

    @Test
    @DisplayName("포인트 적립 시 잔액이 증가하고 양수 이력이 기록된다")
    void earn_recordsPositiveHistory() {
        User u = user(1000);

        pointService.earn(u, 500, "구매 적립");

        assertThat(u.getPoint()).isEqualTo(1500);
        ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
        verify(pointHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(500);
    }

    @Test
    @DisplayName("0 이하 금액은 적립/사용 모두 무시된다")
    void zeroOrNegative_noop() {
        User u = user(1000);

        pointService.earn(u, 0, "x");
        pointService.use(u, -10, "x");

        assertThat(u.getPoint()).isEqualTo(1000);
        verify(pointHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("잔액보다 많이 사용하면 예외가 발생한다")
    void use_insufficient() {
        User u = user(100);

        assertThatThrownBy(() -> pointService.use(u, 500, "주문 사용"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(pointHistoryRepository, never()).save(any());
    }
}
