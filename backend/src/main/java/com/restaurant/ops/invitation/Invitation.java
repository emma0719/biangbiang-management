package com.restaurant.ops.invitation;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.Position;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "invitations")
public class Invitation {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creator_id")
  private Employee creator;

  @Column(nullable = false, unique = true)
  private String tokenHash;

  @Version
  @Column(nullable = false)
  private int tokenVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InvitationStatus status = InvitationStatus.ACTIVE;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "invitation_positions", joinColumns = @JoinColumn(name = "invitation_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "position")
  private Set<Position> positions = EnumSet.noneOf(Position.class);

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant expiresAt;

  private Instant usedAt;
  private Instant revokedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "activated_employee_id")
  private Employee activatedEmployee;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public Employee getCreator() {
    return creator;
  }

  public void setCreator(Employee creator) {
    this.creator = creator;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public int getTokenVersion() {
    return tokenVersion;
  }

  public void incrementTokenVersion() {
    tokenVersion++;
  }

  public InvitationStatus getStatus() {
    return status;
  }

  public void setStatus(InvitationStatus status) {
    this.status = status;
  }

  public Set<Position> getPositions() {
    return positions;
  }

  public void setPositions(Set<Position> positions) {
    this.positions = Position.normalize(positions);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void markUsed(Employee employee) {
    status = InvitationStatus.USED;
    usedAt = Instant.now();
    activatedEmployee = employee;
    tokenHash = "used:" + id + ":" + usedAt.toEpochMilli();
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void revoke() {
    status = InvitationStatus.REVOKED;
    revokedAt = Instant.now();
    tokenHash = "revoked:" + id + ":" + revokedAt.toEpochMilli();
  }

  public Employee getActivatedEmployee() {
    return activatedEmployee;
  }
}
