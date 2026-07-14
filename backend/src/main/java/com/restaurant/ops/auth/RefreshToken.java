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
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id")
  private Employee employee;

  @Column(nullable = false, unique = true)
  private String tokenHash;

  @Column(nullable = false)
  private String familyId;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant revokedAt;
  private String replacedByTokenHash;
  private Instant reusedAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Employee getEmployee() {
    return employee;
  }

  public void setEmployee(Employee employee) {
    this.employee = employee;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public String getFamilyId() {
    return familyId;
  }

  public void setFamilyId(String familyId) {
    this.familyId = familyId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void revoke() {
    revokedAt = Instant.now();
  }

  public String getReplacedByTokenHash() {
    return replacedByTokenHash;
  }

  public void setReplacedByTokenHash(String replacedByTokenHash) {
    this.replacedByTokenHash = replacedByTokenHash;
  }

  public Instant getReusedAt() {
    return reusedAt;
  }

  public void markReused() {
    reusedAt = Instant.now();
  }
}
