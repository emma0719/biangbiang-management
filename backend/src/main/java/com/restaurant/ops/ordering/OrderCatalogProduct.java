package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
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
@Table(name = "order_catalog_products")
public class OrderCatalogProduct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_code", nullable = false)
  private StoreCode locationCode;

  @ManyToOne(optional = false)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  private String vendorProductCode;
  private String sourceKey;
  private String sourceVersion;
  @Column(nullable = false)
  private boolean importedFromReference;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String normalizedName;

  private String chineseDisplayName;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogCategory category = CatalogCategory.OTHER;

  @Enumerated(EnumType.STRING)
  private InventoryBusiness inventoryBusiness;

  @Enumerated(EnumType.STRING)
  private OrderBusiness orderBusiness;

  private String storageArea;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit inventoryUnit = CatalogUnit.OTHER;

  private String inventoryUnitLabel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit orderUnit = CatalogUnit.OTHER;

  private String orderUnitLabel;
  private String packageSpecification;
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private String currency = "USD";

  private BigDecimal parLevel;
  private BigDecimal reorderPoint;
  private BigDecimal defaultOrderQuantity;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private int displayOrder;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  void prePersist() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public StoreCode getLocationCode() { return locationCode; }
  public void setLocationCode(StoreCode locationCode) { this.locationCode = locationCode; }
  public Vendor getVendor() { return vendor; }
  public void setVendor(Vendor vendor) { this.vendor = vendor; }
  public String getVendorProductCode() { return vendorProductCode; }
  public void setVendorProductCode(String vendorProductCode) { this.vendorProductCode = vendorProductCode; }
  public String getSourceKey() { return sourceKey; }
  public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
  public String getSourceVersion() { return sourceVersion; }
  public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
  public boolean isImportedFromReference() { return importedFromReference; }
  public void setImportedFromReference(boolean importedFromReference) { this.importedFromReference = importedFromReference; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getNormalizedName() { return normalizedName; }
  public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
  public String getChineseDisplayName() { return chineseDisplayName; }
  public void setChineseDisplayName(String chineseDisplayName) { this.chineseDisplayName = chineseDisplayName; }
  public CatalogCategory getCategory() { return category; }
  public void setCategory(CatalogCategory category) { this.category = category; }
  public InventoryBusiness getInventoryBusiness() { return inventoryBusiness; }
  public void setInventoryBusiness(InventoryBusiness inventoryBusiness) { this.inventoryBusiness = inventoryBusiness; }
  public OrderBusiness getOrderBusiness() { return orderBusiness; }
  public void setOrderBusiness(OrderBusiness orderBusiness) { this.orderBusiness = orderBusiness; }
  public String getStorageArea() { return storageArea; }
  public void setStorageArea(String storageArea) { this.storageArea = storageArea; }
  public CatalogUnit getInventoryUnit() { return inventoryUnit; }
  public void setInventoryUnit(CatalogUnit inventoryUnit) { this.inventoryUnit = inventoryUnit; }
  public String getInventoryUnitLabel() { return inventoryUnitLabel; }
  public void setInventoryUnitLabel(String inventoryUnitLabel) { this.inventoryUnitLabel = inventoryUnitLabel; }
  public CatalogUnit getOrderUnit() { return orderUnit; }
  public void setOrderUnit(CatalogUnit orderUnit) { this.orderUnit = orderUnit; }
  public String getOrderUnitLabel() { return orderUnitLabel; }
  public void setOrderUnitLabel(String orderUnitLabel) { this.orderUnitLabel = orderUnitLabel; }
  public String getPackageSpecification() { return packageSpecification; }
  public void setPackageSpecification(String packageSpecification) { this.packageSpecification = packageSpecification; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public BigDecimal getParLevel() { return parLevel; }
  public void setParLevel(BigDecimal parLevel) { this.parLevel = parLevel; }
  public BigDecimal getReorderPoint() { return reorderPoint; }
  public void setReorderPoint(BigDecimal reorderPoint) { this.reorderPoint = reorderPoint; }
  public BigDecimal getDefaultOrderQuantity() { return defaultOrderQuantity; }
  public void setDefaultOrderQuantity(BigDecimal defaultOrderQuantity) { this.defaultOrderQuantity = defaultOrderQuantity; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
