package com.restaurant.ops.ordering;

import com.restaurant.ops.ordering.OrderingDtos.AuditResponse;
import com.restaurant.ops.ordering.OrderingDtos.InventoryLineResponse;
import com.restaurant.ops.ordering.OrderingDtos.InventorySessionResponse;
import com.restaurant.ops.ordering.OrderingDtos.OrderPlanLineResponse;
import com.restaurant.ops.ordering.OrderingDtos.OrderPlanPdfMetadataResponse;
import com.restaurant.ops.ordering.OrderingDtos.OrderPlanResponse;
import com.restaurant.ops.ordering.OrderingDtos.PriceHistoryResponse;
import com.restaurant.ops.ordering.OrderingDtos.PdfMetadataResponse;
import com.restaurant.ops.ordering.OrderingDtos.ProductResponse;
import com.restaurant.ops.ordering.OrderingDtos.PurchaseOrderLineResponse;
import com.restaurant.ops.ordering.OrderingDtos.PurchaseOrderResponse;
import com.restaurant.ops.ordering.OrderingDtos.VendorResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OrderingMapper {
  private final PurchaseOrderPdfDocumentRepository pdfDocuments;

  OrderingMapper(PurchaseOrderPdfDocumentRepository pdfDocuments) {
    this.pdfDocuments = pdfDocuments;
  }

  VendorResponse vendor(Vendor vendor) {
    return new VendorResponse(vendor.getId(), vendor.getLocationCode(), vendor.getName(), vendor.getNormalizedName(), vendor.getVendorCode(), vendor.getContactName(), vendor.getPhone(), vendor.getEmail(), vendor.getOrderingMethod(), vendor.getDeliveryDays(), vendor.getOrderDeadline(), vendor.getNotes(), vendor.isActive(), vendor.getDisplayOrder(), vendor.getCreatedAt(), vendor.getUpdatedAt());
  }

  ProductResponse product(OrderCatalogProduct product) {
    return new ProductResponse(product.getId(), product.getLocationCode(), product.getVendor().getId(), product.getVendor().getName(), product.getVendorProductCode(), product.getName(), product.getNormalizedName(), product.getChineseDisplayName(), product.getCategory(), product.getInventoryBusiness(), product.getOrderBusiness(), product.getOrderBusiness() == null ? null : product.getOrderBusiness().displayName(), product.getStorageArea(), product.getInventoryUnit(), product.getInventoryUnitLabel(), product.getOrderUnit(), product.getOrderUnitLabel(), product.getPackageSpecification(), product.getUnitPrice(), product.getCurrency(), product.getParLevel(), product.getReorderPoint(), product.getDefaultOrderQuantity(), product.isActive(), product.getDisplayOrder(), product.getNotes());
  }

  InventorySessionResponse inventorySession(InventoryCountSession session, List<InventoryCountLine> lines) {
    Long completedBy = session.getCompletedBy() == null ? null : session.getCompletedBy().getId();
    return new InventorySessionResponse(
        session.getId(),
        session.getLocationCode(),
        session.getInventoryBusiness(),
        session.getInventoryBusiness().displayName(),
        session.getBusinessDate(),
        session.getStatus(),
        session.getCreatedBy().getId(),
        session.getAssignedCounter() == null ? null : session.getAssignedCounter().getId(),
        session.getDueDate(),
        session.getDueTime(),
        session.getStartedAt(),
        completedBy,
        session.getCompletedByNameSnapshot(),
        session.getCompletedAt(),
        session.getSubmittedBy() == null ? null : session.getSubmittedBy().getId(),
        session.getSubmittedByNameSnapshot(),
        session.getSubmittedAt(),
        session.getReviewedBy() == null ? null : session.getReviewedBy().getId(),
        session.getReviewedByNameSnapshot(),
        session.getReviewedAt(),
        session.getLockedBy() == null ? null : session.getLockedBy().getId(),
        session.getLockedByNameSnapshot(),
        session.getLockedAt(),
        session.getOverrideReason(),
        session.getCancelledAt(),
        session.getNotes(),
        session.getCreatedAt(),
        session.getUpdatedAt(),
        lines.stream().map(this::inventoryLine).toList()
    );
  }

  InventoryLineResponse inventoryLine(InventoryCountLine line) {
    var product = line.getProduct();
    return new InventoryLineResponse(line.getId(), product.getId(), product.getVendor().getId(), product.getVendor().getName(), product.getVendorProductCode(), product.getName(), product.getPackageSpecification(), product.getInventoryUnit(), null, line.getQuantityOnHand(), line.getUnit(), line.getNotes(), line.getUpdatedBy().getId(), line.getUpdatedAt());
  }

  PurchaseOrderResponse purchaseOrder(PurchaseOrder order, List<PurchaseOrderLine> lines, List<OrderAuditEvent> auditEvents) {
    return new PurchaseOrderResponse(
        order.getId(),
        order.getLocationCode(),
        order.getOrderBusiness(),
        order.getOrderBusiness().displayName(),
        order.getVendor().getId(),
        order.getVendor().getName(),
        order.getSourceInventorySession() == null ? null : order.getSourceInventorySession().getId(),
        order.getOrderPlanSession() == null ? null : order.getOrderPlanSession().getId(),
        order.getOrderNumber(),
        order.getBusinessDate(),
        order.getExpectedDeliveryDate(),
        order.getStatus(),
        order.getSubtotal(),
        order.getTax(),
        order.getFees(),
        order.getTotal(),
        order.getCurrency(),
        order.getCreatedBy().getId(),
        order.getSubmittedBy() == null ? null : order.getSubmittedBy().getId(),
        order.getSubmittedByNameSnapshot(),
        order.getSubmittedAt(),
        order.getApprovedBy() == null ? null : order.getApprovedBy().getId(),
        order.getApprovedByNameSnapshot(),
        order.getApprovedAt(),
        order.getRejectedBy() == null ? null : order.getRejectedBy().getId(),
        order.getRejectedByNameSnapshot(),
        order.getRejectedAt(),
        order.getOrderedBy() == null ? null : order.getOrderedBy().getId(),
        order.getReceivedBy() == null ? null : order.getReceivedBy().getId(),
        order.getRejectionReason(),
        order.getVendorConfirmationNumber(),
        order.getExternalOrderNotes(),
        pdfDocuments.findByPurchaseOrderIdAndCurrentVersionTrue(order.getId()).map(this::pdf).orElse(null),
        lines.stream().map(this::purchaseOrderLine).toList(),
        auditEvents.stream().map(this::audit).toList()
    );
  }

  PurchaseOrderLineResponse purchaseOrderLine(PurchaseOrderLine line) {
    return new PurchaseOrderLineResponse(line.getId(), line.getProduct().getId(), line.getProductNameSnapshot(), line.getProductCodeSnapshot(), line.getPackageSpecificationSnapshot(), line.getOrderUnitSnapshot(), line.getUnitPriceSnapshot(), line.getSourceInventoryQuantitySnapshot(), line.getSuggestedOrderQuantity(), line.getFinalOrderQuantity(), line.getCurrentInventoryQuantity(), line.getRequestedQuantity(), line.getApprovedQuantity(), line.getReceivedQuantity(), line.getLineTotal(), line.getNotes());
  }

  OrderPlanResponse orderPlan(OrderPlanSession plan, List<OrderPlanLine> lines, List<PurchaseOrder> vendorOrders, List<OrderAuditEvent> auditEvents) {
    return new OrderPlanResponse(
        plan.getId(),
        plan.getLocationCode(),
        plan.getOrderBusiness(),
        plan.getOrderBusiness().displayName(),
        plan.getSourceInventorySession() == null ? null : plan.getSourceInventorySession().getId(),
        plan.getInventoryBusinessDate(),
        plan.getInventoryCompletedBy() == null ? null : plan.getInventoryCompletedBy().getId(),
        plan.getInventoryCompletedByNameSnapshot(),
        plan.getInventoryCompletedAt(),
        plan.getBusinessDate(),
        plan.getStatus(),
        plan.getCreatedBy().getId(),
        plan.getAssignedOrderer() == null ? null : plan.getAssignedOrderer().getId(),
        plan.getDueDate(),
        plan.getDueTime(),
        plan.getNotes(),
        plan.getStartedAt(),
        plan.getSubmittedBy() == null ? null : plan.getSubmittedBy().getId(),
        plan.getSubmittedByNameSnapshot(),
        plan.getSubmittedAt(),
        plan.getLastModifiedBy() == null ? null : plan.getLastModifiedBy().getId(),
        plan.getLastModifiedByNameSnapshot(),
        plan.getUpdatedAt(),
        plan.getReviewedBy() == null ? null : plan.getReviewedBy().getId(),
        plan.getReviewedByNameSnapshot(),
        plan.getReviewedAt(),
        plan.getReviewNote(),
        plan.getCompletedBy() == null ? null : plan.getCompletedBy().getId(),
        plan.getCompletedByNameSnapshot(),
        plan.getCompletedAt(),
        plan.getRejectedBy() == null ? null : plan.getRejectedBy().getId(),
        plan.getRejectedByNameSnapshot(),
        plan.getRejectedAt(),
        plan.getRejectionReason(),
        plan.getCancelledAt(),
        lines.stream().map(this::orderPlanLine).toList(),
        vendorOrders.stream().map(order -> purchaseOrder(order, List.of(), List.of())).toList(),
        auditEvents.stream().map(this::audit).toList()
    );
  }

  OrderPlanLineResponse orderPlanLine(OrderPlanLine line) {
    return new OrderPlanLineResponse(
        line.getId(),
        line.getProduct().getId(),
        line.getVendor().getId(),
        line.getVendorNameSnapshot(),
        line.getProductCodeSnapshot(),
        line.getProductNameSnapshot(),
        line.getPackageSpecificationSnapshot(),
        line.getInventoryUnitSnapshot(),
        line.getOrderUnitSnapshot(),
        line.getUnitPriceSnapshot(),
        line.getSourceInventoryQuantity(),
        line.getParLevelSnapshot(),
        line.getReorderPointSnapshot(),
        line.getPreviousOrderQuantity(),
        line.getSuggestedOrderQuantity(),
        line.getFinalOrderQuantity(),
        line.getNotes()
    );
  }

  PriceHistoryResponse price(ProductPriceHistory history) {
    return new PriceHistoryResponse(history.getId(), history.getProduct().getId(), history.getUnitPrice(), history.getEffectiveDate(), history.getPurchaseOrder() == null ? null : history.getPurchaseOrder().getId(), history.getEnteredBy().getId(), history.getCreatedAt());
  }

  AuditResponse audit(OrderAuditEvent event) {
    return new AuditResponse(event.getId(), event.getEntityType(), event.getEntityId(), event.getAction(), event.getActor().getId(), event.getActorNameSnapshot(), event.getOldValue(), event.getNewValue(), event.getReason(), event.getPdfDocument() == null ? null : event.getPdfDocument().getId(), event.getPdfVersion(), event.getCreatedAt());
  }

  PdfMetadataResponse pdf(PurchaseOrderPdfDocument document) {
    return new PdfMetadataResponse(document.getId(), document.getPurchaseOrder().getId(), document.getVersionNumber(), document.getFilename(), document.getMimeType(), document.getGeneratedAt(), document.getGeneratedBy().getId(), document.getChecksumSha256(), document.isCurrentVersion());
  }

  OrderPlanPdfMetadataResponse orderPlanPdf(OrderPlanPdfDocument document) {
    return new OrderPlanPdfMetadataResponse(
        document.getId(),
        document.getOrderPlan().getId(),
        document.getVersionNumber(),
        document.getFilename(),
        document.getMimeType(),
        document.getByteSize(),
        document.getChecksumSha256(),
        document.getGeneratedAt(),
        document.getGeneratedBy().getId(),
        document.getGeneratedByNameSnapshot(),
        document.isCurrentVersion()
    );
  }
}
