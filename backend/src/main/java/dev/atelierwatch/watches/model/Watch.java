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

    @Column(name = "collection_name", length = 100)
    private String collectionName;

    @Column(name = "collection_slug", length = 100)
    private String collectionSlug;

    @Column(name = "retail_price", precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    @Column(name = "warranty_years")
    private Integer warrantyYears;

    @Column(name = "provenance_notes", columnDefinition = "TEXT")
    private String provenanceNotes;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 3)
    private String currency;

    @Column(name = "movement_type", nullable = false, columnDefinition = "TEXT")
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

    @Column(name = "case_type_description", columnDefinition = "TEXT")
    private String caseTypeDescription;
    @Column(name = "bezel_description", columnDefinition = "TEXT")
    private String bezelDescription;
    @Column(name = "case_back_description", columnDefinition = "TEXT")
    private String caseBackDescription;
    @Column(name = "winding_crown_description", columnDefinition = "TEXT")
    private String windingCrownDescription;
    @Column(name = "case_diameter_text", length = 50)
    private String caseDiameterText;
    @Column(name = "water_resistance_text", length = 255)
    private String waterResistanceText;
    @Column(name = "power_reserve_text", length = 100)
    private String powerReserveText;
    @Column(name = "precision_text", length = 100)
    private String precision;
    @Column(name = "oscillator_description", columnDefinition = "TEXT")
    private String oscillator;
    @Column(name = "winding_description", columnDefinition = "TEXT")
    private String winding;
    @Column(name = "bracelet_type", length = 255)
    private String braceletType;
    @Column(name = "clasp_description", columnDefinition = "TEXT")
    private String claspDescription;
    @Column(name = "dial_details", columnDefinition = "TEXT")
    private String dialDetails;
    @Column(columnDefinition = "TEXT")
    private String certification;
    @Column(name = "source_pdf", length = 255)
    private String sourcePdf;
    @Column(name = "source_filename_product_code", length = 100)
    private String sourceFilenameProductCode;
    @Column(name = "source_cover_page")
    private Integer sourceCoverPage;
    @Column(name = "source_description_page")
    private Integer sourceDescriptionPage;
    @Column(name = "source_specification_pages", length = 100)
    private String sourceSpecificationPages;
    @Column(name = "source_image_page")
    private Integer sourceImagePage;
    @Column(name = "source_image_object", length = 255)
    private String sourceImageObject;
    @Column(name = "image_source_path", length = 255)
    private String imageSourcePath;
    @Column(name = "image_sha256", length = 64)
    private String imageSha256;
    @Column(name = "image_rights_status", columnDefinition = "TEXT")
    private String imageRightsStatus;
    @Column(name = "image_verification_status", columnDefinition = "TEXT")
    private String imageVerificationStatus;

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
    public String getCaseTypeDescription() { return caseTypeDescription; }
    public void setCaseTypeDescription(String value) { caseTypeDescription = value; }
    public String getBezelDescription() { return bezelDescription; }
    public void setBezelDescription(String value) { bezelDescription = value; }
    public String getCaseBackDescription() { return caseBackDescription; }
    public void setCaseBackDescription(String value) { caseBackDescription = value; }
    public String getWindingCrownDescription() { return windingCrownDescription; }
    public void setWindingCrownDescription(String value) { windingCrownDescription = value; }
    public String getCaseDiameterText() { return caseDiameterText; }
    public void setCaseDiameterText(String value) { caseDiameterText = value; }
    public String getWaterResistanceText() { return waterResistanceText; }
    public void setWaterResistanceText(String value) { waterResistanceText = value; }
    public String getPowerReserveText() { return powerReserveText; }
    public void setPowerReserveText(String value) { powerReserveText = value; }
    public String getPrecision() { return precision; }
    public void setPrecision(String value) { precision = value; }
    public String getOscillator() { return oscillator; }
    public void setOscillator(String value) { oscillator = value; }
    public String getWinding() { return winding; }
    public void setWinding(String value) { winding = value; }
    public String getBraceletType() { return braceletType; }
    public void setBraceletType(String value) { braceletType = value; }
    public String getClaspDescription() { return claspDescription; }
    public void setClaspDescription(String value) { claspDescription = value; }
    public String getDialDetails() { return dialDetails; }
    public void setDialDetails(String value) { dialDetails = value; }
    public String getCertification() { return certification; }
    public void setCertification(String value) { certification = value; }
    public String getSourcePdf() { return sourcePdf; }
    public void setSourcePdf(String value) { sourcePdf = value; }
    public String getSourceFilenameProductCode() { return sourceFilenameProductCode; }
    public void setSourceFilenameProductCode(String value) { sourceFilenameProductCode = value; }
    public Integer getSourceCoverPage() { return sourceCoverPage; }
    public void setSourceCoverPage(Integer value) { sourceCoverPage = value; }
    public Integer getSourceDescriptionPage() { return sourceDescriptionPage; }
    public void setSourceDescriptionPage(Integer value) { sourceDescriptionPage = value; }
    public String getSourceSpecificationPages() { return sourceSpecificationPages; }
    public void setSourceSpecificationPages(String value) { sourceSpecificationPages = value; }
    public Integer getSourceImagePage() { return sourceImagePage; }
    public void setSourceImagePage(Integer value) { sourceImagePage = value; }
    public String getSourceImageObject() { return sourceImageObject; }
    public void setSourceImageObject(String value) { sourceImageObject = value; }
    public String getImageSourcePath() { return imageSourcePath; }
    public void setImageSourcePath(String value) { imageSourcePath = value; }
    public String getImageSha256() { return imageSha256; }
    public void setImageSha256(String value) { imageSha256 = value; }
    public String getImageRightsStatus() { return imageRightsStatus; }
    public void setImageRightsStatus(String value) { imageRightsStatus = value; }
    public String getImageVerificationStatus() { return imageVerificationStatus; }
    public void setImageVerificationStatus(String value) { imageVerificationStatus = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getCollectionName() { return collectionName; }
    public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
    public String getCollectionSlug() { return collectionSlug; }
    public void setCollectionSlug(String collectionSlug) { this.collectionSlug = collectionSlug; }
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
