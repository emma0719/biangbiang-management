package com.restaurant.ops.auth;

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
@Table(name = "password_reset_requests")
public class PasswordResetRequest {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;
  @Column(nullable = false)
  private String deliveryMethod;
  @Column(nullable = false, unique = true)
  private String tokenHash;
  @Column(nullable = false)
  private Instant createdAt;
  @Column(nullable = false)
  private Instant expiresAt;
  private Instant usedAt;
  private Instant revokedAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Employee getEmployee() { return employee; }
  public void setEmployee(Employee employee) { this.employee = employee; }
  public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getUsedAt() { return usedAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void use() { usedAt = Instant.now(); }
  public void revoke() { revokedAt = Instant.now(); }
}
