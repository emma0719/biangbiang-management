package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "order_plan_sessions")
public class OrderPlanSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_code", nullable = false)
  private StoreCode locationCode;

  @ManyToOne(optional = false)
  @JoinColumn(name = "source_inventory_session_id")
  private InventoryCountSession sourceInventorySession;

  @Column(nullable = false)
  private LocalDate inventoryBusinessDate;

  @ManyToOne
  @JoinColumn(name = "inventory_completed_by_employee_id")
  private Employee inventoryCompletedBy;

  private String inventoryCompletedByNameSnapshot;

  private Instant inventoryCompletedAt;

  @Column(nullable = false)
  private LocalDate businessDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderPlanStatus status = OrderPlanStatus.DRAFT;

  @ManyToOne(optional = false)
  @JoinColumn(name = "created_by_employee_id")
  private Employee createdBy;

  @ManyToOne
  @JoinColumn(name = "assigned_orderer_employee_id")
  private Employee assignedOrderer;

  private LocalDate dueDate;
  private LocalTime dueTime;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant startedAt;

  @ManyToOne
  @JoinColumn(name = "submitted_by_employee_id")
  private Employee submittedBy;

  private String submittedByNameSnapshot;

  private Instant submittedAt;

  @ManyToOne
  @JoinColumn(name = "completed_by_employee_id")
  private Employee completedBy;

  private String completedByNameSnapshot;

  private Instant completedAt;

  @ManyToOne
  @JoinColumn(name = "rejected_by_employee_id")
  private Employee rejectedBy;

  private String rejectedByNameSnapshot;

  private Instant rejectedAt;

  @Column(columnDefinition = "TEXT")
  private String rejectionReason;

  private Instant cancelledAt;

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
  public InventoryCountSession getSourceInventorySession() { return sourceInventorySession; }
  public void setSourceInventorySession(InventoryCountSession sourceInventorySession) { this.sourceInventorySession = sourceInventorySession; }
  public LocalDate getInventoryBusinessDate() { return inventoryBusinessDate; }
  public void setInventoryBusinessDate(LocalDate inventoryBusinessDate) { this.inventoryBusinessDate = inventoryBusinessDate; }
  public Employee getInventoryCompletedBy() { return inventoryCompletedBy; }
  public void setInventoryCompletedBy(Employee inventoryCompletedBy) { this.inventoryCompletedBy = inventoryCompletedBy; }
  public String getInventoryCompletedByNameSnapshot() { return inventoryCompletedByNameSnapshot; }
  public void setInventoryCompletedByNameSnapshot(String inventoryCompletedByNameSnapshot) { this.inventoryCompletedByNameSnapshot = inventoryCompletedByNameSnapshot; }
  public Instant getInventoryCompletedAt() { return inventoryCompletedAt; }
  public void setInventoryCompletedAt(Instant inventoryCompletedAt) { this.inventoryCompletedAt = inventoryCompletedAt; }
  public LocalDate getBusinessDate() { return businessDate; }
  public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
  public OrderPlanStatus getStatus() { return status; }
  public void setStatus(OrderPlanStatus status) { this.status = status; }
  public Employee getCreatedBy() { return createdBy; }
  public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
  public Employee getAssignedOrderer() { return assignedOrderer; }
  public void setAssignedOrderer(Employee assignedOrderer) { this.assignedOrderer = assignedOrderer; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
  public LocalTime getDueTime() { return dueTime; }
  public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Employee getSubmittedBy() { return submittedBy; }
  public void setSubmittedBy(Employee submittedBy) { this.submittedBy = submittedBy; }
  public String getSubmittedByNameSnapshot() { return submittedByNameSnapshot; }
  public void setSubmittedByNameSnapshot(String submittedByNameSnapshot) { this.submittedByNameSnapshot = submittedByNameSnapshot; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
  public Employee getCompletedBy() { return completedBy; }
  public void setCompletedBy(Employee completedBy) { this.completedBy = completedBy; }
  public String getCompletedByNameSnapshot() { return completedByNameSnapshot; }
  public void setCompletedByNameSnapshot(String completedByNameSnapshot) { this.completedByNameSnapshot = completedByNameSnapshot; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public Employee getRejectedBy() { return rejectedBy; }
  public void setRejectedBy(Employee rejectedBy) { this.rejectedBy = rejectedBy; }
  public String getRejectedByNameSnapshot() { return rejectedByNameSnapshot; }
  public void setRejectedByNameSnapshot(String rejectedByNameSnapshot) { this.rejectedByNameSnapshot = rejectedByNameSnapshot; }
  public Instant getRejectedAt() { return rejectedAt; }
  public void setRejectedAt(Instant rejectedAt) { this.rejectedAt = rejectedAt; }
  public String getRejectionReason() { return rejectionReason; }
  public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
  public Instant getCancelledAt() { return cancelledAt; }
  public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
}
