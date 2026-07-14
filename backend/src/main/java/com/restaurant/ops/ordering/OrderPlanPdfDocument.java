package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "order_plan_pdf_documents")
public class OrderPlanPdfDocument {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "order_plan_id")
  private OrderPlanSession orderPlan;

  @Column(nullable = false)
  private int versionNumber;

  @Column(nullable = false)
  private String filename;

  @Column(nullable = false)
  private String mimeType = "application/pdf";

  @Column(nullable = false)
  private Instant generatedAt;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "generated_by_employee_id")
  private Employee generatedBy;

  @Column(nullable = false)
  private String generatedByNameSnapshot;

  @Column(nullable = false)
  private long byteSize;

  @Column(nullable = false)
  private String checksumSha256;

  @Lob
  @Column(nullable = false, columnDefinition = "LONGBLOB")
  private byte[] content;

  @Column(nullable = false)
  private boolean currentVersion = true;

  private Instant supersededAt;

  public Long getId() { return id; }
  public OrderPlanSession getOrderPlan() { return orderPlan; }
  public void setOrderPlan(OrderPlanSession orderPlan) { this.orderPlan = orderPlan; }
  public int getVersionNumber() { return versionNumber; }
  public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }
  public String getFilename() { return filename; }
  public void setFilename(String filename) { this.filename = filename; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }
  public Instant getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
  public Employee getGeneratedBy() { return generatedBy; }
  public void setGeneratedBy(Employee generatedBy) { this.generatedBy = generatedBy; }
  public String getGeneratedByNameSnapshot() { return generatedByNameSnapshot; }
  public void setGeneratedByNameSnapshot(String generatedByNameSnapshot) { this.generatedByNameSnapshot = generatedByNameSnapshot; }
  public long getByteSize() { return byteSize; }
  public void setByteSize(long byteSize) { this.byteSize = byteSize; }
  public String getChecksumSha256() { return checksumSha256; }
  public void setChecksumSha256(String checksumSha256) { this.checksumSha256 = checksumSha256; }
  public byte[] getContent() { return content; }
  public void setContent(byte[] content) { this.content = content; }
  public boolean isCurrentVersion() { return currentVersion; }
  public void setCurrentVersion(boolean currentVersion) { this.currentVersion = currentVersion; }
  public Instant getSupersededAt() { return supersededAt; }
  public void setSupersededAt(Instant supersededAt) { this.supersededAt = supersededAt; }
}
