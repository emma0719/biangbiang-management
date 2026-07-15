package com.restaurant.ops.coverage;

import com.restaurant.ops.coverage.CoverageEnums.CoverageStatus;
import com.restaurant.ops.employee.Employee;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "coverage_request_audit_events")
public class CoverageAuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long requestId;

  @Column(nullable = false)
  private String action;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_employee_id")
  private Employee actor;

  private String actorNameSnapshot;
  private Long requestedByEmployeeId;
  private String requestedByNameSnapshot;
  private Long replacementEmployeeId;
  private String replacementNameSnapshot;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StoreCode store;

  @Column(nullable = false)
  private LocalDate shiftDate;

  @Enumerated(EnumType.STRING)
  private CoverageStatus oldStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CoverageStatus newStatus;

  @Column(columnDefinition = "TEXT")
  private String details;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public Long getRequestId() { return requestId; }
  public void setRequestId(Long requestId) { this.requestId = requestId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public Employee getActor() { return actor; }
  public void setActor(Employee actor) { this.actor = actor; }
  public String getActorNameSnapshot() { return actorNameSnapshot; }
  public void setActorNameSnapshot(String actorNameSnapshot) { this.actorNameSnapshot = actorNameSnapshot; }
  public Long getRequestedByEmployeeId() { return requestedByEmployeeId; }
  public void setRequestedByEmployeeId(Long requestedByEmployeeId) { this.requestedByEmployeeId = requestedByEmployeeId; }
  public String getRequestedByNameSnapshot() { return requestedByNameSnapshot; }
  public void setRequestedByNameSnapshot(String requestedByNameSnapshot) { this.requestedByNameSnapshot = requestedByNameSnapshot; }
  public Long getReplacementEmployeeId() { return replacementEmployeeId; }
  public void setReplacementEmployeeId(Long replacementEmployeeId) { this.replacementEmployeeId = replacementEmployeeId; }
  public String getReplacementNameSnapshot() { return replacementNameSnapshot; }
  public void setReplacementNameSnapshot(String replacementNameSnapshot) { this.replacementNameSnapshot = replacementNameSnapshot; }
  public StoreCode getStore() { return store; }
  public void setStore(StoreCode store) { this.store = store; }
  public LocalDate getShiftDate() { return shiftDate; }
  public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
  public CoverageStatus getOldStatus() { return oldStatus; }
  public void setOldStatus(CoverageStatus oldStatus) { this.oldStatus = oldStatus; }
  public CoverageStatus getNewStatus() { return newStatus; }
  public void setNewStatus(CoverageStatus newStatus) { this.newStatus = newStatus; }
  public String getDetails() { return details; }
  public void setDetails(String details) { this.details = details; }
  public Instant getCreatedAt() { return createdAt; }
}
