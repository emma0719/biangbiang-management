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
@Table(name = "inventory_count_lines")
public class InventoryCountLine {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "session_id")
  private InventoryCountSession session;

  @ManyToOne(optional = false)
  @JoinColumn(name = "product_id")
  private OrderCatalogProduct product;

  @Column(nullable = false)
  private BigDecimal quantityOnHand = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CatalogUnit unit = CatalogUnit.OTHER;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(optional = false)
  @JoinColumn(name = "updated_by_employee_id")
  private Employee updatedBy;

  @Column(nullable = false)
  private Instant updatedAt;

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  public Long getId() { return id; }
  public InventoryCountSession getSession() { return session; }
  public void setSession(InventoryCountSession session) { this.session = session; }
  public OrderCatalogProduct getProduct() { return product; }
  public void setProduct(OrderCatalogProduct product) { this.product = product; }
  public BigDecimal getQuantityOnHand() { return quantityOnHand; }
  public void setQuantityOnHand(BigDecimal quantityOnHand) { this.quantityOnHand = quantityOnHand; }
  public CatalogUnit getUnit() { return unit; }
  public void setUnit(CatalogUnit unit) { this.unit = unit; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Employee getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(Employee updatedBy) { this.updatedBy = updatedBy; }
  public Instant getUpdatedAt() { return updatedAt; }
}
