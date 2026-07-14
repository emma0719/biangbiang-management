package com.restaurant.ops.ordering;

import com.restaurant.ops.auth.AppPrincipal;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventorySessionRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderPlanRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.InventorySessionResponse;
import com.restaurant.ops.ordering.OrderingDtos.InventoryTransitionRequest;
import com.restaurant.ops.ordering.OrderingDtos.MarkOrderedRequest;
import com.restaurant.ops.ordering.OrderingDtos.OrderPlanResponse;
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.ProductResponse;
import com.restaurant.ops.ordering.OrderingDtos.PurchaseOrderResponse;
import com.restaurant.ops.ordering.OrderingDtos.ReceiveOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.RejectOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorResponse;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class OrderingController {
  private final OrderingService service;
  private final OrderingMapper mapper;

  OrderingController(OrderingService service, OrderingMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping("/api/vendors")
  List<VendorResponse> vendors(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam(defaultValue = "false") boolean activeOnly) {
    return service.listVendors(principal.employee(), locationCode, activeOnly).stream().map(mapper::vendor).toList();
  }

  @GetMapping("/api/vendors/{id}")
  VendorResponse vendor(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.vendor(service.getVendor(principal.employee(), id));
  }

  @PostMapping("/api/vendors")
  VendorResponse createVendor(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody VendorRequest request) {
    return mapper.vendor(service.saveVendor(principal.employee(), request, null));
  }

  @PatchMapping("/api/vendors/{id}")
  VendorResponse updateVendor(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody VendorRequest request) {
    return mapper.vendor(service.saveVendor(principal.employee(), request, id));
  }

  @DeleteMapping("/api/vendors/{id}")
  VendorResponse deactivateVendor(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.vendor(service.deactivateVendor(principal.employee(), id));
  }

  @GetMapping("/api/order-products")
  List<ProductResponse> products(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam(required = false) Long vendorId, @RequestParam(required = false) CatalogCategory category, @RequestParam(required = false) InventoryBusiness inventoryBusiness, @RequestParam(required = false) OrderBusiness orderBusiness, @RequestParam(required = false) String search, @RequestParam(required = false) Boolean active) {
    return service.listProducts(principal.employee(), locationCode, vendorId, category, inventoryBusiness, orderBusiness, search, active).stream().map(mapper::product).toList();
  }

  @GetMapping("/api/order-products/{id}")
  ProductResponse product(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.product(service.getProduct(principal.employee(), id));
  }

  @PostMapping("/api/order-products")
  ProductResponse createProduct(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ProductRequest request) {
    return mapper.product(service.saveProduct(principal.employee(), request, null));
  }

  @PatchMapping("/api/order-products/{id}")
  ProductResponse updateProduct(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return mapper.product(service.saveProduct(principal.employee(), request, id));
  }

  @DeleteMapping("/api/order-products/{id}")
  ProductResponse deactivateProduct(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.product(service.deactivateProduct(principal.employee(), id));
  }

  @GetMapping("/api/order-products/{id}/price-history")
  List<OrderingDtos.PriceHistoryResponse> priceHistory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return service.priceHistory(principal.employee(), id).stream().map(mapper::price).toList();
  }

  @GetMapping("/api/inventory-catalog")
  List<ProductResponse> inventoryCatalog(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam InventoryBusiness business) {
    return service.listInventoryCatalog(principal.employee(), locationCode, business).stream().map(mapper::product).toList();
  }

  @PostMapping("/api/inventory-catalog")
  ProductResponse createInventoryCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreateInventoryCatalogItemRequest request) {
    return mapper.product(service.createInventoryCatalogItem(principal.employee(), request));
  }

  @PatchMapping("/api/inventory-catalog/{id}")
  ProductResponse updateInventoryCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdateInventoryCatalogItemRequest request) {
    return mapper.product(service.updateInventoryCatalogItem(principal.employee(), id, request));
  }

  @DeleteMapping("/api/inventory-catalog/{id}")
  ProductResponse deleteInventoryCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.product(service.deactivateInventoryCatalogItem(principal.employee(), id));
  }

  @GetMapping("/api/order-catalog")
  List<ProductResponse> orderCatalog(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam OrderBusiness business) {
    return service.listOrderCatalog(principal.employee(), locationCode, business).stream().map(mapper::product).toList();
  }

  @PostMapping("/api/order-catalog")
  ProductResponse createOrderCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreateOrderCatalogItemRequest request) {
    return mapper.product(service.createOrderCatalogItem(principal.employee(), request));
  }

  @PatchMapping("/api/order-catalog/{id}")
  ProductResponse updateOrderCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdateOrderCatalogItemRequest request) {
    return mapper.product(service.updateOrderCatalogItem(principal.employee(), id, request));
  }

  @DeleteMapping("/api/order-catalog/{id}")
  ProductResponse deleteOrderCatalogItem(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.product(service.deactivateOrderCatalogItem(principal.employee(), id));
  }

  @GetMapping("/api/order-inventory-reference")
  OrderingDtos.OrderInventoryReferenceResponse orderInventoryReference(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam OrderBusiness business) {
    return service.orderInventoryReference(principal.employee(), locationCode, business);
  }

  @PostMapping("/api/inventory-counts")
  InventorySessionResponse createInventory(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreateInventorySessionRequest request) {
    var session = service.createInventory(principal.employee(), request);
    return mapper.inventorySession(session, List.of());
  }

  @GetMapping("/api/inventory-counts")
  List<InventorySessionResponse> inventoryCounts(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam(required = false) InventoryBusiness business) {
    return service.listInventory(principal.employee(), locationCode, business).stream().map(session -> mapper.inventorySession(session, service.inventoryLines(session.getId()))).toList();
  }

  @GetMapping("/api/inventory-counts/{id}")
  InventorySessionResponse inventoryCount(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.inventorySession(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @GetMapping("/api/inventory-counts/{id}/pdf")
  ResponseEntity<byte[]> inventoryCountPdf(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    byte[] content = service.inventoryCountPdf(principal.employee(), id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(content.length)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("inventory-count-" + id + ".pdf").build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(content);
  }

  @PutMapping("/api/inventory-counts/{id}/lines")
  InventorySessionResponse updateInventoryLines(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpsertInventoryLinesRequest request) {
    var session = service.upsertInventoryLines(principal.employee(), id, request);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/complete")
  InventorySessionResponse completeInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.completeInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/start")
  InventorySessionResponse startInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.startInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/submit")
  InventorySessionResponse submitInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.submitInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/review")
  InventorySessionResponse reviewInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.reviewInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/lock")
  InventorySessionResponse lockInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.lockInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/override")
  InventorySessionResponse overrideInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @RequestBody(required = false) InventoryTransitionRequest request) {
    var session = service.overrideInventory(principal.employee(), id, request);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/inventory-counts/{id}/cancel")
  InventorySessionResponse cancelInventory(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var session = service.cancelInventory(principal.employee(), id);
    return mapper.inventorySession(session, service.inventoryLines(session.getId()));
  }

  @PostMapping("/api/order-plans")
  OrderPlanResponse createOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreateOrderPlanRequest request) {
    var plan = service.createOrderPlan(principal.employee(), request);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @GetMapping("/api/order-plans")
  List<OrderPlanResponse> orderPlans(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam(required = false) OrderPlanStatus status) {
    return service.listOrderPlans(principal.employee(), locationCode, status).stream()
        .map(plan -> mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId())))
        .toList();
  }

  @GetMapping("/api/order-plans/{id}")
  OrderPlanResponse orderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.getOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/start")
  OrderPlanResponse startOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.startOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PutMapping("/api/order-plans/{id}/lines")
  OrderPlanResponse updateOrderPlanLines(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpsertOrderPlanLinesRequest request) {
    var plan = service.upsertOrderPlanLines(principal.employee(), id, request);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/generate-vendor-orders")
  OrderPlanResponse generateVendorOrders(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    service.generateVendorOrders(principal.employee(), id);
    return orderPlan(principal, id);
  }

  @PostMapping("/api/order-plans/{id}/submit")
  OrderPlanResponse submitOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.submitOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/approve")
  OrderPlanResponse approveOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.approveOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/reject")
  OrderPlanResponse rejectOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody RejectOrderRequest request) {
    var plan = service.rejectOrderPlan(principal.employee(), id, request);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/complete")
  OrderPlanResponse completeOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.completeOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/order-plans/{id}/cancel")
  OrderPlanResponse cancelOrderPlan(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var plan = service.cancelOrderPlan(principal.employee(), id);
    return mapper.orderPlan(plan, service.orderPlanLines(plan.getId()), service.vendorOrdersForPlan(plan.getId()), service.auditEvents("ORDER_PLAN", plan.getId()));
  }

  @PostMapping("/api/purchase-orders")
  PurchaseOrderResponse createOrder(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody CreatePurchaseOrderRequest request) {
    var order = service.createOrder(principal.employee(), request);
    return mapper.purchaseOrder(order, List.of(), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @GetMapping("/api/purchase-orders")
  List<PurchaseOrderResponse> orders(@AuthenticationPrincipal AppPrincipal principal, @RequestParam StoreCode locationCode, @RequestParam(required = false) PurchaseOrderStatus status, @RequestParam(required = false) OrderBusiness orderBusiness) {
    return service.listOrders(principal.employee(), locationCode, status, orderBusiness).stream().map(order -> mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()))).toList();
  }

  @GetMapping("/api/purchase-orders/{id}")
  PurchaseOrderResponse order(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var order = service.getOrder(principal.employee(), id);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PatchMapping("/api/purchase-orders/{id}")
  PurchaseOrderResponse updateOrder(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpdatePurchaseOrderRequest request) {
    var order = service.updateOrder(principal.employee(), id, request);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PutMapping("/api/purchase-orders/{id}/lines")
  PurchaseOrderResponse updateOrderLines(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody UpsertPurchaseOrderLinesRequest request) {
    var order = service.upsertOrderLines(principal.employee(), id, request);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/submit")
  PurchaseOrderResponse submitOrder(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var order = service.submit(principal.employee(), id);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/approve")
  PurchaseOrderResponse approveOrder(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var order = service.approve(principal.employee(), id);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/reject")
  PurchaseOrderResponse rejectOrder(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody RejectOrderRequest request) {
    var order = service.reject(principal.employee(), id, request);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/mark-ordered")
  PurchaseOrderResponse markOrdered(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @RequestBody(required = false) MarkOrderedRequest request) {
    var order = service.markOrdered(principal.employee(), id, request == null ? new MarkOrderedRequest(null, null) : request);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/receive")
  PurchaseOrderResponse receive(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id, @Valid @RequestBody ReceiveOrderRequest request) {
    var order = service.receive(principal.employee(), id, request);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping("/api/purchase-orders/{id}/cancel")
  PurchaseOrderResponse cancel(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var order = service.cancel(principal.employee(), id);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @PostMapping({"/api/purchase-orders/{id}/reopen", "/api/order-plans/{id}/reopen"})
  PurchaseOrderResponse reopen(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var order = service.reopen(principal.employee(), id);
    return mapper.purchaseOrder(order, service.orderLines(order.getId()), service.auditEvents("PURCHASE_ORDER", order.getId()));
  }

  @GetMapping("/api/purchase-orders/{id}/summary")
  PurchaseOrderResponse orderSummary(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return order(principal, id);
  }

  @GetMapping("/api/purchase-orders/{id}/audit")
  List<OrderingDtos.AuditResponse> orderAudit(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    service.getOrder(principal.employee(), id);
    return service.auditEvents("PURCHASE_ORDER", id).stream().map(mapper::audit).toList();
  }

  @GetMapping("/api/purchase-orders/{id}/pdf")
  ResponseEntity<byte[]> orderPdf(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    var document = service.currentPdf(principal.employee(), id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.getFilename()).build().toString())
        .body(document.getContent());
  }

  @GetMapping("/api/purchase-orders/{id}/pdf/metadata")
  OrderingDtos.PdfMetadataResponse orderPdfMetadata(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.pdf(service.currentPdf(principal.employee(), id));
  }

  @GetMapping("/api/purchase-orders/{id}/pdf/versions")
  List<OrderingDtos.PdfMetadataResponse> orderPdfVersions(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return service.pdfVersions(principal.employee(), id).stream().map(mapper::pdf).toList();
  }

  @GetMapping("/api/order-plans/{id}/pdf")
  OrderingDtos.OrderPlanPdfMetadataResponse orderPlanPdfMetadata(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return mapper.orderPlanPdf(service.currentOrderPlanPdf(principal.employee(), id));
  }

  @GetMapping("/api/order-plans/{id}/pdf/view")
  ResponseEntity<byte[]> viewOrderPlanPdf(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return orderPlanPdfResponse(service.currentOrderPlanPdf(principal.employee(), id), ContentDisposition.inline());
  }

  @GetMapping("/api/order-plans/{id}/pdf/download")
  ResponseEntity<byte[]> downloadOrderPlanPdf(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
    return orderPlanPdfResponse(service.currentOrderPlanPdf(principal.employee(), id), ContentDisposition.attachment());
  }

  private ResponseEntity<byte[]> orderPlanPdfResponse(OrderPlanPdfDocument document, ContentDisposition.Builder disposition) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(document.getByteSize())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.filename(document.getFilename()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(document.getContent());
  }
}
