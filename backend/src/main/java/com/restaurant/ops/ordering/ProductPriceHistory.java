package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "product_price_history")
public class ProductPriceHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private OrderCatalogProduct product;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private LocalDate effectiveDate;

  @ManyToOne
  @JoinColumn(name = "purchase_order_id")
  private PurchaseOrder purchaseOrder;

  @ManyToOne(optional = false)
  @JoinColumn(name = "entered_by_employee_id")
  private Employee enteredBy;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public OrderCatalogProduct getProduct() { return product; }
  public void setProduct(OrderCatalogProduct product) { this.product = product; }
  public BigDecimal getUnitPrice() { return unitPrice; }
  public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
  public LocalDate getEffectiveDate() { return effectiveDate; }
  public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
  public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
  public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }
  public Employee getEnteredBy() { return enteredBy; }
  public void setEnteredBy(Employee enteredBy) { this.enteredBy = enteredBy; }
  public Instant getCreatedAt() { return createdAt; }
}
