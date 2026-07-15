package com.restaurant.ops.coverage;

import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.coverage.CoverageEnums.CoverageType;
import com.restaurant.ops.coverage.CoverageEnums.ShiftType;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "coverage_requests")
public class CoverageRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StoreCode store;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "requested_by_employee_id")
  private Employee requestedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "replacement_employee_id")
  private Employee replacementEmployee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CoverageType coverageType;

  @Column(nullable = false)
  private LocalDate shiftDate;

  private LocalTime startTime;
  private LocalTime endTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ShiftType shiftType;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Position position;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @Column(columnDefinition = "TEXT")
  private String managerNote;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CoverageStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_employee_id")
  private Employee approvedBy;

  private Instant approvedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by_employee_id")
  private Employee reviewedBy;

  private Instant reviewedAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

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
  public StoreCode getStore() { return store; }
  public void setStore(StoreCode store) { this.store = store; }
  public Employee getRequestedBy() { return requestedBy; }
  public void setRequestedBy(Employee requestedBy) { this.requestedBy = requestedBy; }
  public Employee getReplacementEmployee() { return replacementEmployee; }
  public void setReplacementEmployee(Employee replacementEmployee) { this.replacementEmployee = replacementEmployee; }
  public CoverageType getCoverageType() { return coverageType; }
  public void setCoverageType(CoverageType coverageType) { this.coverageType = coverageType; }
  public LocalDate getShiftDate() { return shiftDate; }
  public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
  public LocalTime getStartTime() { return startTime; }
  public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
  public LocalTime getEndTime() { return endTime; }
  public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
  public ShiftType getShiftType() { return shiftType; }
  public void setShiftType(ShiftType shiftType) { this.shiftType = shiftType; }
  public Position getPosition() { return position; }
  public void setPosition(Position position) { this.position = position; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public String getManagerNote() { return managerNote; }
  public void setManagerNote(String managerNote) { this.managerNote = managerNote; }
  public CoverageStatus getStatus() { return status; }
  public void setStatus(CoverageStatus status) { this.status = status; }
  public Employee getApprovedBy() { return approvedBy; }
  public void setApprovedBy(Employee approvedBy) { this.approvedBy = approvedBy; }
  public Instant getApprovedAt() { return approvedAt; }
  public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
  public Employee getReviewedBy() { return reviewedBy; }
  public void setReviewedBy(Employee reviewedBy) { this.reviewedBy = reviewedBy; }
  public Instant getReviewedAt() { return reviewedAt; }
  public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public Long getVersion() { return version; }
}
