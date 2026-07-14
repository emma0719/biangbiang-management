package com.restaurant.ops.audit;

import com.restaurant.ops.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "toast_pin_audit_logs")
public class ToastPinAuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_id")
  private Employee actor;
  @Column(nullable = false)
  private String oldMaskedValue;
  @Column(nullable = false)
  private String newMaskedValue;
  @Column(nullable = false)
  private Instant changedAt;

  @PrePersist
  void prePersist() {
    changedAt = Instant.now();
  }

  public void setEmployee(Employee employee) { this.employee = employee; }
  public void setActor(Employee actor) { this.actor = actor; }
  public void setOldMaskedValue(String oldMaskedValue) { this.oldMaskedValue = oldMaskedValue; }
  public void setNewMaskedValue(String newMaskedValue) { this.newMaskedValue = newMaskedValue; }
}
