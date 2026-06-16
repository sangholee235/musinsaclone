package com.musinsaclone.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductImage;
import com.musinsaclone.product.entity.ProductOption;
import lombok.Getter;

import java.util.List;

@Getter
public class ProductResponse {
    private final Long id;
    private final String brandName;
    private final String categoryName;
    private final String name;
    private final String description;
    private final int price;
    private final int discountRate;
    private final int discountedPrice;
    private final String status;
    private final List<ImageDto> images;
    private final List<OptionDto> options;

    public ProductResponse(Product product, List<ProductImage> images, List<ProductOption> options) {
        this.id = product.getId();
        this.brandName = product.getBrand().getName();
        this.categoryName = product.getCategory().getName();
        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.discountRate = product.getDiscountRate();
        this.discountedPrice = product.getDiscountedPrice();
        this.status = product.getStatus().name();
        this.images = images.stream().map(ImageDto::new).toList();
        this.options = options.stream().map(OptionDto::new).toList();
    }

    @Getter
    public static class ImageDto {
        private final Long id;
        private final String url;
        private final boolean isMain;

        public ImageDto(ProductImage image) {
            this.id = image.getId();
            this.url = image.getUrl();
            this.isMain = image.isMain();
        }

        @JsonProperty("isMain")
        public boolean isMain() {
            return isMain;
        }
    }

    @Getter
    public static class OptionDto {
        private final Long id;
        private final String size;
        private final String color;
        private final int stock;
        private final int extraPrice;

        public OptionDto(ProductOption option) {
            this.id = option.getId();
            this.size = option.getSize();
            this.color = option.getColor();
            this.stock = option.getStock();
            this.extraPrice = option.getExtraPrice();
        }
    }
}
