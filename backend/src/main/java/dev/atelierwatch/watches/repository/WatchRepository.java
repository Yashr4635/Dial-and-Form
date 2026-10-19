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

    List<Watch> findByFeaturedTrueAndStatus(String status);

    @Query("SELECT w FROM Watch w WHERE w.status = 'PUBLISHED' " +
           "AND (:search IS NULL OR LOWER(w.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(w.brand) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:categorySlug IS NULL OR w.category.slug = :categorySlug) " +
           "AND (:movementType IS NULL OR w.movementType = :movementType) " +
           "AND (:caseMaterial IS NULL OR LOWER(w.caseMaterial) LIKE LOWER(CONCAT('%', :caseMaterial, '%'))) " +
           "AND (:minPrice IS NULL OR w.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR w.price <= :maxPrice)")
    Page<Watch> findFiltered(
            @Param("search") String search,
            @Param("categorySlug") String categorySlug,
            @Param("movementType") String movementType,
            @Param("caseMaterial") String caseMaterial,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );

    List<Watch> findByIdInAndStatus(List<Long> ids, String status);

    @Query("SELECT DISTINCT w.brand FROM Watch w WHERE w.status = 'PUBLISHED' ORDER BY w.brand")
    List<String> findDistinctBrands();
}
