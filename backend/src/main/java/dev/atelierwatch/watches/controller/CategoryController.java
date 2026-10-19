package dev.atelierwatch.watches.controller;

import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.watches.dto.CategoryResponse;
import dev.atelierwatch.watches.repository.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Endpoints for luxury watch categories")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "Get all watch categories with descriptions")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        var categories = categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }
}
