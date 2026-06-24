package com.musinsaclone.product.repository;

import com.musinsaclone.product.entity.ProductOption;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {
    List<ProductOption> findByProductId(Long productId);

    // 주문 재고 차감 시 동시성(오버셀) 방지를 위한 비관적 쓰기 락.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM ProductOption po WHERE po.id = :id")
    Optional<ProductOption> findByIdForUpdate(@Param("id") Long id);
}
