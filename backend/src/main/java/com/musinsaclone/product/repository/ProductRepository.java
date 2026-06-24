package com.musinsaclone.product.repository;

import com.musinsaclone.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ON_SALE' AND " +
           "(p.name LIKE %:keyword% OR p.brand.name LIKE %:keyword%)")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.status = 'ON_SALE' AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:brandId IS NULL OR p.brand.id = :brandId) AND " +
           "(:saleOnly = false OR p.discountRate > 0) AND " +
           "p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findWithFilter(
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("saleOnly") boolean saleOnly,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice,
            Pageable pageable);

    // 판매량(취소 제외 주문 수량 합) 기준 인기순. 동일 필터 + 페이지네이션 결합.
    @Query(value = "SELECT p.* FROM products p " +
            "LEFT JOIN (" +
            "  SELECT po.product_id AS pid, SUM(oi.quantity) AS sold " +
            "  FROM order_items oi " +
            "  JOIN product_options po ON oi.product_option_id = po.id " +
            "  JOIN orders o ON oi.order_id = o.id AND o.status <> 'CANCELLED' " +
            "  GROUP BY po.product_id" +
            ") s ON s.pid = p.id " +
            "WHERE p.status = 'ON_SALE' " +
            "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
            "AND (:brandId IS NULL OR p.brand_id = :brandId) " +
            "AND (:saleOnly = FALSE OR p.discount_rate > 0) " +
            "AND p.price BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY COALESCE(s.sold, 0) DESC, p.id DESC",
            countQuery = "SELECT COUNT(*) FROM products p " +
            "WHERE p.status = 'ON_SALE' " +
            "AND (:categoryId IS NULL OR p.category_id = :categoryId) " +
            "AND (:brandId IS NULL OR p.brand_id = :brandId) " +
            "AND (:saleOnly = FALSE OR p.discount_rate > 0) " +
            "AND p.price BETWEEN :minPrice AND :maxPrice",
            nativeQuery = true)
    Page<Product> findBestSellers(
            @Param("categoryId") Long categoryId,
            @Param("brandId") Long brandId,
            @Param("saleOnly") boolean saleOnly,
            @Param("minPrice") int minPrice,
            @Param("maxPrice") int maxPrice,
            Pageable pageable);
}
