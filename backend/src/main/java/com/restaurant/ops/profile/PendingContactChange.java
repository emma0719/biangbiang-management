package com.restaurant.ops.profile;

import com.restaurant.ops.employee.Employee;
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

@Entity
@Table(name = "pending_contact_changes")
public class PendingContactChange {
  public enum Type { EMAIL, PHONE }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Type type;
  @Column(nullable = false)
  private String normalizedValue;
  @Column(nullable = false, unique = true)
  private String tokenHash;
  @Column(nullable = false)
  private Instant createdAt;
  @Column(nullable = false)
  private Instant expiresAt;
  private Instant verifiedAt;
  private Instant revokedAt;

  @PrePersist
  void prePersist() { createdAt = Instant.now(); }

  public Employee getEmployee() { return employee; }
  public void setEmployee(Employee employee) { this.employee = employee; }
  public Type getType() { return type; }
  public void setType(Type type) { this.type = type; }
  public String getNormalizedValue() { return normalizedValue; }
  public void setNormalizedValue(String normalizedValue) { this.normalizedValue = normalizedValue; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public Instant getVerifiedAt() { return verifiedAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public void verify() { verifiedAt = Instant.now(); }
  public void revoke() { revokedAt = Instant.now(); }
}
