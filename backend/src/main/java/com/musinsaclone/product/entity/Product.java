package com.musinsaclone.product.entity;

import com.musinsaclone.brand.entity.Brand;
import com.musinsaclone.category.entity.Category;
import com.musinsaclone.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int discountRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public enum Status {
        ON_SALE, SOLD_OUT, HIDDEN
    }

    public int getDiscountedPrice() {
        return price - (price * discountRate / 100);
    }

    public void updateInfo(Brand brand, Category category, String name, String description,
                           int price, int discountRate) {
        this.brand = brand;
        this.category = category;
        this.name = name;
        this.description = description;
        this.price = price;
        this.discountRate = discountRate;
    }

    public void updateStatus(Status status) {
        this.status = status;
    }
}
