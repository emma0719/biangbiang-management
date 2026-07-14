package com.restaurant.ops.ordering;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "purchase_order_id")
  private PurchaseOrder purchaseOrder;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private OrderCatalogProduct product;

  @Column(nullable = false)
  private String productNameSnapshot;

  private String productCodeSnapshot;
  private String packageSpecificationSnapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit orderUnitSnapshot;

  @Column(nullable = false)
  private BigDecimal unitPriceSnapshot = BigDecimal.ZERO;

  private BigDecimal sourceInventoryQuantitySnapshot;
  private BigDecimal suggestedOrderQuantity;
  private BigDecimal finalOrderQuantity;

  private BigDecimal currentInventoryQuantity;

  @Column(nullable = false)
  private BigDecimal requestedQuantity = BigDecimal.ZERO;

  private BigDecimal approvedQuantity;

  @Column(nullable = false)
  private BigDecimal receivedQuantity = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal lineTotal = BigDecimal.ZERO;

  @Column(columnDefinition = "TEXT")
  private String notes;

  public Long getId() { return id; }
  public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
  public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
  public OrderCatalogProduct getProduct() { return product; }
  public void setProduct(OrderCatalogProduct product) { this.product = product; }
  public String getProductNameSnapshot() { return productNameSnapshot; }
  public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
  public String getProductCodeSnapshot() { return productCodeSnapshot; }
  public void setProductCodeSnapshot(String productCodeSnapshot) { this.productCodeSnapshot = productCodeSnapshot; }
  public String getPackageSpecificationSnapshot() { return packageSpecificationSnapshot; }
  public void setPackageSpecificationSnapshot(String packageSpecificationSnapshot) { this.packageSpecificationSnapshot = packageSpecificationSnapshot; }
  public CatalogUnit getOrderUnitSnapshot() { return orderUnitSnapshot; }
  public void setOrderUnitSnapshot(CatalogUnit orderUnitSnapshot) { this.orderUnitSnapshot = orderUnitSnapshot; }
  public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
  public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
  public BigDecimal getSourceInventoryQuantitySnapshot() { return sourceInventoryQuantitySnapshot; }
  public void setSourceInventoryQuantitySnapshot(BigDecimal sourceInventoryQuantitySnapshot) { this.sourceInventoryQuantitySnapshot = sourceInventoryQuantitySnapshot; }
  public BigDecimal getSuggestedOrderQuantity() { return suggestedOrderQuantity; }
  public void setSuggestedOrderQuantity(BigDecimal suggestedOrderQuantity) { this.suggestedOrderQuantity = suggestedOrderQuantity; }
  public BigDecimal getFinalOrderQuantity() { return finalOrderQuantity; }
  public void setFinalOrderQuantity(BigDecimal finalOrderQuantity) { this.finalOrderQuantity = finalOrderQuantity; }
  public BigDecimal getCurrentInventoryQuantity() { return currentInventoryQuantity; }
  public void setCurrentInventoryQuantity(BigDecimal currentInventoryQuantity) { this.currentInventoryQuantity = currentInventoryQuantity; }
  public BigDecimal getRequestedQuantity() { return requestedQuantity; }
  public void setRequestedQuantity(BigDecimal requestedQuantity) { this.requestedQuantity = requestedQuantity; }
  public BigDecimal getApprovedQuantity() { return approvedQuantity; }
  public void setApprovedQuantity(BigDecimal approvedQuantity) { this.approvedQuantity = approvedQuantity; }
  public BigDecimal getReceivedQuantity() { return receivedQuantity; }
  public void setReceivedQuantity(BigDecimal receivedQuantity) { this.receivedQuantity = receivedQuantity; }
  public BigDecimal getLineTotal() { return lineTotal; }
  public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
}
