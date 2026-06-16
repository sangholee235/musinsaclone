package com.musinsaclone.point.repository;

import com.musinsaclone.point.entity.PointHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    Page<PointHistory> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
