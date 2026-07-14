package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_plan_lines")
public class OrderPlanLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "order_plan_session_id")
  private OrderPlanSession orderPlanSession;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private OrderCatalogProduct product;

  @ManyToOne(optional = false)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  @Column(nullable = false)
  private String productNameSnapshot;
  private String productCodeSnapshot;

  @Column(nullable = false)
  private String vendorNameSnapshot;

  private String packageSpecificationSnapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit inventoryUnitSnapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit orderUnitSnapshot;

  private BigDecimal unitPriceSnapshot;
  private BigDecimal sourceInventoryQuantity;
  private BigDecimal parLevelSnapshot;
  private BigDecimal reorderPointSnapshot;
  private BigDecimal previousOrderQuantity;
  private BigDecimal suggestedOrderQuantity;
  private BigDecimal finalOrderQuantity;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne
  @JoinColumn(name = "updated_by_employee_id")
  private Employee updatedBy;

  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public OrderPlanSession getOrderPlanSession() { return orderPlanSession; }
  public void setOrderPlanSession(OrderPlanSession orderPlanSession) { this.orderPlanSession = orderPlanSession; }
  public OrderCatalogProduct getProduct() { return product; }
  public void setProduct(OrderCatalogProduct product) { this.product = product; }
  public Vendor getVendor() { return vendor; }
  public void setVendor(Vendor vendor) { this.vendor = vendor; }
  public String getProductNameSnapshot() { return productNameSnapshot; }
  public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
  public String getProductCodeSnapshot() { return productCodeSnapshot; }
  public void setProductCodeSnapshot(String productCodeSnapshot) { this.productCodeSnapshot = productCodeSnapshot; }
  public String getVendorNameSnapshot() { return vendorNameSnapshot; }
  public void setVendorNameSnapshot(String vendorNameSnapshot) { this.vendorNameSnapshot = vendorNameSnapshot; }
  public String getPackageSpecificationSnapshot() { return packageSpecificationSnapshot; }
  public void setPackageSpecificationSnapshot(String packageSpecificationSnapshot) { this.packageSpecificationSnapshot = packageSpecificationSnapshot; }
  public CatalogUnit getInventoryUnitSnapshot() { return inventoryUnitSnapshot; }
  public void setInventoryUnitSnapshot(CatalogUnit inventoryUnitSnapshot) { this.inventoryUnitSnapshot = inventoryUnitSnapshot; }
  public CatalogUnit getOrderUnitSnapshot() { return orderUnitSnapshot; }
  public void setOrderUnitSnapshot(CatalogUnit orderUnitSnapshot) { this.orderUnitSnapshot = orderUnitSnapshot; }
  public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
  public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
  public BigDecimal getSourceInventoryQuantity() { return sourceInventoryQuantity; }
  public void setSourceInventoryQuantity(BigDecimal sourceInventoryQuantity) { this.sourceInventoryQuantity = sourceInventoryQuantity; }
  public BigDecimal getParLevelSnapshot() { return parLevelSnapshot; }
  public void setParLevelSnapshot(BigDecimal parLevelSnapshot) { this.parLevelSnapshot = parLevelSnapshot; }
  public BigDecimal getReorderPointSnapshot() { return reorderPointSnapshot; }
  public void setReorderPointSnapshot(BigDecimal reorderPointSnapshot) { this.reorderPointSnapshot = reorderPointSnapshot; }
  public BigDecimal getPreviousOrderQuantity() { return previousOrderQuantity; }
  public void setPreviousOrderQuantity(BigDecimal previousOrderQuantity) { this.previousOrderQuantity = previousOrderQuantity; }
  public BigDecimal getSuggestedOrderQuantity() { return suggestedOrderQuantity; }
  public void setSuggestedOrderQuantity(BigDecimal suggestedOrderQuantity) { this.suggestedOrderQuantity = suggestedOrderQuantity; }
  public BigDecimal getFinalOrderQuantity() { return finalOrderQuantity; }
  public void setFinalOrderQuantity(BigDecimal finalOrderQuantity) { this.finalOrderQuantity = finalOrderQuantity; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Employee getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Employee updatedBy) { this.updatedBy = updatedBy; }
  public Instant getUpdatedAt() { return updatedAt; }
}
