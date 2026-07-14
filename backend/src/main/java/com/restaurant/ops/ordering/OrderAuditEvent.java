package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_audit_events")
public class OrderAuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String entityType;

  @Column(nullable = false)
  private Long entityId;

  @Column(nullable = false)
  private String action;

  @ManyToOne(optional = false)
  @JoinColumn(name = "actor_employee_id")
  private Employee actor;

  private String actorNameSnapshot;

  @Column(columnDefinition = "TEXT")
  private String oldValue;

  @Column(columnDefinition = "TEXT")
  private String newValue;

  @Column(columnDefinition = "TEXT")
  private String reason;

  @ManyToOne
  @JoinColumn(name = "pdf_document_id")
  private PurchaseOrderPdfDocument pdfDocument;

  private Integer pdfVersion;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public Long getEntityId() { return entityId; }
  public void setEntityId(Long entityId) { this.entityId = entityId; }
  public String getAction() { return action; }
  public void setAction(String action) { this.action = action; }
  public Employee getActor() { return actor; }
  public void setActor(Employee actor) { this.actor = actor; }
  public String getActorNameSnapshot() { return actorNameSnapshot; }
  public void setActorNameSnapshot(String actorNameSnapshot) { this.actorNameSnapshot = actorNameSnapshot; }
  public String getOldValue() { return oldValue; }
  public void setOldValue(String oldValue) { this.oldValue = oldValue; }
  public String getNewValue() { return newValue; }
  public void setNewValue(String newValue) { this.newValue = newValue; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public PurchaseOrderPdfDocument getPdfDocument() { return pdfDocument; }
  public void setPdfDocument(PurchaseOrderPdfDocument pdfDocument) { this.pdfDocument = pdfDocument; }
  public Integer getPdfVersion() { return pdfVersion; }
  public void setPdfVersion(Integer pdfVersion) { this.pdfVersion = pdfVersion; }
  public Instant getCreatedAt() { return createdAt; }
}
