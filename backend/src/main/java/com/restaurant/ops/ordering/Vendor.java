package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.StoreCode;
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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "vendors")
public class Vendor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "location_code", nullable = false)
  private StoreCode locationCode;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String normalizedName;

  private String vendorCode;
  private String sourceKey;
  private String sourceVersion;
  @Column(nullable = false)
  private boolean importedFromReference;
  private String contactName;
  private String phone;
  private String email;
  private String orderingMethod;
  private LocalTime orderDeadline;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private int displayOrder;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "vendor_delivery_days", joinColumns = @JoinColumn(name = "vendor_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "day_of_week")
  private Set<DayOfWeek> deliveryDays = EnumSet.noneOf(DayOfWeek.class);

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
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getNormalizedName() { return normalizedName; }
  public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
  public String getVendorCode() { return vendorCode; }
  public void setVendorCode(String vendorCode) { this.vendorCode = vendorCode; }
  public String getSourceKey() { return sourceKey; }
  public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }
  public String getSourceVersion() { return sourceVersion; }
  public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
  public boolean isImportedFromReference() { return importedFromReference; }
  public void setImportedFromReference(boolean importedFromReference) { this.importedFromReference = importedFromReference; }
  public String getContactName() { return contactName; }
  public void setContactName(String contactName) { this.contactName = contactName; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public String getOrderingMethod() { return orderingMethod; }
  public void setOrderingMethod(String orderingMethod) { this.orderingMethod = orderingMethod; }
  public LocalTime getOrderDeadline() { return orderDeadline; }
  public void setOrderDeadline(LocalTime orderDeadline) { this.orderDeadline = orderDeadline; }
  public String getNotes() { return notes; }
  public void setNotes(String notes) { this.notes = notes; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
  public Set<DayOfWeek> getDeliveryDays() { return deliveryDays; }
  public void setDeliveryDays(Set<DayOfWeek> deliveryDays) { this.deliveryDays = deliveryDays == null || deliveryDays.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(deliveryDays); }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
