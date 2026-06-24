package com.musinsaclone.product;

import com.musinsaclone.brand.entity.Brand;
import com.musinsaclone.category.entity.Category;
import com.musinsaclone.product.entity.Product;
import com.musinsaclone.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private TestEntityManager em;

    private Product product(Brand brand, Category category, String name, int price, int discountRate) {
        Product p = Product.builder().brand(brand).category(category).name(name)
                .description("d").price(price).discountRate(discountRate)
                .status(Product.Status.ON_SALE).build();
        return em.persist(p);
    }

    @Test
    @DisplayName("findBestSellers 네이티브 쿼리가 H2에서 실행되고 필터·카운트가 동작한다")
    void findBestSellers_runsAndCounts() {
        Brand brand = em.persist(Brand.builder().name("브랜드A").build());
        Category category = em.persist(Category.builder().name("상의").sortOrder(0).build());
        product(brand, category, "상품1", 10000, 0);
        product(brand, category, "상품2", 20000, 10);  // 할인 상품
        product(brand, category, "상품3", 30000, 0);
        em.flush();

        // 주문이 없으므로 판매량 0 동률 → id DESC 로 전부 반환
        Page<Product> all = productRepository.findBestSellers(null, null, false, 0, 10_000_000, PageRequest.of(0, 10));
        assertThat(all.getTotalElements()).isEqualTo(3);   // countQuery 검증
        assertThat(all.getContent()).hasSize(3);

        // 세일 필터: 할인 상품만
        Page<Product> saleOnly = productRepository.findBestSellers(null, null, true, 0, 10_000_000, PageRequest.of(0, 10));
        assertThat(saleOnly.getTotalElements()).isEqualTo(1);
        assertThat(saleOnly.getContent().get(0).getName()).isEqualTo("상품2");

        // 가격 필터: 15000 이하
        Page<Product> cheap = productRepository.findBestSellers(null, null, false, 0, 15_000, PageRequest.of(0, 10));
        assertThat(cheap.getTotalElements()).isEqualTo(1);
        assertThat(cheap.getContent().get(0).getName()).isEqualTo("상품1");
    }
}
