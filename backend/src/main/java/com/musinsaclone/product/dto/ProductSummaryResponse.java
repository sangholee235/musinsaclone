package com.musinsaclone.product.dto;

import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductImage;
import lombok.Getter;

@Getter
public class ProductSummaryResponse {
    private final Long id;
    private final String brandName;
    private final String name;
    private final int price;
    private final int discountRate;
    private final int discountedPrice;
    private final String mainImageUrl;

    public ProductSummaryResponse(Product product, ProductImage mainImage) {
        this.id = product.getId();
        this.brandName = product.getBrand().getName();
        this.name = product.getName();
        this.price = product.getPrice();
        this.discountRate = product.getDiscountRate();
        this.discountedPrice = product.getDiscountedPrice();
        this.mainImageUrl = mainImage != null ? mainImage.getUrl() : null;
    }
}
