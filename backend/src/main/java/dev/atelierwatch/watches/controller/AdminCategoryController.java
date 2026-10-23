package dev.atelierwatch.watches.controller;

import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.watches.dto.CategoryResponse;
import dev.atelierwatch.watches.dto.CategoryWriteRequest;
import dev.atelierwatch.watches.model.Category;
import dev.atelierwatch.watches.repository.CategoryRepository;
import dev.atelierwatch.watches.repository.WatchRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {
    private final CategoryRepository categoryRepository;
    private final WatchRepository watchRepository;

    public AdminCategoryController(CategoryRepository categoryRepository, WatchRepository watchRepository) {
        this.categoryRepository = categoryRepository;
        this.watchRepository = watchRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(categoryRepository.findAll().stream().map(CategoryResponse::from).toList()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryWriteRequest request) {
        if (categoryRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("A category with this slug already exists");
        }
        Category category = new Category(null, request.name().trim(), request.slug().trim(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created", CategoryResponse.from(categoryRepository.save(category))));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id, @Valid @RequestBody CategoryWriteRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));
        categoryRepository.findBySlug(request.slug()).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new IllegalArgumentException("A category with this slug already exists");
        });
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim());
        category.setDescription(request.description());
        return ResponseEntity.ok(ApiResponse.ok("Category updated", CategoryResponse.from(categoryRepository.save(category))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Category not found: " + id));
        if (watchRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("This category is assigned to watches and cannot be removed");
        }
        categoryRepository.delete(category);
        return ResponseEntity.ok(ApiResponse.ok("Category removed", null));
    }
}
