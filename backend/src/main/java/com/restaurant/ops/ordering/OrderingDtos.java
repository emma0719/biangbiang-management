package com.restaurant.ops.ordering;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.InventoryCountStatus;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

public final class OrderingDtos {
  private OrderingDtos() {}

  public record VendorResponse(Long id, StoreCode locationCode, String name, String normalizedName, String vendorCode, String contactName, String phone, String email, String orderingMethod, Set<DayOfWeek> deliveryDays, LocalTime orderDeadline, String notes, boolean active, int displayOrder, Instant createdAt, Instant updatedAt) {}
  public record VendorRequest(@NotNull StoreCode locationCode, @NotBlank String name, String vendorCode, String contactName, String phone, String email, String orderingMethod, Set<DayOfWeek> deliveryDays, LocalTime orderDeadline, String notes, Boolean active, Integer displayOrder) {}

  public record ProductResponse(Long id, StoreCode locationCode, Long vendorId, String vendorName, String vendorProductCode, String name, String normalizedName, String chineseDisplayName, CatalogCategory category, InventoryBusiness inventoryBusiness, OrderBusiness orderBusiness, String orderBusinessName, String storageArea, CatalogUnit inventoryUnit, String inventoryUnitLabel, CatalogUnit orderUnit, String orderUnitLabel, String packageSpecification, BigDecimal unitPrice, String currency, BigDecimal parLevel, BigDecimal reorderPoint, BigDecimal defaultOrderQuantity, boolean active, int displayOrder, String notes) {}
  public record ProductRequest(@NotNull StoreCode locationCode, @NotNull Long vendorId, String vendorProductCode, @NotBlank String name, String chineseDisplayName, @NotNull CatalogCategory category, InventoryBusiness inventoryBusiness, OrderBusiness orderBusiness, String storageArea, @NotNull CatalogUnit inventoryUnit, String inventoryUnitLabel, @NotNull CatalogUnit orderUnit, String orderUnitLabel, String packageSpecification, @DecimalMin("0.00") BigDecimal unitPrice, String currency, @DecimalMin("0.000") BigDecimal parLevel, @DecimalMin("0.000") BigDecimal reorderPoint, @DecimalMin("0.000") BigDecimal defaultOrderQuantity, Boolean active, Integer displayOrder, String notes) {
    public ProductRequest(StoreCode locationCode, Long vendorId, String vendorProductCode, String name, String chineseDisplayName, CatalogCategory category, String storageArea, CatalogUnit inventoryUnit, String inventoryUnitLabel, CatalogUnit orderUnit, String orderUnitLabel, String packageSpecification, BigDecimal unitPrice, String currency, BigDecimal parLevel, BigDecimal reorderPoint, BigDecimal defaultOrderQuantity, Boolean active, Integer displayOrder, String notes) {
      this(locationCode, vendorId, vendorProductCode, name, chineseDisplayName, category, InventoryBusiness.BIANGBIANG_FRONT, OrderBusiness.BIANGBIANG_FRONT, storageArea, inventoryUnit, inventoryUnitLabel, orderUnit, orderUnitLabel, packageSpecification, unitPrice, currency, parLevel, reorderPoint, defaultOrderQuantity, active, displayOrder, notes);
    }
  }
  public record CreateInventoryCatalogItemRequest(@NotNull StoreCode locationCode, @NotNull InventoryBusiness inventoryBusiness, @NotBlank String name, @NotBlank String unit, String vendorName) {}
  public record UpdateInventoryCatalogItemRequest(@NotBlank String name, @NotBlank String unit, String vendorName, Boolean active) {}
  public record CreateOrderCatalogItemRequest(@NotNull StoreCode locationCode, @NotNull OrderBusiness orderBusiness, @NotBlank String name, @NotBlank String unit, String vendorName) {}
  public record UpdateOrderCatalogItemRequest(@NotBlank String name, @NotBlank String unit, String vendorName, Boolean active) {}

  public record InventorySessionResponse(Long id, StoreCode locationCode, InventoryBusiness inventoryBusiness, String inventoryBusinessName, LocalDate businessDate, InventoryCountStatus status, Long createdByEmployeeId, Long assignedCounterEmployeeId, LocalDate dueDate, LocalTime dueTime, Instant startedAt, Long completedByEmployeeId, String completedByNameSnapshot, Instant completedAt, Long submittedByEmployeeId, String submittedByNameSnapshot, Instant submittedAt, Long reviewedByEmployeeId, String reviewedByNameSnapshot, Instant reviewedAt, Long lockedByEmployeeId, String lockedByNameSnapshot, Instant lockedAt, String overrideReason, Instant cancelledAt, String notes, Instant createdAt, Instant updatedAt, List<InventoryLineResponse> lines) {
    @JsonProperty("submittedByName")
    public String submittedByName() {
      return submittedByNameSnapshot;
    }
  }
  public record InventoryLineResponse(Long id, Long productId, Long vendorId, String vendorName, String productCode, String productName, String packageSpecification, CatalogUnit inventoryUnit, BigDecimal previousCount, BigDecimal quantityOnHand, CatalogUnit unit, String notes, Long updatedByEmployeeId, Instant updatedAt) {}
  public record CreateInventorySessionRequest(@NotNull StoreCode locationCode, @NotNull InventoryBusiness inventoryBusiness, @NotNull LocalDate businessDate, Long assignedCounterEmployeeId, LocalDate dueDate, LocalTime dueTime, String notes) {
    public CreateInventorySessionRequest(StoreCode locationCode, LocalDate businessDate, Long assignedCounterEmployeeId, LocalDate dueDate, LocalTime dueTime, String notes) {
      this(locationCode, InventoryBusiness.BIANGBIANG_FRONT, businessDate, assignedCounterEmployeeId, dueDate, dueTime, notes);
    }

    public CreateInventorySessionRequest(StoreCode locationCode, LocalDate businessDate, String notes) {
      this(locationCode, InventoryBusiness.BIANGBIANG_FRONT, businessDate, null, null, null, notes);
    }
  }
  public record UpsertInventoryLineRequest(@NotNull Long productId, @NotNull @DecimalMin("0.000") BigDecimal quantityOnHand, CatalogUnit unit, String notes) {}
  public record UpsertInventoryLinesRequest(@NotEmpty List<UpsertInventoryLineRequest> lines) {}
  public record InventoryTransitionRequest(String reason) {}

  public record PurchaseOrderResponse(Long id, StoreCode locationCode, OrderBusiness orderBusiness, String orderBusinessName, Long vendorId, String vendorName, Long sourceInventorySessionId, Long orderPlanSessionId, String orderNumber, LocalDate businessDate, LocalDate expectedDeliveryDate, PurchaseOrderStatus status, BigDecimal subtotal, BigDecimal tax, BigDecimal fees, BigDecimal total, String currency, Long createdByEmployeeId, Long submittedByEmployeeId, String submittedByNameSnapshot, Instant submittedAt, Long approvedByEmployeeId, String approvedByNameSnapshot, Instant approvedAt, Long rejectedByEmployeeId, String rejectedByNameSnapshot, Instant rejectedAt, Long orderedByEmployeeId, Long receivedByEmployeeId, String rejectionReason, String vendorConfirmationNumber, String externalOrderNotes, PdfMetadataResponse pdf, List<PurchaseOrderLineResponse> lines, List<AuditResponse> auditEvents) {}
  public record PurchaseOrderLineResponse(Long id, Long productId, String productNameSnapshot, String productCodeSnapshot, String packageSpecificationSnapshot, CatalogUnit orderUnitSnapshot, BigDecimal unitPriceSnapshot, BigDecimal sourceInventoryQuantitySnapshot, BigDecimal suggestedOrderQuantity, BigDecimal finalOrderQuantity, BigDecimal currentInventoryQuantity, BigDecimal requestedQuantity, BigDecimal approvedQuantity, BigDecimal receivedQuantity, BigDecimal lineTotal, String notes) {}
  public record CreatePurchaseOrderRequest(@NotNull StoreCode locationCode, @NotNull Long vendorId, Long sourceInventorySessionId, @NotNull LocalDate businessDate, LocalDate expectedDeliveryDate, String externalOrderNotes, OrderBusiness orderBusiness) {
    public CreatePurchaseOrderRequest(StoreCode locationCode, Long vendorId, Long sourceInventorySessionId, LocalDate businessDate, LocalDate expectedDeliveryDate, String externalOrderNotes) {
      this(locationCode, vendorId, sourceInventorySessionId, businessDate, expectedDeliveryDate, externalOrderNotes, OrderBusiness.BIANGBIANG_FRONT);
    }
  }
  public record UpdatePurchaseOrderRequest(LocalDate expectedDeliveryDate, @DecimalMin("0.00") BigDecimal tax, @DecimalMin("0.00") BigDecimal fees, String externalOrderNotes) {}
  public record UpsertPurchaseOrderLineRequest(@NotNull Long productId, @DecimalMin("0.000") BigDecimal currentInventoryQuantity, @NotNull @DecimalMin("0.000") BigDecimal requestedQuantity, @DecimalMin("0.000") BigDecimal approvedQuantity, String notes) {}
  public record UpsertPurchaseOrderLinesRequest(@NotEmpty List<UpsertPurchaseOrderLineRequest> lines) {}
  public record RejectOrderRequest(@NotBlank String reason) {}
  public record MarkOrderedRequest(String vendorConfirmationNumber, String externalOrderNotes) {}
  public record ReceiveLineRequest(@NotNull Long lineId, @NotNull @DecimalMin("0.000") BigDecimal quantityReceivedNow, Boolean allowOverReceive, String note) {}
  public record ReceiveOrderRequest(@NotEmpty List<ReceiveLineRequest> lines) {}

  public record CreateOrderPlanRequest(@NotNull StoreCode locationCode, @NotNull Long sourceInventorySessionId, @NotNull LocalDate businessDate, Long assignedOrdererEmployeeId, LocalDate dueDate, LocalTime dueTime, String notes) {}
  public record UpsertOrderPlanLineRequest(@NotNull Long productId, @DecimalMin("0.000") BigDecimal finalOrderQuantity, String notes) {}
  public record UpsertOrderPlanLinesRequest(@NotEmpty List<UpsertOrderPlanLineRequest> lines) {}
  public record OrderPlanResponse(Long id, StoreCode locationCode, Long sourceInventorySessionId, LocalDate inventoryBusinessDate, Long inventoryCompletedByEmployeeId, String inventoryCompletedByNameSnapshot, Instant inventoryCompletedAt, LocalDate businessDate, OrderPlanStatus status, Long createdByEmployeeId, Long assignedOrdererEmployeeId, LocalDate dueDate, LocalTime dueTime, String notes, Instant startedAt, Long submittedByEmployeeId, String submittedByNameSnapshot, Instant submittedAt, Long completedByEmployeeId, String completedByNameSnapshot, Instant completedAt, Long rejectedByEmployeeId, String rejectedByNameSnapshot, Instant rejectedAt, String rejectionReason, Instant cancelledAt, List<OrderPlanLineResponse> lines, List<PurchaseOrderResponse> vendorOrders, List<AuditResponse> auditEvents) {
    @JsonProperty("submittedByName")
    public String submittedByName() {
      return submittedByNameSnapshot;
    }
  }
  public record OrderPlanLineResponse(Long id, Long productId, Long vendorId, String vendorNameSnapshot, String productCodeSnapshot, String productNameSnapshot, String packageSpecificationSnapshot, CatalogUnit inventoryUnitSnapshot, CatalogUnit orderUnitSnapshot, BigDecimal unitPriceSnapshot, BigDecimal sourceInventoryQuantity, BigDecimal parLevelSnapshot, BigDecimal reorderPointSnapshot, BigDecimal previousOrderQuantity, BigDecimal suggestedOrderQuantity, BigDecimal finalOrderQuantity, String notes) {}

  public record PriceHistoryResponse(Long id, Long productId, BigDecimal unitPrice, LocalDate effectiveDate, Long purchaseOrderId, Long enteredByEmployeeId, Instant createdAt) {}
  public record AuditResponse(Long id, String entityType, Long entityId, String action, Long actorEmployeeId, String actorNameSnapshot, String oldValue, String newValue, String reason, Long pdfDocumentId, Integer pdfVersion, Instant createdAt) {}
  public record PdfMetadataResponse(Long documentId, Long purchaseOrderId, int versionNumber, String filename, String mimeType, Instant generatedAt, Long generatedByEmployeeId, String checksumSha256, boolean currentVersion) {}
  public record OrderPlanPdfMetadataResponse(Long documentId, Long orderPlanId, int version, String filename, String mimeType, long byteSize, String sha256, Instant generatedAt, Long generatedByEmployeeId, String generatedByNameSnapshot, boolean current) {}
  public record OrderInventoryReferenceResponse(Long inventoryCountId, StoreCode locationCode, OrderBusiness orderBusiness, String orderBusinessName, Instant countedAt, List<OrderInventoryReferenceLineResponse> lines) {}
  public record OrderInventoryReferenceLineResponse(Long inventoryProductId, String itemName, String normalizedItemName, String unit, BigDecimal quantity) {}
  public record CatalogImportReport(int importedVendors, int importedProducts, List<String> correctedValues, List<String> ambiguities, List<String> duplicateCandidates, List<String> interpretedPages) {}
}
