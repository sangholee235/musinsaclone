package com.musinsaclone.category.controller;

import com.musinsaclone.category.entity.Category;
import com.musinsaclone.category.repository.CategoryRepository;
import com.musinsaclone.common.response.ApiResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getRootCategories() {
        return ApiResponse.ok(categoryRepository.findByParentIsNull().stream()
                .map(CategoryResponse::new).toList());
    }

    @GetMapping("/{categoryId}/children")
    public ApiResponse<List<CategoryResponse>> getChildren(@PathVariable Long categoryId) {
        return ApiResponse.ok(categoryRepository.findByParentId(categoryId).stream()
                .map(CategoryResponse::new).toList());
    }

    @Getter
    static class CategoryResponse {
        private final Long id;
        private final String name;
        private final int sortOrder;

        CategoryResponse(Category category) {
            this.id = category.getId();
            this.name = category.getName();
            this.sortOrder = category.getSortOrder();
        }
    }
}
