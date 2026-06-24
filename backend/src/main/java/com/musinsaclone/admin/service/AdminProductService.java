package com.musinsaclone.admin.service;

import com.musinsaclone.brand.entity.Brand;
import com.musinsaclone.brand.repository.BrandRepository;
import com.musinsaclone.category.entity.Category;
import com.musinsaclone.category.repository.CategoryRepository;
import com.musinsaclone.cart.repository.CartItemRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.order.repository.OrderItemRepository;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.entity.ProductImage;
import com.musinsaclone.product.entity.ProductOption;
import com.musinsaclone.product.repository.ProductImageRepository;
import com.musinsaclone.product.repository.ProductOptionRepository;
import com.musinsaclone.product.repository.ProductRepository;
import lombok.Getter;
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
public class AdminProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<AdminProductResponse> getProducts(Pageable pageable) {
        Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "id"));
        return productRepository.findAll(sorted).map(p -> new AdminProductResponse(p, mainImageUrl(p.getId())));
    }

    @Transactional(readOnly = true)
    public MetaResponse getMeta() {
        List<OptionItem> brands = brandRepository.findAll().stream()
                .map(b -> new OptionItem(b.getId(), b.getName())).toList();
        List<OptionItem> categories = categoryRepository.findAll().stream()
                .map(c -> new OptionItem(c.getId(), c.getName())).toList();
        return new MetaResponse(brands, categories);
    }

    @Transactional
    public Long createProduct(ProductRequest req) {
        Brand brand = findBrand(req.brandId());
        Category category = findCategory(req.categoryId());
        Product product = Product.builder()
                .brand(brand)
                .category(category)
                .name(req.name())
                .description(req.description())
                .price(req.price())
                .discountRate(req.discountRate())
                .status(parseStatus(req.status()))
                .build();
        productRepository.save(product);
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) {
            productImageRepository.save(ProductImage.builder()
                    .product(product).url(req.imageUrl()).isMain(true).sortOrder(0).build());
        }
        return product.getId();
    }

    @Transactional
    public void updateProduct(Long productId, ProductRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
        product.updateInfo(findBrand(req.brandId()), findCategory(req.categoryId()),
                req.name(), req.description(), req.price(), req.discountRate());
        product.updateStatus(parseStatus(req.status()));
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) {
            List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
            ProductImage main = images.stream().filter(ProductImage::isMain).findFirst()
                    .orElse(images.isEmpty() ? null : images.get(0));
            if (main != null) {
                main.changeUrl(req.imageUrl());
            } else {
                productImageRepository.save(ProductImage.builder()
                        .product(product).url(req.imageUrl()).isMain(true).sortOrder(0).build());
            }
        }
    }

    @Transactional
    public void updateStatus(Long productId, String status) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
        product.updateStatus(parseStatus(status));
    }

    // ===== 옵션 관리 =====

    @Transactional(readOnly = true)
    public List<OptionResponse> getOptions(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw BusinessException.notFound("상품을 찾을 수 없습니다.");
        }
        return productOptionRepository.findByProductId(productId).stream()
                .map(OptionResponse::new).toList();
    }

    @Transactional
    public Long addOption(Long productId, OptionRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> BusinessException.notFound("상품을 찾을 수 없습니다."));
        ProductOption option = productOptionRepository.save(ProductOption.builder()
                .product(product)
                .size(req.size())
                .color(req.color())
                .stock(req.stock())
                .extraPrice(req.extraPrice())
                .build());
        return option.getId();
    }

    @Transactional
    public void updateOption(Long optionId, OptionRequest req) {
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> BusinessException.notFound("옵션을 찾을 수 없습니다."));
        option.update(req.size(), req.color(), req.stock(), req.extraPrice());
    }

    @Transactional
    public void deleteOption(Long optionId) {
        ProductOption option = productOptionRepository.findById(optionId)
                .orElseThrow(() -> BusinessException.notFound("옵션을 찾을 수 없습니다."));
        if (orderItemRepository.existsByProductOptionId(optionId)) {
            throw BusinessException.badRequest("주문 내역이 있는 옵션은 삭제할 수 없습니다. 재고를 0으로 변경해주세요.");
        }
        // 장바구니에 담긴 옵션은 함께 정리한다 (cart_items FK 제약 회피).
        cartItemRepository.deleteByProductOptionId(optionId);
        productOptionRepository.delete(option);
    }

    private String mainImageUrl(Long productId) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
        ProductImage main = images.stream().filter(ProductImage::isMain).findFirst()
                .orElse(images.isEmpty() ? null : images.get(0));
        return main != null ? main.getUrl() : null;
    }

    private Brand findBrand(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("브랜드를 찾을 수 없습니다."));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다."));
    }

    private Product.Status parseStatus(String status) {
        if (status == null) return Product.Status.ON_SALE;
        try {
            return Product.Status.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("유효하지 않은 상품 상태입니다.");
        }
    }

    public record ProductRequest(Long brandId, Long categoryId, String name, String description,
                                 int price, int discountRate, String status, String imageUrl) {}

    public record OptionItem(Long id, String name) {}

    public record OptionRequest(String size, String color, int stock, int extraPrice) {}

    @Getter
    public static class OptionResponse {
        private final Long id;
        private final String size;
        private final String color;
        private final int stock;
        private final int extraPrice;

        public OptionResponse(ProductOption o) {
            this.id = o.getId();
            this.size = o.getSize();
            this.color = o.getColor();
            this.stock = o.getStock();
            this.extraPrice = o.getExtraPrice();
        }
    }

    public record MetaResponse(List<OptionItem> brands, List<OptionItem> categories) {}

    @Getter
    public static class AdminProductResponse {
        private final Long id;
        private final Long brandId;
        private final String brandName;
        private final Long categoryId;
        private final String categoryName;
        private final String name;
        private final String description;
        private final int price;
        private final int discountRate;
        private final int discountedPrice;
        private final String status;
        private final String mainImageUrl;

        public AdminProductResponse(Product p, String mainImageUrl) {
            this.id = p.getId();
            this.brandId = p.getBrand().getId();
            this.brandName = p.getBrand().getName();
            this.categoryId = p.getCategory().getId();
            this.categoryName = p.getCategory().getName();
            this.name = p.getName();
            this.description = p.getDescription();
            this.price = p.getPrice();
            this.discountRate = p.getDiscountRate();
            this.discountedPrice = p.getDiscountedPrice();
            this.status = p.getStatus().name();
            this.mainImageUrl = mainImageUrl;
        }
    }
}
