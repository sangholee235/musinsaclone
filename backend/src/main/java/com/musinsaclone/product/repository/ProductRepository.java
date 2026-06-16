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
}
