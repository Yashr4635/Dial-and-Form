package dev.atelierwatch.watches.repository;

import dev.atelierwatch.watches.model.Watch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WatchRepository extends JpaRepository<Watch, Long>, JpaSpecificationExecutor<Watch> {

    Optional<Watch> findBySlug(String slug);

    Optional<Watch> findBySlugAndStatusAndBrandIgnoreCase(String slug, String status, String brand);

    List<Watch> findByFeaturedTrueAndStatusAndBrandIgnoreCase(String status, String brand);

    @Query("SELECT w FROM Watch w WHERE w.status = 'PUBLISHED' AND LOWER(w.brand) = 'rolex' " +
           "AND (:search = '' OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.brand) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.caliber) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:brand = '' OR LOWER(w.brand) = LOWER(:brand)) " +
           "AND (:collectionSlug = '' OR w.collectionSlug = :collectionSlug) " +
           "AND (:categorySlug = '' OR w.category.slug = :categorySlug) " +
           "AND (:movementType = '' OR w.movementType = :movementType) " +
           "AND (:caseMaterial = '' OR LOWER(w.caseMaterial) LIKE LOWER(CONCAT('%', :caseMaterial, '%'))) " +
           "AND (:minPrice IS NULL OR w.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR w.price <= :maxPrice)")
    Page<Watch> findFiltered(
            @Param("search") String search,
            @Param("brand") String brand,
            @Param("collectionSlug") String collectionSlug,
            @Param("categorySlug") String categorySlug,
            @Param("movementType") String movementType,
            @Param("caseMaterial") String caseMaterial,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    List<Watch> findByIdInAndStatusAndBrandIgnoreCase(List<Long> ids, String status, String brand);

    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT w.brand FROM Watch w WHERE w.status = 'PUBLISHED' AND LOWER(w.brand) = 'rolex' ORDER BY w.brand")
    List<String> findDistinctBrands();

    @Query("SELECT DISTINCT w.collectionName FROM Watch w WHERE w.status = 'PUBLISHED' AND LOWER(w.brand) = 'rolex' AND w.collectionName IS NOT NULL ORDER BY w.collectionName")
    List<String> findDistinctCollections();

    @Query("SELECT DISTINCT w.caseMaterial FROM Watch w WHERE w.status = 'PUBLISHED' AND LOWER(w.brand) = 'rolex' ORDER BY w.caseMaterial")
    List<String> findDistinctCaseMaterials();

    @Query("SELECT DISTINCT w.movementType FROM Watch w WHERE w.status = 'PUBLISHED' AND LOWER(w.brand) = 'rolex' ORDER BY w.movementType")
    List<String> findDistinctMovementTypes();
}
