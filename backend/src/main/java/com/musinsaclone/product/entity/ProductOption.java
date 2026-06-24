package com.musinsaclone.product.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private String size;

    private String color;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int extraPrice;

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) throw new IllegalStateException("재고 부족");
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        this.stock += quantity;
    }

    public void update(String size, String color, int stock, int extraPrice) {
        this.size = size;
        this.color = color;
        this.stock = stock;
        this.extraPrice = extraPrice;
    }
}
