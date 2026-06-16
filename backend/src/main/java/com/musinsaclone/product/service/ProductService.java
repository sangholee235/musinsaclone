package com.musinsaclone.product.service;

import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.product.dto.ProductResponse;
import com.musinsaclone.product.dto.ProductSummaryResponse;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductImage;
import com.musinsaclone.product.repository.ProductImageRepository;
import com.musinsaclone.product.repository.ProductOptionRepository;
import com.musinsaclone.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository productOptionRepository;

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
        var images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        var options = productOptionRepository.findByProductId(productId);
        return new ProductResponse(product, images, options);
    }

    public Page<ProductSummaryResponse> getProducts(Long categoryId, Long brandId,
                                                     int minPrice, int maxPrice,
                                                     boolean saleOnly, String sort, Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), resolveSort(sort));
        return productRepository.findWithFilter(categoryId, brandId, saleOnly, minPrice, maxPrice, sorted)
                .map(this::toSummary);
    }

    private Sort resolveSort(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "id");
        return switch (sort) {
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "priceAsc" -> Sort.by(Sort.Direction.ASC, "price");
            case "priceDesc" -> Sort.by(Sort.Direction.DESC, "price");
            default -> Sort.by(Sort.Direction.DESC, "id"); // recommend / popular 등
        };
    }

    private ProductSummaryResponse toSummary(Product p) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(p.getId());
        ProductImage main = images.stream().filter(ProductImage::isMain).findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
        return new ProductSummaryResponse(p, main);
    }

    public Page<ProductSummaryResponse> search(String keyword, Pageable pageable) {
        return productRepository.searchByKeyword(keyword, pageable).map(this::toSummary);
    }
}
