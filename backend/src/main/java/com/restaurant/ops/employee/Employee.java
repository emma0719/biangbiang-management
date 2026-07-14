package com.restaurant.ops.employee;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String englishName;

  @Column(nullable = false)
  private String preferredName;

  @Column(nullable = false, unique = true)
  private String normalizedEmail;

  @Column(nullable = false, unique = true)
  private String normalizedPhone;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false, unique = true)
  private String toastPinHash;

  @Column(nullable = false)
  private String toastPinCiphertext;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private StoreCode homeStore;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EmployeeStatus status = EmployeeStatus.ACTIVE;

  private String profilePhotoKey;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "employee_positions", joinColumns = @JoinColumn(name = "employee_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "position")
  private Set<Position> positions = EnumSet.noneOf(Position.class);

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "employee_eligible_stores", joinColumns = @JoinColumn(name = "employee_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "store")
  private Set<StoreCode> eligibleStores = EnumSet.noneOf(StoreCode.class);

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  private Instant lastLoginAt;

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

  public Long getId() {
    return id;
  }

  public String getEnglishName() {
    return englishName;
  }

  public void setEnglishName(String englishName) {
    this.englishName = englishName;
  }

  public String getPreferredName() {
    return preferredName;
  }

  public void setPreferredName(String preferredName) {
    this.preferredName = preferredName;
  }

  public String getDisplayName() {
    return preferredName == null || preferredName.isBlank() ? englishName : preferredName;
  }

  public String getNormalizedEmail() {
    return normalizedEmail;
  }

  public void setNormalizedEmail(String normalizedEmail) {
    this.normalizedEmail = normalizedEmail;
  }

  public String getNormalizedPhone() {
    return normalizedPhone;
  }

  public void setNormalizedPhone(String normalizedPhone) {
    this.normalizedPhone = normalizedPhone;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getToastPinHash() {
    return toastPinHash;
  }

  public void setToastPinHash(String toastPinHash) {
    this.toastPinHash = toastPinHash;
  }

  public String getToastPinCiphertext() {
    return toastPinCiphertext;
  }

  public void setToastPinCiphertext(String toastPinCiphertext) {
    this.toastPinCiphertext = toastPinCiphertext;
  }

  public StoreCode getHomeStore() {
    return homeStore;
  }

  public void setHomeStore(StoreCode homeStore) {
    this.homeStore = homeStore;
  }

  public EmployeeStatus getStatus() {
    return status;
  }

  public void setStatus(EmployeeStatus status) {
    this.status = status;
  }

  public String getProfilePhotoKey() {
    return profilePhotoKey;
  }

  public void setProfilePhotoKey(String profilePhotoKey) {
    this.profilePhotoKey = profilePhotoKey;
  }

  public Set<Position> getPositions() {
    return positions;
  }

  public void setPositions(Set<Position> positions) {
    this.positions = Position.normalize(positions);
  }

  public void assignPosition(Position position) {
    this.positions = Position.normalizeAfterAssigning(this.positions, position);
  }

  public Set<StoreCode> getEligibleStores() {
    return eligibleStores;
  }

  public void setEligibleStores(Set<StoreCode> eligibleStores) {
    this.eligibleStores = eligibleStores.isEmpty() ? EnumSet.noneOf(StoreCode.class) : EnumSet.copyOf(eligibleStores);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }
}
