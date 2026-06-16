package com.musinsaclone.brand.repository;

import com.musinsaclone.brand.entity.BrandFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandFollowRepository extends JpaRepository<BrandFollow, Long> {
    boolean existsByUserIdAndBrandId(Long userId, Long brandId);
    Optional<BrandFollow> findByUserIdAndBrandId(Long userId, Long brandId);
    long countByBrandId(Long brandId);
}
