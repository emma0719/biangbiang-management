package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
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
import java.time.LocalDate;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_code", nullable = false)
  private StoreCode locationCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderBusiness orderBusiness = OrderBusiness.BIANGBIANG_FRONT;

  @ManyToOne(optional = false)
  @JoinColumn(name = "vendor_id")
  private Vendor vendor;

  @ManyToOne
  @JoinColumn(name = "source_inventory_session_id")
  private InventoryCountSession sourceInventorySession;

  @ManyToOne
  @JoinColumn(name = "order_plan_session_id")
  private OrderPlanSession orderPlanSession;

  @Column(nullable = false, unique = true)
  private String orderNumber;

  @Column(nullable = false)
  private LocalDate businessDate;

  private LocalDate expectedDeliveryDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

  @Column(nullable = false)
  private BigDecimal subtotal = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal fees = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false)
  private String currency = "USD";

  @ManyToOne(optional = false)
  @JoinColumn(name = "created_by_employee_id")
  private Employee createdBy;

  @ManyToOne
  @JoinColumn(name = "submitted_by_employee_id")
  private Employee submittedBy;

  private String submittedByNameSnapshot;

  @ManyToOne
  @JoinColumn(name = "approved_by_employee_id")
  private Employee approvedBy;

  private String approvedByNameSnapshot;

  @ManyToOne
  @JoinColumn(name = "rejected_by_employee_id")
  private Employee rejectedBy;

  private String rejectedByNameSnapshot;

  @ManyToOne
  @JoinColumn(name = "ordered_by_employee_id")
  private Employee orderedBy;

  @ManyToOne
  @JoinColumn(name = "received_by_employee_id")
  private Employee receivedBy;

  @Column(columnDefinition = "TEXT")
  private String rejectionReason;

  private String vendorConfirmationNumber;

  @Column(columnDefinition = "TEXT")
  private String externalOrderNotes;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant submittedAt;
  private Instant approvedAt;
  private Instant orderedAt;
  private Instant receivedAt;
  private Instant rejectedAt;

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
  public OrderBusiness getOrderBusiness() { return orderBusiness; }
  public void setOrderBusiness(OrderBusiness orderBusiness) { this.orderBusiness = orderBusiness; }
  public Vendor getVendor() { return vendor; }
  public void setVendor(Vendor vendor) { this.vendor = vendor; }
  public InventoryCountSession getSourceInventorySession() { return sourceInventorySession; }
  public void setSourceInventorySession(InventoryCountSession sourceInventorySession) { this.sourceInventorySession = sourceInventorySession; }
  public OrderPlanSession getOrderPlanSession() { return orderPlanSession; }
  public void setOrderPlanSession(OrderPlanSession orderPlanSession) { this.orderPlanSession = orderPlanSession; }
  public String getOrderNumber() { return orderNumber; }
  public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
  public LocalDate getBusinessDate() { return businessDate; }
  public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
  public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
  public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) { this.expectedDeliveryDate = expectedDeliveryDate; }
  public PurchaseOrderStatus getStatus() { return status; }
  public void setStatus(PurchaseOrderStatus status) { this.status = status; }
  public BigDecimal getSubtotal() { return subtotal; }
  public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
  public BigDecimal getTax() { return tax; }
  public void setTax(BigDecimal tax) { this.tax = tax; }
  public BigDecimal getFees() { return fees; }
  public void setFees(BigDecimal fees) { this.fees = fees; }
  public BigDecimal getTotal() { return total; }
  public void setTotal(BigDecimal total) { this.total = total; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public Employee getCreatedBy() { return createdBy; }
  public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
  public Employee getSubmittedBy() { return submittedBy; }
  public void setSubmittedBy(Employee submittedBy) { this.submittedBy = submittedBy; }
  public String getSubmittedByNameSnapshot() { return submittedByNameSnapshot; }
  public void setSubmittedByNameSnapshot(String submittedByNameSnapshot) { this.submittedByNameSnapshot = submittedByNameSnapshot; }
  public Employee getApprovedBy() { return approvedBy; }
  public void setApprovedBy(Employee approvedBy) { this.approvedBy = approvedBy; }
  public String getApprovedByNameSnapshot() { return approvedByNameSnapshot; }
  public void setApprovedByNameSnapshot(String approvedByNameSnapshot) { this.approvedByNameSnapshot = approvedByNameSnapshot; }
  public Employee getRejectedBy() { return rejectedBy; }
  public void setRejectedBy(Employee rejectedBy) { this.rejectedBy = rejectedBy; }
  public String getRejectedByNameSnapshot() { return rejectedByNameSnapshot; }
  public void setRejectedByNameSnapshot(String rejectedByNameSnapshot) { this.rejectedByNameSnapshot = rejectedByNameSnapshot; }
  public Employee getOrderedBy() { return orderedBy; }
  public void setOrderedBy(Employee orderedBy) { this.orderedBy = orderedBy; }
  public Employee getReceivedBy() { return receivedBy; }
  public void setReceivedBy(Employee receivedBy) { this.receivedBy = receivedBy; }
  public String getRejectionReason() { return rejectionReason; }
  public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
  public String getVendorConfirmationNumber() { return vendorConfirmationNumber; }
  public void setVendorConfirmationNumber(String vendorConfirmationNumber) { this.vendorConfirmationNumber = vendorConfirmationNumber; }
  public String getExternalOrderNotes() { return externalOrderNotes; }
  public void setExternalOrderNotes(String externalOrderNotes) { this.externalOrderNotes = externalOrderNotes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public Instant getOrderedAt() { return orderedAt; }
  public void setOrderedAt(Instant orderedAt) { this.orderedAt = orderedAt; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
  public Instant getRejectedAt() { return rejectedAt; }
  public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
}
