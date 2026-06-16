package com.musinsaclone.point.service;

import com.musinsaclone.point.entity.PointHistory;
import com.musinsaclone.point.repository.PointHistoryRepository;
import com.musinsaclone.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointHistoryRepository pointHistoryRepository;

    /** 포인트 사용(차감) + 이력 기록. 잔액 검증은 User.usePoint 가 수행. */
    @Transactional
    public void use(User user, int amount, String reason) {
        if (amount <= 0) return;
        user.usePoint(amount);
        pointHistoryRepository.save(PointHistory.builder()
                .user(user).amount(-amount).reason(reason).build());
    }

    /** 포인트 적립 + 이력 기록. */
    @Transactional
    public void earn(User user, int amount, String reason) {
        if (amount <= 0) return;
        user.addPoint(amount);
        pointHistoryRepository.save(PointHistory.builder()
                .user(user).amount(amount).reason(reason).build());
    }

    @Transactional(readOnly = true)
    public Page<PointHistoryResponse> getHistory(Long userId, Pageable pageable) {
        return pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(PointHistoryResponse::new);
    }

    @Getter
    public static class PointHistoryResponse {
        private final Long id;
        private final int amount;
        private final String reason;
        private final String createdAt;

        public PointHistoryResponse(PointHistory h) {
            this.id = h.getId();
            this.amount = h.getAmount();
            this.reason = h.getReason();
            this.createdAt = h.getCreatedAt().toString();
        }
    }
}
