package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.InventoryCountStatus;
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
@Table(name = "inventory_count_sessions")
public class InventoryCountSession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_code", nullable = false)
  private StoreCode locationCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InventoryBusiness inventoryBusiness = InventoryBusiness.BIANGBIANG_FRONT;

  @Column(nullable = false)
  private LocalDate businessDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InventoryCountStatus status = InventoryCountStatus.DRAFT;

  @ManyToOne(optional = false)
  @JoinColumn(name = "created_by_employee_id")
  private Employee createdBy;

  @ManyToOne
  @JoinColumn(name = "assigned_counter_employee_id")
  private Employee assignedCounter;

  private LocalDate dueDate;

  private LocalTime dueTime;

  private Instant startedAt;

  @ManyToOne
  @JoinColumn(name = "completed_by_employee_id")
  private Employee completedBy;

  private String completedByNameSnapshot;

  private Instant completedAt;

  @ManyToOne
  @JoinColumn(name = "submitted_by_employee_id")
  private Employee submittedBy;

  private String submittedByNameSnapshot;

  private Instant submittedAt;

  @ManyToOne
  @JoinColumn(name = "reviewed_by_employee_id")
  private Employee reviewedBy;

  private String reviewedByNameSnapshot;

  private Instant reviewedAt;

  @ManyToOne
  @JoinColumn(name = "locked_by_employee_id")
  private Employee lockedBy;

  private String lockedByNameSnapshot;

  private Instant lockedAt;

  @Column(columnDefinition = "TEXT")
  private String overrideReason;

  private Instant cancelledAt;

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
  public InventoryBusiness getInventoryBusiness() { return inventoryBusiness; }
  public void setInventoryBusiness(InventoryBusiness inventoryBusiness) { this.inventoryBusiness = inventoryBusiness; }
  public LocalDate getBusinessDate() { return businessDate; }
  public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }
  public InventoryCountStatus getStatus() { return status; }
  public void setStatus(InventoryCountStatus status) { this.status = status; }
  public Employee getCreatedBy() { return createdBy; }
  public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
  public Employee getAssignedCounter() { return assignedCounter; }
  public void setAssignedCounter(Employee assignedCounter) { this.assignedCounter = assignedCounter; }
  public LocalDate getDueDate() { return dueDate; }
  public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
  public LocalTime getDueTime() { return dueTime; }
  public void setDueTime(LocalTime dueTime) { this.dueTime = dueTime; }
  public Instant getStartedAt() { return startedAt; }
  public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
  public Employee getCompletedBy() { return completedBy; }
  public void setCompletedBy(Employee completedBy) { this.completedBy = completedBy; }
  public String getCompletedByNameSnapshot() { return completedByNameSnapshot; }
  public void setCompletedByNameSnapshot(String completedByNameSnapshot) { this.completedByNameSnapshot = completedByNameSnapshot; }
  public Instant getCompletedAt() { return completedAt; }
  public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
  public Employee getSubmittedBy() { return submittedBy; }
  public void setSubmittedBy(Employee submittedBy) { this.submittedBy = submittedBy; }
  public String getSubmittedByNameSnapshot() { return submittedByNameSnapshot; }
  public void setSubmittedByNameSnapshot(String submittedByNameSnapshot) { this.submittedByNameSnapshot = submittedByNameSnapshot; }
  public Instant getSubmittedAt() { return submittedAt; }
  public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
  public Employee getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(Employee reviewedBy) { this.reviewedBy = reviewedBy; }
  public String getReviewedByNameSnapshot() { return reviewedByNameSnapshot; }
  public void setReviewedByNameSnapshot(String reviewedByNameSnapshot) { this.reviewedByNameSnapshot = reviewedByNameSnapshot; }
  public Instant getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
  public Employee getLockedBy() { return lockedBy; }
  public void setLockedBy(Employee lockedBy) { this.lockedBy = lockedBy; }
  public String getLockedByNameSnapshot() { return lockedByNameSnapshot; }
  public void setLockedByNameSnapshot(String lockedByNameSnapshot) { this.lockedByNameSnapshot = lockedByNameSnapshot; }
  public Instant getLockedAt() { return lockedAt; }
  public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
  public String getOverrideReason() { return overrideReason; }
  public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
  public Instant getCancelledAt() { return cancelledAt; }
  public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
