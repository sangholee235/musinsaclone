package com.musinsaclone.brand.service;

import com.musinsaclone.brand.entity.Brand;
import com.musinsaclone.brand.entity.BrandFollow;
import com.musinsaclone.brand.repository.BrandFollowRepository;
import com.musinsaclone.brand.repository.BrandRepository;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final BrandFollowRepository brandFollowRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<BrandResponse> getBrands(Pageable pageable) {
        return brandRepository.findAll(pageable).map(b ->
                new BrandResponse(b, brandFollowRepository.countByBrandId(b.getId())));
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> BusinessException.notFound("브랜드를 찾을 수 없습니다."));
        return new BrandResponse(brand, brandFollowRepository.countByBrandId(brandId));
    }

    @Transactional
    public boolean toggleFollow(Long userId, Long brandId) {
        return brandFollowRepository.findByUserIdAndBrandId(userId, brandId)
                .map(f -> { brandFollowRepository.delete(f); return false; })
                .orElseGet(() -> {
                    User user = userRepository.getReferenceById(userId);
                    Brand brand = brandRepository.findById(brandId)
                            .orElseThrow(() -> BusinessException.notFound("브랜드를 찾을 수 없습니다."));
                    brandFollowRepository.save(BrandFollow.builder().user(user).brand(brand).build());
                    return true;
                });
    }

    @Getter
    public static class BrandResponse {
        private final Long id;
        private final String name;
        private final String logoUrl;
        private final long followerCount;

        public BrandResponse(Brand brand, long followerCount) {
            this.id = brand.getId();
            this.name = brand.getName();
            this.logoUrl = brand.getLogoUrl();
            this.followerCount = followerCount;
        }
    }
}
