package dev.atelierwatch.watches.service;

import dev.atelierwatch.watches.dto.*;
import dev.atelierwatch.watches.model.Category;
import dev.atelierwatch.watches.model.Watch;
import dev.atelierwatch.watches.repository.CategoryRepository;
import dev.atelierwatch.watches.repository.WatchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class WatchService {

    private final WatchRepository watchRepository;
    private final CategoryRepository categoryRepository;

    public WatchService(WatchRepository watchRepository, CategoryRepository categoryRepository) {
        this.watchRepository = watchRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<WatchSummaryResponse> getFilteredWatches(
            String search,
            String brand,
            String collectionSlug,
            String categorySlug,
            String movementType,
            String caseMaterial,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        return watchRepository.findFiltered(
                normalizeFilter(search), normalizeFilter(brand), normalizeFilter(collectionSlug),
                normalizeFilter(categorySlug), normalizeFilter(movementType), normalizeFilter(caseMaterial),
                minPrice, maxPrice, pageable
        ).map(WatchSummaryResponse::from);
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    public WatchDetailResponse getWatchBySlug(String slug) {
        return watchRepository.findBySlugAndStatusAndBrandIgnoreCase(slug, "PUBLISHED", "Rolex")
                .map(WatchDetailResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Watch not found with slug: " + slug));
    }

    public List<WatchSummaryResponse> getFeaturedWatches() {
        return watchRepository.findByFeaturedTrueAndStatusAndBrandIgnoreCase("PUBLISHED", "Rolex")
                .stream()
                .map(WatchSummaryResponse::from)
                .toList();
    }

    public WatchComparisonResponse compareWatches(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("Comparison requires at least 2 watch IDs");
        }
        if (ids.size() > 4) {
            throw new IllegalArgumentException("Maximum of 4 watches can be compared side-by-side");
        }
        List<WatchDetailResponse> watches = watchRepository.findByIdInAndStatusAndBrandIgnoreCase(ids, "PUBLISHED", "Rolex")
                .stream()
                .map(WatchDetailResponse::from)
                .toList();
        return WatchComparisonResponse.from(watches);
    }

    public List<String> getBrands() {
        return watchRepository.findDistinctBrands();
    }

    public List<String> getCollections() {
        return watchRepository.findDistinctCollections();
    }

    public List<String> getCaseMaterials() {
        return watchRepository.findDistinctCaseMaterials();
    }

    public List<String> getMovementTypes() {
        return watchRepository.findDistinctMovementTypes();
    }

    public Page<WatchSummaryResponse> getAdminWatches(Pageable pageable) {
        return watchRepository.findAll(pageable).map(WatchSummaryResponse::from);
    }

    @Transactional
    public WatchDetailResponse createWatch(WatchCreateRequest req) {
        if (req.brand() == null || !"Rolex".equalsIgnoreCase(req.brand().trim())) {
            throw new IllegalArgumentException("Only Rolex catalog entries can be created");
        }
        if (watchRepository.findBySlug(req.slug()).isPresent()) {
            throw new IllegalArgumentException("A watch with slug '" + req.slug() + "' already exists");
        }
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + req.categoryId()));

        Watch watch = new Watch();
        watch.setName(req.name());
        watch.setBrand("Rolex");
        watch.setSlug(req.slug());
        watch.setTagline(req.tagline());
        watch.setDescription(req.description());
        watch.setCategory(category);
        watch.setPrice(req.price());
        watch.setCurrency(req.currency() != null ? req.currency() : "USD");
        watch.setMovementType(req.movementType());
        watch.setCaliber(req.caliber());
        watch.setCaseMaterial(req.caseMaterial());
        watch.setCaseDiameterMm(req.caseDiameterMm());
        watch.setCaseThicknessMm(req.caseThicknessMm());
        watch.setWaterResistanceM(req.waterResistanceM());
        watch.setPowerReserveHours(req.powerReserveHours());
        watch.setCrystalMaterial(req.crystalMaterial());
        watch.setDialColor(req.dialColor());
        watch.setStrapMaterial(req.strapMaterial());
        watch.setFunctions(req.functions());
        watch.setImageUrl(req.imageUrl());
        watch.setFeatured(req.featured());
        watch.setStatus(req.status() != null ? req.status() : "PUBLISHED");
        if (req.stockQuantity() != null) watch.setStockQuantity(req.stockQuantity());

        Watch saved = watchRepository.save(watch);
        return WatchDetailResponse.from(saved);
    }

    @Transactional
    public WatchDetailResponse updateWatch(Long id, WatchUpdateRequest req) {
        Watch watch = watchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Watch not found with ID: " + id));

        if (req.name() != null) watch.setName(req.name());
        if (req.brand() != null && !"Rolex".equalsIgnoreCase(req.brand().trim())) {
            throw new IllegalArgumentException("Only Rolex catalog entries can be published");
        }
        if (req.brand() != null) watch.setBrand("Rolex");
        if (req.tagline() != null) watch.setTagline(req.tagline());
        if (req.description() != null) watch.setDescription(req.description());
        if (req.categoryId() != null) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new NoSuchElementException("Category not found with ID: " + req.categoryId()));
            watch.setCategory(category);
        }
        if (req.price() != null) watch.setPrice(req.price());
        if (req.currency() != null) watch.setCurrency(req.currency());
        if (req.movementType() != null) watch.setMovementType(req.movementType());
        if (req.caliber() != null) watch.setCaliber(req.caliber());
        if (req.caseMaterial() != null) watch.setCaseMaterial(req.caseMaterial());
        if (req.caseDiameterMm() != null) watch.setCaseDiameterMm(req.caseDiameterMm());
        if (req.caseThicknessMm() != null) watch.setCaseThicknessMm(req.caseThicknessMm());
        if (req.waterResistanceM() != null) watch.setWaterResistanceM(req.waterResistanceM());
        if (req.powerReserveHours() != null) watch.setPowerReserveHours(req.powerReserveHours());
        if (req.crystalMaterial() != null) watch.setCrystalMaterial(req.crystalMaterial());
        if (req.dialColor() != null) watch.setDialColor(req.dialColor());
        if (req.strapMaterial() != null) watch.setStrapMaterial(req.strapMaterial());
        if (req.functions() != null) watch.setFunctions(req.functions());
        if (req.imageUrl() != null) watch.setImageUrl(req.imageUrl());
        if (req.featured() != null) watch.setFeatured(req.featured());
        if (req.status() != null) {
            if ("PUBLISHED".equalsIgnoreCase(req.status()) && !"Rolex".equalsIgnoreCase(watch.getBrand())) {
                throw new IllegalArgumentException("Only Rolex catalog entries can be published");
            }
            watch.setStatus(req.status());
        }
        if (req.stockQuantity() != null) watch.setStockQuantity(req.stockQuantity());

        return WatchDetailResponse.from(watchRepository.save(watch));
    }

    @Transactional
    public void deleteWatch(Long id) {
        Watch watch = watchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Watch not found with ID: " + id));
        watch.setStatus("ARCHIVED");
        watchRepository.save(watch);
    }
}
