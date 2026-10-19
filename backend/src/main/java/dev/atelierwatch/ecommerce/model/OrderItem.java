package dev.atelierwatch.ecommerce.model;

import dev.atelierwatch.watches.model.Watch;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "watch_id", nullable = false)
    private Watch watch;

    @Column(name = "snapshot_name", nullable = false, length = 150)
    private String snapshotName;

    @Column(name = "snapshot_brand", nullable = false, length = 100)
    private String snapshotBrand;

    @Column(name = "snapshot_reference", length = 100)
    private String snapshotReference;

    @Column(name = "snapshot_image_url", length = 500)
    private String snapshotImageUrl;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    public OrderItem() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public Watch getWatch() { return watch; }
    public void setWatch(Watch watch) { this.watch = watch; }
    public String getSnapshotName() { return snapshotName; }
    public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
    public String getSnapshotBrand() { return snapshotBrand; }
    public void setSnapshotBrand(String snapshotBrand) { this.snapshotBrand = snapshotBrand; }
    public String getSnapshotReference() { return snapshotReference; }
    public void setSnapshotReference(String snapshotReference) { this.snapshotReference = snapshotReference; }
    public String getSnapshotImageUrl() { return snapshotImageUrl; }
    public void setSnapshotImageUrl(String snapshotImageUrl) { this.snapshotImageUrl = snapshotImageUrl; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}
