package dev.atelierwatch.watches.controller;

import dev.atelierwatch.common.ApiResponse;
import dev.atelierwatch.common.PageResponse;
import dev.atelierwatch.watches.dto.*;
import dev.atelierwatch.watches.service.WatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Watches", description = "Rolex catalog, watch details, search, filtering, and administration")
public class WatchController {

    private final WatchService watchService;

    public WatchController(WatchService watchService) {
        this.watchService = watchService;
    }

    @GetMapping("/watches")
    @Operation(summary = "Get paginated, filtered catalog of luxury watches")
    public ResponseEntity<ApiResponse<PageResponse<WatchSummaryResponse>>> getWatches(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String collection,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String movement,
            @RequestParam(required = false) String material,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortProp = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)), Sort.by(direction, sortProp));
        var result = watchService.getFilteredWatches(search, brand, collection, category, movement, material, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @GetMapping("/watches/featured")
    @Operation(summary = "Get curated featured watches for spotlight sections")
    public ResponseEntity<ApiResponse<List<WatchSummaryResponse>>> getFeatured() {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getFeaturedWatches()));
    }

    @GetMapping("/watches/brands")
    @Operation(summary = "Get distinct luxury brands present in the catalog")
    public ResponseEntity<ApiResponse<List<String>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getBrands()));
    }

    @GetMapping("/watches/collections")
    @Operation(summary = "Get the Rolex collections in the public catalog")
    public ResponseEntity<ApiResponse<List<String>>> getCollections() {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getCollections()));
    }

    @GetMapping("/watches/materials")
    @Operation(summary = "Get distinct case materials in the public Rolex catalog")
    public ResponseEntity<ApiResponse<List<String>>> getCaseMaterials() {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getCaseMaterials()));
    }

    @GetMapping("/watches/movements")
    @Operation(summary = "Get distinct movements in the public Rolex catalog")
    public ResponseEntity<ApiResponse<List<String>>> getMovementTypes() {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getMovementTypes()));
    }

    @GetMapping("/watches/compare")
    @Operation(summary = "Compare up to four published Rolex watches by ID")
    public ResponseEntity<ApiResponse<WatchComparisonResponse>> compareWatches(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.ok(watchService.compareWatches(ids)));
    }

    @GetMapping("/admin/watches")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: list published, draft, and archived watches")
    public ResponseEntity<ApiResponse<PageResponse<WatchSummaryResponse>>> getAdminWatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "id,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        String sortProp = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(direction, sortProp));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(watchService.getAdminWatches(pageable))));
    }

    @GetMapping("/watches/{slug}")
    @Operation(summary = "Get comprehensive technical details of a watch by unique slug")
    public ResponseEntity<ApiResponse<WatchDetailResponse>> getWatchBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(watchService.getWatchBySlug(slug)));
    }

    @PostMapping("/admin/watches")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Create a new luxury timepiece entry")
    public ResponseEntity<ApiResponse<WatchDetailResponse>> createWatch(@Valid @RequestBody WatchCreateRequest request) {
        var created = watchService.createWatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Watch created successfully", created));
    }

    @PatchMapping("/admin/watches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Update an existing watch")
    public ResponseEntity<ApiResponse<WatchDetailResponse>> updateWatch(
            @PathVariable Long id,
            @Valid @RequestBody WatchUpdateRequest request
    ) {
        var updated = watchService.updateWatch(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Watch updated successfully", updated));
    }

    @DeleteMapping("/admin/watches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: Archive a watch from the catalog")
    public ResponseEntity<ApiResponse<Void>> deleteWatch(@PathVariable Long id) {
        watchService.deleteWatch(id);
        return ResponseEntity.ok(ApiResponse.ok("Watch archived successfully", null));
    }
}
