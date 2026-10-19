package dev.atelierwatch.watches.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "watches")
public class Watch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    @Column(length = 255)
    private String tagline;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "retail_price", precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 5;

    @Column(name = "warranty_years")
    private Integer warrantyYears = 5;

    @Column(name = "provenance_notes", columnDefinition = "TEXT")
    private String provenanceNotes;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(length = 100)
    private String caliber;

    @Column(name = "case_material", nullable = false, length = 100)
    private String caseMaterial;

    @Column(name = "case_diameter_mm", nullable = false, precision = 4, scale = 1)
    private BigDecimal caseDiameterMm;

    @Column(name = "case_thickness_mm", precision = 4, scale = 1)
    private BigDecimal caseThicknessMm;

    @Column(name = "water_resistance_m", nullable = false)
    private Integer waterResistanceM;

    @Column(name = "power_reserve_hours", nullable = false)
    private Integer powerReserveHours;

    @Column(name = "crystal_material", nullable = false, length = 100)
    private String crystalMaterial;

    @Column(name = "dial_color", nullable = false, length = 100)
    private String dialColor;

    @Column(name = "strap_material", length = 100)
    private String strapMaterial;

    @Column(columnDefinition = "TEXT")
    private String functions;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "is_featured", nullable = false)
    private boolean featured = false;

    @Column(nullable = false, length = 20)
    private String status = "PUBLISHED";

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public Watch() {}

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public String getCaliber() { return caliber; }
    public void setCaliber(String caliber) { this.caliber = caliber; }
    public String getCaseMaterial() { return caseMaterial; }
    public void setCaseMaterial(String caseMaterial) { this.caseMaterial = caseMaterial; }
    public BigDecimal getCaseDiameterMm() { return caseDiameterMm; }
    public void setCaseDiameterMm(BigDecimal caseDiameterMm) { this.caseDiameterMm = caseDiameterMm; }
    public BigDecimal getCaseThicknessMm() { return caseThicknessMm; }
    public void setCaseThicknessMm(BigDecimal caseThicknessMm) { this.caseThicknessMm = caseThicknessMm; }
    public Integer getWaterResistanceM() { return waterResistanceM; }
    public void setWaterResistanceM(Integer waterResistanceM) { this.waterResistanceM = waterResistanceM; }
    public Integer getPowerReserveHours() { return powerReserveHours; }
    public void setPowerReserveHours(Integer powerReserveHours) { this.powerReserveHours = powerReserveHours; }
    public String getCrystalMaterial() { return crystalMaterial; }
    public void setCrystalMaterial(String crystalMaterial) { this.crystalMaterial = crystalMaterial; }
    public String getDialColor() { return dialColor; }
    public void setDialColor(String dialColor) { this.dialColor = dialColor; }
    public String getStrapMaterial() { return strapMaterial; }
    public void setStrapMaterial(String strapMaterial) { this.strapMaterial = strapMaterial; }
    public String getFunctions() { return functions; }
    public void setFunctions(String functions) { this.functions = functions; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    public Integer getWarrantyYears() { return warrantyYears; }
    public void setWarrantyYears(Integer warrantyYears) { this.warrantyYears = warrantyYears; }
    public String getProvenanceNotes() { return provenanceNotes; }
    public void setProvenanceNotes(String provenanceNotes) { this.provenanceNotes = provenanceNotes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
