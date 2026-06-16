package com.musinsaclone.cart.repository;

import com.musinsaclone.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("SELECT c FROM CartItem c JOIN FETCH c.productOption po JOIN FETCH po.product WHERE c.user.id = :userId")
    List<CartItem> findByUserIdWithProduct(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndProductOptionId(Long userId, Long productOptionId);

    void deleteByUserId(Long userId);
}
