package com.restaurant.ops.ordering;

import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventorySessionRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderPlanRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.InventoryTransitionRequest;
import com.restaurant.ops.ordering.OrderingDtos.MarkOrderedRequest;
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.ReceiveOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.RejectOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateOrderPlanAmountsRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.InventoryCountStatus;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import com.restaurant.ops.security.AuthorizationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderingService {
  private final VendorRepository vendors;
  private final OrderCatalogProductRepository products;
  private final InventoryCountSessionRepository inventorySessions;
  private final InventoryCountLineRepository inventoryLines;
  private final OrderPlanSessionRepository orderPlans;
  private final OrderPlanLineRepository orderPlanLines;
  private final PurchaseOrderRepository purchaseOrders;
  private final PurchaseOrderLineRepository purchaseOrderLines;
  private final ProductPriceHistoryRepository priceHistory;
  private final OrderAuditEventRepository auditEvents;
  private final PurchaseOrderPdfDocumentRepository pdfDocuments;
  private final PurchaseOrderPdfService pdfService;
  private final OrderPlanPdfDocumentRepository orderPlanPdfDocuments;
  private final OrderPlanPdfService orderPlanPdfService;
  private final InventoryCountPdfService inventoryCountPdfService;
  private final OrderingAuditRecorder auditRecorder;
  private final AuthorizationService authorization;
  private final EmployeeRepository employees;

  public OrderingService(VendorRepository vendors, OrderCatalogProductRepository products, InventoryCountSessionRepository inventorySessions, InventoryCountLineRepository inventoryLines, OrderPlanSessionRepository orderPlans, OrderPlanLineRepository orderPlanLines, PurchaseOrderRepository purchaseOrders, PurchaseOrderLineRepository purchaseOrderLines, ProductPriceHistoryRepository priceHistory, OrderAuditEventRepository auditEvents, PurchaseOrderPdfDocumentRepository pdfDocuments, PurchaseOrderPdfService pdfService, OrderPlanPdfDocumentRepository orderPlanPdfDocuments, OrderPlanPdfService orderPlanPdfService, InventoryCountPdfService inventoryCountPdfService, OrderingAuditRecorder auditRecorder, AuthorizationService authorization, EmployeeRepository employees) {
    this.vendors = vendors;
    this.products = products;
    this.inventorySessions = inventorySessions;
    this.inventoryLines = inventoryLines;
    this.orderPlans = orderPlans;
    this.orderPlanLines = orderPlanLines;
    this.purchaseOrders = purchaseOrders;
    this.purchaseOrderLines = purchaseOrderLines;
    this.priceHistory = priceHistory;
    this.auditEvents = auditEvents;
    this.pdfDocuments = pdfDocuments;
    this.pdfService = pdfService;
    this.orderPlanPdfDocuments = orderPlanPdfDocuments;
    this.orderPlanPdfService = orderPlanPdfService;
    this.inventoryCountPdfService = inventoryCountPdfService;
    this.auditRecorder = auditRecorder;
    this.authorization = authorization;
    this.employees = employees;
  }

  public List<Vendor> listVendors(Employee actor, StoreCode locationCode, boolean activeOnly) {
    requireStoreAccess(actor, locationCode);
    return activeOnly ? vendors.findByLocationCodeAndActiveTrueOrderByDisplayOrderAscNameAsc(locationCode) : vendors.findByLocationCodeOrderByDisplayOrderAscNameAsc(locationCode);
  }

  public Vendor getVendor(Employee actor, Long id) {
    Vendor vendor = vendor(id);
    requireStoreAccess(actor, vendor.getLocationCode());
    return vendor;
  }

  @Transactional
  public Vendor saveVendor(Employee actor, VendorRequest request, Long id) {
    requireBusinessPartner(actor);
    requireStoreAccess(actor, request.locationCode());
    Vendor vendor = id == null ? new Vendor() : getVendor(actor, id);
    vendor.setLocationCode(request.locationCode());
    vendor.setName(request.name().trim());
    vendor.setNormalizedName(key(request.name()));
    vendor.setVendorCode(blankToNull(request.vendorCode()));
    vendor.setContactName(blankToNull(request.contactName()));
    vendor.setPhone(blankToNull(request.phone()));
    vendor.setEmail(blankToNull(request.email()));
    vendor.setOrderingMethod(blankToNull(request.orderingMethod()));
    vendor.setDeliveryDays(request.deliveryDays());
    vendor.setOrderDeadline(request.orderDeadline());
    vendor.setNotes(blankToNull(request.notes()));
    vendor.setActive(request.active() == null || request.active());
    vendor.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    return vendors.save(vendor);
  }

  @Transactional
  public Vendor deactivateVendor(Employee actor, Long id) {
    requireBusinessPartner(actor);
    Vendor vendor = getVendor(actor, id);
    vendor.setActive(false);
    return vendor;
  }

  public List<OrderCatalogProduct> listProducts(Employee actor, StoreCode locationCode, Long vendorId, OrderingEnums.CatalogCategory category, InventoryBusiness inventoryBusiness, OrderBusiness orderBusiness, String search, Boolean active) {
    requireStoreAccess(actor, locationCode);
    return products.findByLocationCodeOrderByDisplayOrderAscNameAsc(locationCode).stream()
        .filter(product -> vendorId == null || Objects.equals(product.getVendor().getId(), vendorId))
        .filter(product -> category == null || product.getCategory() == category)
        .filter(product -> inventoryBusiness == null || product.getInventoryBusiness() == inventoryBusiness)
        .filter(product -> orderBusiness == null || product.getOrderBusiness() == orderBusiness)
        .filter(product -> active == null || product.isActive() == active)
        .filter(product -> search == null || search.isBlank() || product.getNormalizedName().contains(key(search)) || (product.getVendorProductCode() != null && product.getVendorProductCode().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))))
        .toList();
  }

  public List<OrderCatalogProduct> listProducts(Employee actor, StoreCode locationCode, Long vendorId, OrderingEnums.CatalogCategory category, InventoryBusiness inventoryBusiness, String search, Boolean active) {
    return listProducts(actor, locationCode, vendorId, category, inventoryBusiness, null, search, active);
  }

  public OrderCatalogProduct getProduct(Employee actor, Long id) {
    OrderCatalogProduct product = product(id);
    requireStoreAccess(actor, product.getLocationCode());
    return product;
  }

  public List<OrderCatalogProduct> listInventoryCatalog(Employee actor, StoreCode locationCode, InventoryBusiness business) {
    requireStoreAccess(actor, locationCode);
    return products.findByLocationCodeAndInventoryBusinessOrderByDisplayOrderAscNameAsc(locationCode, business).stream()
        .filter(OrderCatalogProduct::isActive)
        .toList();
  }

  public List<OrderCatalogProduct> listOrderCatalog(Employee actor, StoreCode locationCode, OrderBusiness business) {
    requireStoreAccess(actor, locationCode);
    return products.findByLocationCodeAndOrderBusinessOrderByDisplayOrderAscNameAsc(locationCode, business).stream()
        .filter(OrderCatalogProduct::isActive)
        .toList();
  }

  @Transactional
  public OrderCatalogProduct createInventoryCatalogItem(Employee actor, CreateInventoryCatalogItemRequest request) {
    requireBusinessPartner(actor);
    requireStoreAccess(actor, request.locationCode());
    Vendor vendor = inventoryCatalogVendor(request.locationCode(), request.vendorName());
    OrderCatalogProduct product = new OrderCatalogProduct();
    product.setLocationCode(request.locationCode());
    product.setVendor(vendor);
    product.setName(request.name().trim());
    product.setNormalizedName(productKey(null, request.name()));
    product.setCategory(CatalogCategory.OTHER);
    product.setInventoryBusiness(request.inventoryBusiness());
    product.setOrderBusiness(null);
    CatalogUnit unit = inventoryCatalogUnit(request.unit());
    String unitLabel = request.unit().trim();
    product.setInventoryUnit(unit);
    product.setInventoryUnitLabel(unitLabel);
    product.setOrderUnit(unit);
    product.setOrderUnitLabel(unitLabel);
    product.setCurrency("USD");
    product.setActive(true);
    product.setDisplayOrder((int) Math.min(Integer.MAX_VALUE, products.count() + 1));
    OrderCatalogProduct saved = products.save(product);
    audit("PRODUCT", saved.getId(), "INVENTORY_CATALOG_CREATED", actor, null, saved.getInventoryBusiness().name(), null);
    return saved;
  }

  @Transactional
  public OrderCatalogProduct createOrderCatalogItem(Employee actor, CreateOrderCatalogItemRequest request) {
    requireBusinessPartner(actor);
    requireStoreAccess(actor, request.locationCode());
    Vendor vendor = inventoryCatalogVendor(request.locationCode(), request.vendorName());
    OrderCatalogProduct product = new OrderCatalogProduct();
    product.setLocationCode(request.locationCode());
    product.setVendor(vendor);
    product.setName(request.name().trim());
    product.setNormalizedName(productKey(null, request.name()));
    product.setCategory(CatalogCategory.OTHER);
    product.setInventoryBusiness(null);
    product.setOrderBusiness(request.orderBusiness());
    CatalogUnit unit = inventoryCatalogUnit(request.unit());
    String unitLabel = request.unit().trim();
    product.setInventoryUnit(unit);
    product.setInventoryUnitLabel(unitLabel);
    product.setOrderUnit(unit);
    product.setOrderUnitLabel(unitLabel);
    product.setCurrency("USD");
    product.setActive(true);
    product.setDisplayOrder((int) Math.min(Integer.MAX_VALUE, products.count() + 1));
    OrderCatalogProduct saved = products.save(product);
    audit("PRODUCT", saved.getId(), "ORDER_CATALOG_CREATED", actor, null, saved.getOrderBusiness().name(), null);
    return saved;
  }

  @Transactional
  public OrderCatalogProduct updateOrderCatalogItem(Employee actor, Long id, UpdateOrderCatalogItemRequest request) {
    requireBusinessPartner(actor);
    OrderCatalogProduct product = orderCatalogProduct(actor, id);
    Vendor vendor = inventoryCatalogVendor(product.getLocationCode(), request.vendorName());
    product.setVendor(vendor);
    product.setName(request.name().trim());
    product.setNormalizedName(productKey(null, request.name()));
    CatalogUnit unit = inventoryCatalogUnit(request.unit());
    String unitLabel = request.unit().trim();
    product.setInventoryUnit(unit);
    product.setInventoryUnitLabel(unitLabel);
    product.setOrderUnit(unit);
    product.setOrderUnitLabel(unitLabel);
    if (request.active() != null) {
      product.setActive(request.active());
    }
    audit("PRODUCT", product.getId(), "ORDER_CATALOG_UPDATED", actor, null, product.getOrderBusiness().name(), null);
    return product;
  }

  @Transactional
  public OrderCatalogProduct deactivateOrderCatalogItem(Employee actor, Long id) {
    requireBusinessPartner(actor);
    OrderCatalogProduct product = orderCatalogProduct(actor, id);
    product.setActive(false);
    audit("PRODUCT", product.getId(), "ORDER_CATALOG_DEACTIVATED", actor, null, product.getOrderBusiness().name(), null);
    return product;
  }

  @Transactional
  public OrderCatalogProduct updateInventoryCatalogItem(Employee actor, Long id, UpdateInventoryCatalogItemRequest request) {
    requireBusinessPartner(actor);
    OrderCatalogProduct product = inventoryCatalogProduct(actor, id);
    Vendor vendor = inventoryCatalogVendor(product.getLocationCode(), request.vendorName());
    product.setVendor(vendor);
    product.setName(request.name().trim());
    product.setNormalizedName(productKey(null, request.name()));
    CatalogUnit unit = inventoryCatalogUnit(request.unit());
    String unitLabel = request.unit().trim();
    product.setInventoryUnit(unit);
    product.setInventoryUnitLabel(unitLabel);
    product.setOrderUnit(unit);
    product.setOrderUnitLabel(unitLabel);
    if (request.active() != null) {
      product.setActive(request.active());
    }
    audit("PRODUCT", product.getId(), "INVENTORY_CATALOG_UPDATED", actor, null, product.getInventoryBusiness().name(), null);
    return product;
  }

  @Transactional
  public OrderCatalogProduct deactivateInventoryCatalogItem(Employee actor, Long id) {
    requireBusinessPartner(actor);
    OrderCatalogProduct product = inventoryCatalogProduct(actor, id);
    product.setActive(false);
    audit("PRODUCT", product.getId(), "INVENTORY_CATALOG_DEACTIVATED", actor, null, product.getInventoryBusiness().name(), null);
    return product;
  }

  @Transactional
  public OrderCatalogProduct saveProduct(Employee actor, ProductRequest request, Long id) {
    requireBusinessPartner(actor);
    requireStoreAccess(actor, request.locationCode());
    Vendor vendor = getVendor(actor, request.vendorId());
    if (vendor.getLocationCode() != request.locationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_VENDOR_LOCATION_MISMATCH");
    OrderCatalogProduct product = id == null ? new OrderCatalogProduct() : getProduct(actor, id);
    BigDecimal oldPrice = product.getUnitPrice();
    product.setLocationCode(request.locationCode());
    product.setVendor(vendor);
    product.setVendorProductCode(blankToNull(request.vendorProductCode()));
    product.setName(request.name().trim());
    product.setNormalizedName(productKey(request.vendorProductCode(), request.name()));
    product.setChineseDisplayName(blankToNull(request.chineseDisplayName()));
    product.setCategory(request.category());
    product.setInventoryBusiness(request.inventoryBusiness());
    product.setOrderBusiness(request.orderBusiness());
    product.setStorageArea(blankToNull(request.storageArea()));
    product.setInventoryUnit(request.inventoryUnit());
    product.setInventoryUnitLabel(blankToNull(request.inventoryUnitLabel()));
    product.setOrderUnit(request.orderUnit());
    product.setOrderUnitLabel(blankToNull(request.orderUnitLabel()));
    product.setPackageSpecification(blankToNull(request.packageSpecification()));
    product.setUnitPrice(moneyOrNull(request.unitPrice()));
    product.setCurrency(request.currency() == null || request.currency().isBlank() ? "USD" : request.currency());
    product.setParLevel(request.parLevel());
    product.setReorderPoint(request.reorderPoint());
    product.setDefaultOrderQuantity(request.defaultOrderQuantity());
    product.setActive(request.active() == null || request.active());
    product.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    product.setNotes(blankToNull(request.notes()));
    OrderCatalogProduct saved = products.save(product);
    if (saved.getUnitPrice() != null && (oldPrice == null || oldPrice.compareTo(saved.getUnitPrice()) != 0)) {
      recordPrice(saved, saved.getUnitPrice(), LocalDate.now(), null, actor);
      audit("PRODUCT", saved.getId(), "PRICE_CHANGED", actor, oldPrice == null ? null : oldPrice.toPlainString(), saved.getUnitPrice().toPlainString(), null);
    }
    return saved;
  }

  @Transactional
  public OrderCatalogProduct deactivateProduct(Employee actor, Long id) {
    requireBusinessPartner(actor);
    OrderCatalogProduct product = getProduct(actor, id);
    product.setActive(false);
    return product;
  }

  @Transactional
  public InventoryCountSession createInventory(Employee actor, CreateInventorySessionRequest request) {
    requireStoreAccess(actor, request.locationCode());
    Employee assignedCounter = request.assignedCounterEmployeeId() == null ? null : employees.findById(request.assignedCounterEmployeeId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
    if (assignedCounter != null) {
      requireStoreAccess(assignedCounter, request.locationCode());
    }
    InventoryCountSession session = new InventoryCountSession();
    session.setLocationCode(request.locationCode());
    session.setInventoryBusiness(request.inventoryBusiness());
    session.setBusinessDate(request.businessDate());
    session.setCreatedBy(actor);
    session.setAssignedCounter(assignedCounter);
    session.setDueDate(request.dueDate());
    session.setDueTime(request.dueTime());
    session.setNotes(blankToNull(request.notes()));
    InventoryCountSession saved = inventorySessions.save(session);
    audit("INVENTORY_COUNT", saved.getId(), "INVENTORY_CREATED", actor, null, saved.getStatus().name(), null);
    if (assignedCounter != null) audit("INVENTORY_COUNT", saved.getId(), "INVENTORY_ASSIGNED", actor, null, assignedCounter.getId().toString(), null);
    return saved;
  }

  @Transactional
  public InventoryCountSession upsertInventoryLines(Employee actor, Long sessionId, UpsertInventoryLinesRequest request) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.DRAFT || session.getStatus() == InventoryCountStatus.IN_PROGRESS, "INVENTORY_NOT_EDITABLE");
    if (session.getStatus() == InventoryCountStatus.DRAFT) {
      session.setStatus(InventoryCountStatus.IN_PROGRESS);
      if (session.getStartedAt() == null) session.setStartedAt(Instant.now());
      audit("INVENTORY_COUNT", session.getId(), "INVENTORY_STARTED", actor, InventoryCountStatus.DRAFT.name(), InventoryCountStatus.IN_PROGRESS.name(), null);
    }
    for (var item : request.lines()) {
      OrderCatalogProduct product = getProduct(actor, item.productId());
      if (product.getLocationCode() != session.getLocationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_LOCATION_MISMATCH");
      if (product.getInventoryBusiness() == null || product.getInventoryBusiness() != session.getInventoryBusiness()) throw new ApiException(HttpStatus.BAD_REQUEST, "INVENTORY_PRODUCT_BUSINESS_MISMATCH");
      InventoryCountLine line = inventoryLines.findBySessionIdAndProductId(session.getId(), product.getId()).orElseGet(InventoryCountLine::new);
      line.setSession(session);
      line.setProduct(product);
      line.setQuantityOnHand(nonNegative(item.quantityOnHand(), "ORDER_NEGATIVE_QUANTITY"));
      line.setUnit(item.unit() == null ? product.getInventoryUnit() : item.unit());
      line.setNotes(blankToNull(item.notes()));
      line.setUpdatedBy(actor);
      inventoryLines.save(line);
    }
    audit("INVENTORY_COUNT", session.getId(), "DRAFT_EDITED", actor, null, null, null);
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_LINE_UPDATED", actor, null, String.valueOf(request.lines().size()), null);
    return session;
  }

  @Transactional
  public InventoryCountSession startInventory(Employee actor, Long sessionId) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.DRAFT, "INVENTORY_NOT_STARTABLE");
    session.setStatus(InventoryCountStatus.IN_PROGRESS);
    session.setStartedAt(Instant.now());
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_STARTED", actor, InventoryCountStatus.DRAFT.name(), session.getStatus().name(), null);
    return session;
  }

  @Transactional
  public InventoryCountSession submitInventory(Employee actor, Long sessionId) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.DRAFT || session.getStatus() == InventoryCountStatus.IN_PROGRESS, "INVENTORY_NOT_SUBMITTABLE");
    InventoryCountStatus oldStatus = session.getStatus();
    session.setStatus(InventoryCountStatus.SUBMITTED);
    session.setCompletedBy(actor);
    session.setCompletedByNameSnapshot(actor.getDisplayName());
    session.setCompletedAt(Instant.now());
    session.setSubmittedBy(actor);
    session.setSubmittedByNameSnapshot(actor.getDisplayName());
    session.setSubmittedAt(session.getCompletedAt());
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_SUBMITTED", actor, oldStatus.name(), session.getStatus().name(), null);
    return session;
  }

  @Transactional
  public InventoryCountSession completeInventory(Employee actor, Long sessionId) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.DRAFT || session.getStatus() == InventoryCountStatus.IN_PROGRESS || session.getStatus() == InventoryCountStatus.SUBMITTED, "INVENTORY_NOT_COMPLETABLE");
    InventoryCountStatus oldStatus = session.getStatus();
    session.setStatus(InventoryCountStatus.COMPLETED);
    session.setCompletedBy(actor);
    session.setCompletedByNameSnapshot(actor.getDisplayName());
    session.setCompletedAt(Instant.now());
    session.setSubmittedBy(actor);
    session.setSubmittedByNameSnapshot(actor.getDisplayName());
    session.setSubmittedAt(session.getCompletedAt());
    audit("INVENTORY_COUNT", session.getId(), "COMPLETED", actor, oldStatus.name(), session.getStatus().name(), null);
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_SUBMITTED", actor, oldStatus.name(), session.getStatus().name(), null);
    return session;
  }

  @Transactional
  public InventoryCountSession reviewInventory(Employee actor, Long sessionId) {
    requireBusinessPartner(actor);
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.SUBMITTED || session.getStatus() == InventoryCountStatus.COMPLETED, "INVENTORY_NOT_REVIEWABLE");
    InventoryCountStatus oldStatus = session.getStatus();
    session.setStatus(InventoryCountStatus.REVIEWED);
    session.setReviewedBy(actor);
    session.setReviewedByNameSnapshot(actor.getDisplayName());
    session.setReviewedAt(Instant.now());
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_REVIEWED", actor, oldStatus.name(), session.getStatus().name(), null);
    return session;
  }

  @Transactional
  public InventoryCountSession lockInventory(Employee actor, Long sessionId) {
    requireBusinessPartner(actor);
    InventoryCountSession session = inventorySession(actor, sessionId);
    requireStatus(session.getStatus() == InventoryCountStatus.REVIEWED || session.getStatus() == InventoryCountStatus.SUBMITTED || session.getStatus() == InventoryCountStatus.COMPLETED, "INVENTORY_NOT_LOCKABLE");
    InventoryCountStatus oldStatus = session.getStatus();
    session.setStatus(InventoryCountStatus.LOCKED);
    session.setLockedBy(actor);
    session.setLockedByNameSnapshot(actor.getDisplayName());
    session.setLockedAt(Instant.now());
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_LOCKED", actor, oldStatus.name(), session.getStatus().name(), null);
    return session;
  }

  @Transactional
  public InventoryCountSession overrideInventory(Employee actor, Long sessionId, InventoryTransitionRequest request) {
    requireBusinessPartner(actor);
    InventoryCountSession session = inventorySession(actor, sessionId);
    String reason = request == null ? null : blankToNull(request.reason());
    if (reason == null) throw new ApiException(HttpStatus.BAD_REQUEST, "INVENTORY_OVERRIDE_REASON_REQUIRED");
    InventoryCountStatus oldStatus = session.getStatus();
    requireStatus(oldStatus == InventoryCountStatus.SUBMITTED || oldStatus == InventoryCountStatus.REVIEWED || oldStatus == InventoryCountStatus.LOCKED || oldStatus == InventoryCountStatus.COMPLETED, "INVENTORY_NOT_OVERRIDABLE");
    session.setStatus(InventoryCountStatus.IN_PROGRESS);
    session.setOverrideReason(reason);
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_OVERRIDDEN", actor, oldStatus.name(), session.getStatus().name(), reason);
    return session;
  }

  @Transactional
  public InventoryCountSession cancelInventory(Employee actor, Long sessionId) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    if (!Objects.equals(session.getCreatedBy().getId(), actor.getId())) requireBusinessPartner(actor);
    InventoryCountStatus oldStatus = session.getStatus();
    requireStatus(oldStatus != InventoryCountStatus.LOCKED, "INVENTORY_NOT_CANCELLABLE");
    session.setStatus(InventoryCountStatus.CANCELLED);
    session.setCancelledAt(Instant.now());
    audit("INVENTORY_COUNT", session.getId(), "INVENTORY_CANCELLED", actor, oldStatus.name(), session.getStatus().name(), null);
    return session;
  }

  public List<InventoryCountSession> listInventory(Employee actor, StoreCode locationCode) {
    return listInventory(actor, locationCode, null);
  }

  public List<InventoryCountSession> listInventory(Employee actor, StoreCode locationCode, InventoryBusiness business) {
    requireStoreAccess(actor, locationCode);
    List<InventoryCountSession> sessions = business == null
        ? inventorySessions.findByLocationCodeOrderByBusinessDateDescIdDesc(locationCode)
        : inventorySessions.findByLocationCodeAndInventoryBusinessOrderByBusinessDateDescIdDesc(locationCode, business);
    return sessions.stream()
        .sorted((left, right) -> {
          int timeComparison = effectiveInventoryHistoryTime(right).compareTo(effectiveInventoryHistoryTime(left));
          return timeComparison != 0 ? timeComparison : right.getId().compareTo(left.getId());
        })
        .toList();
  }

  private Instant effectiveInventoryHistoryTime(InventoryCountSession session) {
    if (session.getCompletedAt() != null) return session.getCompletedAt();
    if (session.getSubmittedAt() != null) return session.getSubmittedAt();
    if (session.getUpdatedAt() != null) return session.getUpdatedAt();
    return session.getCreatedAt();
  }

  public InventoryCountSession inventorySession(Employee actor, Long id) {
    InventoryCountSession session = inventorySessions.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_COUNT_NOT_FOUND"));
    requireStoreAccess(actor, session.getLocationCode());
    return session;
  }

  @Transactional
  public OrderPlanSession createOrderPlan(Employee actor, CreateOrderPlanRequest request) {
    requireStoreAccess(actor, request.locationCode());
    InventoryCountSession source = request.sourceInventorySessionId() == null ? null : inventorySession(actor, request.sourceInventorySessionId());
    if (source != null && source.getLocationCode() != request.locationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PLAN_INVENTORY_LOCATION_MISMATCH");
    if (source != null && !isEligibleInventorySource(source)) throw new ApiException(HttpStatus.CONFLICT, "ORDER_PLAN_INVENTORY_NOT_ELIGIBLE");
    Employee assignedOrderer = request.assignedOrdererEmployeeId() == null ? null : employees.findById(request.assignedOrdererEmployeeId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EMPLOYEE_NOT_FOUND"));
    if (assignedOrderer != null) {
      requireStoreAccess(assignedOrderer, request.locationCode());
    }
    OrderPlanSession plan = new OrderPlanSession();
    plan.setLocationCode(request.locationCode());
    plan.setOrderBusiness(request.orderBusiness() == null ? OrderBusiness.BIANGBIANG_FRONT : request.orderBusiness());
    plan.setSourceInventorySession(source);
    if (source != null) {
      plan.setInventoryBusinessDate(source.getBusinessDate());
      plan.setInventoryCompletedBy(source.getCompletedBy());
      plan.setInventoryCompletedByNameSnapshot(source.getCompletedByNameSnapshot());
      plan.setInventoryCompletedAt(source.getCompletedAt());
    }
    plan.setBusinessDate(request.businessDate());
    plan.setCreatedBy(actor);
    plan.setAssignedOrderer(assignedOrderer);
    plan.setDueDate(request.dueDate());
    plan.setDueTime(request.dueTime());
    plan.setNotes(blankToNull(request.notes()));
    OrderPlanSession saved = orderPlans.save(plan);
    audit("ORDER_PLAN", saved.getId(), "ORDER_PLAN_CREATED", actor, null, saved.getStatus().name(), null);
    if (source != null) audit("ORDER_PLAN", saved.getId(), "SOURCE_INVENTORY_SELECTED", actor, null, source.getId().toString(), null);
    if (assignedOrderer != null) audit("ORDER_PLAN", saved.getId(), "ORDERER_ASSIGNED", actor, null, assignedOrderer.getId().toString(), null);
    seedOrderPlanLines(saved, actor);
    return saved;
  }

  @Transactional
  public OrderPlanSession startOrderPlan(Employee actor, Long id) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    requireOwnedEditableSession(actor, plan);
    requireStatus(plan.getStatus() == OrderPlanStatus.DRAFT, "ORDER_PLAN_NOT_STARTABLE");
    plan.setStatus(OrderPlanStatus.IN_PROGRESS);
    plan.setStartedAt(Instant.now());
    audit("ORDER_PLAN", plan.getId(), "ORDER_PLAN_STARTED", actor, OrderPlanStatus.DRAFT.name(), plan.getStatus().name(), null);
    return plan;
  }

  @Transactional
  public OrderPlanSession upsertOrderPlanLines(Employee actor, Long id, UpsertOrderPlanLinesRequest request) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    requireOwnedEditableSession(actor, plan);
    requireStatus(plan.getStatus() == OrderPlanStatus.DRAFT || plan.getStatus() == OrderPlanStatus.IN_PROGRESS, "ORDER_PLAN_NOT_EDITABLE");
    if (plan.getStatus() == OrderPlanStatus.DRAFT) {
      plan.setStatus(OrderPlanStatus.IN_PROGRESS);
      plan.setStartedAt(Instant.now());
      audit("ORDER_PLAN", plan.getId(), "ORDER_PLAN_STARTED", actor, OrderPlanStatus.DRAFT.name(), plan.getStatus().name(), null);
    }
    for (var item : request.lines()) {
      OrderPlanLine line = orderPlanLines.findByOrderPlanSessionIdAndProductId(plan.getId(), item.productId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_PLAN_LINE_NOT_FOUND"));
      BigDecimal old = line.getFinalOrderQuantity();
      BigDecimal next = item.finalOrderQuantity() == null ? null : nonNegative(item.finalOrderQuantity(), "ORDER_NEGATIVE_QUANTITY");
      line.setFinalOrderQuantity(next);
      line.setNotes(blankToNull(item.notes()));
      line.setUpdatedBy(actor);
      if (!Objects.equals(old, next) && line.getSuggestedOrderQuantity() != null && next != null && line.getSuggestedOrderQuantity().compareTo(next) != 0) {
        audit("ORDER_PLAN", plan.getId(), "FINAL_QUANTITY_OVERRIDDEN", actor, old == null ? null : old.toPlainString(), next.toPlainString(), line.getNotes());
      }
    }
    audit("ORDER_PLAN", plan.getId(), "ORDER_PLAN_LINES_UPDATED", actor, null, String.valueOf(request.lines().size()), null);
    return plan;
  }

  @Transactional
  public List<PurchaseOrder> generateVendorOrders(Employee actor, Long id) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    requireStatus(plan.getStatus() == OrderPlanStatus.DRAFT || plan.getStatus() == OrderPlanStatus.IN_PROGRESS || plan.getStatus() == OrderPlanStatus.SUBMITTED, "ORDER_PLAN_NOT_GENERATABLE");
    List<PurchaseOrder> existing = purchaseOrders.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameAsc(plan.getId());
    if (!existing.isEmpty()) return existing;
    Map<Vendor, List<OrderPlanLine>> byVendor = orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()).stream()
        .filter(line -> line.getFinalOrderQuantity() != null && line.getFinalOrderQuantity().compareTo(BigDecimal.ZERO) > 0)
        .collect(Collectors.groupingBy(OrderPlanLine::getVendor, java.util.LinkedHashMap::new, Collectors.toList()));
    List<PurchaseOrder> generated = byVendor.entrySet().stream().map(entry -> createVendorOrderFromPlan(actor, plan, entry.getKey(), entry.getValue())).toList();
    audit("ORDER_PLAN", plan.getId(), "VENDOR_ORDERS_GENERATED", actor, null, String.valueOf(generated.size()), null);
    return generated;
  }

  @Transactional
  public OrderPlanSession submitOrderPlan(Employee actor, Long id) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    requireStatus(plan.getStatus() == OrderPlanStatus.DRAFT || plan.getStatus() == OrderPlanStatus.IN_PROGRESS, "ORDER_PLAN_NOT_SUBMITTABLE");
    boolean hasLines = orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()).stream()
        .anyMatch(line -> line.getFinalOrderQuantity() != null && line.getFinalOrderQuantity().compareTo(BigDecimal.ZERO) > 0);
    requireStatus(hasLines, "ORDER_SESSION_HAS_NO_LINES");
    OrderPlanStatus oldStatus = plan.getStatus();
    plan.setStatus(OrderPlanStatus.SUBMITTED);
    plan.setSubmittedBy(actor);
    plan.setSubmittedByNameSnapshot(actor.getDisplayName());
    plan.setSubmittedAt(Instant.now());
    plan.setReviewNote(null);
    audit("ORDER_PLAN", plan.getId(), "ORDER_PLAN_SUBMITTED", actor, oldStatus.name(), plan.getStatus().name(), null);
    createOrderPlanPdfDocument(plan, actor);
    return plan;
  }

  @Transactional
  public OrderPlanSession approveOrderPlan(Employee actor, Long id) {
    throw new ApiException(HttpStatus.GONE, "ORDER_SESSION_APPROVAL_DISABLED");
  }

  @Transactional
  public OrderPlanSession completeOrderPlan(Employee actor, Long id) {
    throw new ApiException(HttpStatus.GONE, "ORDER_SESSION_APPROVAL_DISABLED");
  }

  @Transactional
  public OrderPlanSession rejectOrderPlan(Employee actor, Long id, RejectOrderRequest request) {
    throw new ApiException(HttpStatus.GONE, "ORDER_SESSION_APPROVAL_DISABLED");
  }

  @Transactional
  public OrderPlanSession updateSubmittedOrderPlanAmounts(Employee actor, Long id, UpdateOrderPlanAmountsRequest request) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    requireBusinessPartner(actor);
    requireExplicitStoreAccess(actor, plan.getLocationCode());
    requireStatus(plan.getStatus() == OrderPlanStatus.SUBMITTED, "ORDER_SESSION_NOT_EDITABLE");
    Map<Long, OrderPlanLine> linesById = orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()).stream()
        .collect(Collectors.toMap(OrderPlanLine::getId, line -> line));
    StringBuilder changeSummary = new StringBuilder();
    for (var item : request.lines()) {
      OrderPlanLine line = linesById.get(item.lineId());
      if (line == null) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PLAN_LINE_MISMATCH");
      BigDecimal old = line.getFinalOrderQuantity();
      BigDecimal next = nonNegative(item.finalOrderQuantity(), "ORDER_NEGATIVE_QUANTITY");
      line.setFinalOrderQuantity(next);
      line.setNotes(blankToNull(item.notes()));
      line.setUpdatedBy(actor);
      if (!Objects.equals(old, next)) {
        if (changeSummary.length() > 0) changeSummary.append("; ");
        changeSummary.append(line.getProductNameSnapshot()).append(": ")
            .append(old == null ? "null" : old.toPlainString())
            .append(" -> ")
            .append(next.toPlainString());
      }
    }
    plan.setLastModifiedBy(actor);
    plan.setLastModifiedByNameSnapshot(actor.getDisplayName());
    audit("ORDER_PLAN", plan.getId(), "ORDER_SESSION_AMOUNTS_UPDATED", actor, null, String.valueOf(request.lines().size()), changeSummary.length() == 0 ? null : changeSummary.toString());
    createOrderPlanPdfDocument(plan, actor);
    return plan;
  }

  @Transactional
  public OrderPlanSession cancelOrderPlan(Employee actor, Long id) {
    OrderPlanSession plan = getOrderPlan(actor, id);
    if (!Objects.equals(plan.getCreatedBy().getId(), actor.getId())) requireBusinessPartner(actor);
    requireStatus(plan.getStatus() == OrderPlanStatus.DRAFT || plan.getStatus() == OrderPlanStatus.IN_PROGRESS, "ORDER_SESSION_NOT_REMOVABLE");
    OrderPlanStatus oldStatus = plan.getStatus();
    plan.setStatus(OrderPlanStatus.CANCELLED);
    plan.setCancelledAt(Instant.now());
    audit("ORDER_PLAN", plan.getId(), "ORDER_PLAN_CANCELLED", actor, oldStatus.name(), plan.getStatus().name(), null);
    return plan;
  }

  public List<OrderPlanSession> listOrderPlans(Employee actor, StoreCode locationCode, OrderPlanStatus status) {
    requireStoreAccess(actor, locationCode);
    List<OrderPlanSession> result = status == null ? orderPlans.findByLocationCodeOrderByBusinessDateDescIdDesc(locationCode) : orderPlans.findByLocationCodeAndStatusOrderByBusinessDateDescIdDesc(locationCode, status);
    return result;
  }

  public OrderPlanSession getOrderPlan(Employee actor, Long id) {
    OrderPlanSession plan = orderPlans.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_PLAN_NOT_FOUND"));
    requireStoreAccess(actor, plan.getLocationCode());
    return plan;
  }

  public List<OrderPlanLine> orderPlanLines(Long planId) {
    return orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(planId);
  }

  public List<PurchaseOrder> vendorOrdersForPlan(Long planId) {
    return purchaseOrders.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameAsc(planId);
  }

  @Transactional
  public PurchaseOrder createOrder(Employee actor, CreatePurchaseOrderRequest request) {
    requireStoreAccess(actor, request.locationCode());
    Vendor vendor = getVendor(actor, request.vendorId());
    if (vendor.getLocationCode() != request.locationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_VENDOR_LOCATION_MISMATCH");
    PurchaseOrder order = new PurchaseOrder();
    order.setLocationCode(request.locationCode());
    order.setOrderBusiness(request.orderBusiness() == null ? OrderBusiness.BIANGBIANG_FRONT : request.orderBusiness());
    order.setVendor(vendor);
    if (request.sourceInventorySessionId() != null) {
      InventoryCountSession source = inventorySession(actor, request.sourceInventorySessionId());
      if (source.getLocationCode() != request.locationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_INVENTORY_LOCATION_MISMATCH");
      if (!isEligibleInventorySource(source)) throw new ApiException(HttpStatus.CONFLICT, "ORDER_INVENTORY_NOT_COMPLETED");
      if (orderBusiness(source.getInventoryBusiness()) != order.getOrderBusiness()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_INVENTORY_BUSINESS_MISMATCH");
      order.setSourceInventorySession(source);
    }
    order.setBusinessDate(request.businessDate());
    order.setExpectedDeliveryDate(request.expectedDeliveryDate());
    order.setCreatedBy(actor);
    order.setExternalOrderNotes(blankToNull(request.externalOrderNotes()));
    order.setOrderNumber(nextOrderNumber(request.locationCode(), request.businessDate(), vendor));
    PurchaseOrder saved = purchaseOrders.save(order);
    audit("PURCHASE_ORDER", saved.getId(), "CREATED", actor, null, saved.getStatus().name(), null);
    return saved;
  }

  @Transactional
  public PurchaseOrder updateOrder(Employee actor, Long id, UpdatePurchaseOrderRequest request) {
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.DRAFT, "ORDER_NOT_EDITABLE");
    order.setExpectedDeliveryDate(request.expectedDeliveryDate());
    order.setTax(money(request.tax()));
    order.setFees(money(request.fees()));
    order.setExternalOrderNotes(blankToNull(request.externalOrderNotes()));
    recalculate(order);
    audit("PURCHASE_ORDER", order.getId(), "DRAFT_EDITED", actor, null, null, null);
    return order;
  }

  @Transactional
  public PurchaseOrder upsertOrderLines(Employee actor, Long id, UpsertPurchaseOrderLinesRequest request) {
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.DRAFT || order.getStatus() == PurchaseOrderStatus.APPROVED, "ORDER_LINES_NOT_EDITABLE");
    boolean manager = authorization.isBusinessPartner(actor);
    if (order.getStatus() == PurchaseOrderStatus.APPROVED && !manager) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_BUSINESS_PARTNER_REQUIRED");
    for (var item : request.lines()) {
      OrderCatalogProduct product = getProduct(actor, item.productId());
      if (product.getLocationCode() != order.getLocationCode()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_VENDOR_MISMATCH");
      if (product.getOrderBusiness() == null || product.getOrderBusiness() != order.getOrderBusiness()) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_BUSINESS_MISMATCH");
      if (!Objects.equals(product.getVendor().getId(), order.getVendor().getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_PRODUCT_VENDOR_MISMATCH");
      PurchaseOrderLine line = purchaseOrderLines.findByPurchaseOrderIdAndProductId(order.getId(), product.getId()).orElseGet(PurchaseOrderLine::new);
      line.setPurchaseOrder(order);
      line.setProduct(product);
      line.setProductNameSnapshot(product.getName());
      line.setProductCodeSnapshot(product.getVendorProductCode());
      line.setPackageSpecificationSnapshot(product.getPackageSpecification());
      line.setOrderUnitSnapshot(product.getOrderUnit());
      line.setUnitPriceSnapshot(money(product.getUnitPrice()));
      if (order.getStatus() == PurchaseOrderStatus.APPROVED) {
        line.setApprovedQuantity(nonNegative(item.approvedQuantity() == null ? item.requestedQuantity() : item.approvedQuantity(), "ORDER_NEGATIVE_QUANTITY"));
      } else {
        line.setCurrentInventoryQuantity(item.currentInventoryQuantity());
        line.setRequestedQuantity(nonNegative(item.requestedQuantity(), "ORDER_NEGATIVE_QUANTITY"));
      }
      if (item.approvedQuantity() != null) {
        if (!manager) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_BUSINESS_PARTNER_REQUIRED");
        line.setApprovedQuantity(nonNegative(item.approvedQuantity(), "ORDER_NEGATIVE_QUANTITY"));
      }
      line.setNotes(blankToNull(item.notes()));
      line.setLineTotal(lineTotal(line));
      purchaseOrderLines.save(line);
    }
    recalculate(order);
    audit("PURCHASE_ORDER", order.getId(), "DRAFT_EDITED", actor, null, null, null);
    return order;
  }

  @Transactional
  public PurchaseOrder submit(Employee actor, Long id) {
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.DRAFT || order.getStatus() == PurchaseOrderStatus.REJECTED, "ORDER_NOT_SUBMITTABLE");
    PurchaseOrderStatus oldStatus = order.getStatus();
    order.setStatus(PurchaseOrderStatus.SUBMITTED);
    order.setSubmittedBy(actor);
    order.setSubmittedByNameSnapshot(actor.getDisplayName());
    order.setSubmittedAt(Instant.now());
    audit("PURCHASE_ORDER", order.getId(), "SUBMITTED", actor, oldStatus.name(), order.getStatus().name(), null);
    return order;
  }

  @Transactional
  public PurchaseOrder approve(Employee actor, Long id) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.SUBMITTED, "ORDER_NOT_APPROVABLE");
    if (order.getSubmittedBy() != null && Objects.equals(order.getSubmittedBy().getId(), actor.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "ORDER_SELF_APPROVAL_FORBIDDEN");
    PurchaseOrderStatus oldStatus = order.getStatus();
    order.setStatus(PurchaseOrderStatus.APPROVED);
    order.setApprovedBy(actor);
    order.setApprovedByNameSnapshot(actor.getDisplayName());
    order.setApprovedAt(Instant.now());
    purchaseOrderLines.findByPurchaseOrderIdOrderByIdAsc(order.getId()).forEach(line -> {
      if (line.getApprovedQuantity() == null) line.setApprovedQuantity(line.getRequestedQuantity());
      line.setLineTotal(lineTotal(line));
    });
    recalculate(order);
    audit("PURCHASE_ORDER", order.getId(), "APPROVED", actor, oldStatus.name(), order.getStatus().name(), null);
    PurchaseOrderPdfDocument document;
    try {
      document = createPdfDocument(order, actor);
    } catch (RuntimeException ex) {
      auditRecorder.recordPdfGenerationFailure(order.getId(), actor, order.getStatus().name(), ex.getMessage());
      throw ex;
    }
    audit("PURCHASE_ORDER", order.getId(), "APPROVED_PDF_GENERATED", actor, null, "v" + document.getVersionNumber(), null, document);
    return order;
  }

  @Transactional
  public PurchaseOrder reject(Employee actor, Long id, RejectOrderRequest request) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.SUBMITTED, "ORDER_NOT_REJECTABLE");
    PurchaseOrderStatus oldStatus = order.getStatus();
    order.setStatus(PurchaseOrderStatus.REJECTED);
    order.setRejectionReason(request.reason().trim());
    order.setRejectedBy(actor);
    order.setRejectedByNameSnapshot(actor.getDisplayName());
    order.setRejectedAt(Instant.now());
    audit("PURCHASE_ORDER", order.getId(), "REJECTED", actor, oldStatus.name(), order.getStatus().name(), request.reason());
    return order;
  }

  @Transactional
  public PurchaseOrder markOrdered(Employee actor, Long id, MarkOrderedRequest request) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.APPROVED, "ORDER_NOT_READY_TO_MARK_ORDERED");
    order.setStatus(PurchaseOrderStatus.ORDERED);
    order.setOrderedBy(actor);
    order.setOrderedAt(Instant.now());
    order.setVendorConfirmationNumber(blankToNull(request.vendorConfirmationNumber()));
    order.setExternalOrderNotes(blankToNull(request.externalOrderNotes()));
    audit("PURCHASE_ORDER", order.getId(), "ORDERED", actor, null, order.getStatus().name(), null);
    return order;
  }

  @Transactional
  public PurchaseOrder receive(Employee actor, Long id, ReceiveOrderRequest request) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.ORDERED || order.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED, "ORDER_NOT_RECEIVABLE");
    for (var item : request.lines()) {
      PurchaseOrderLine line = purchaseOrderLines.findById(item.lineId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_LINE_NOT_FOUND"));
      if (!Objects.equals(line.getPurchaseOrder().getId(), order.getId())) throw new ApiException(HttpStatus.BAD_REQUEST, "ORDER_LINE_MISMATCH");
      BigDecimal now = nonNegative(item.quantityReceivedNow(), "ORDER_NEGATIVE_QUANTITY");
      BigDecimal next = line.getReceivedQuantity().add(now);
      BigDecimal cap = line.getApprovedQuantity() == null ? line.getRequestedQuantity() : line.getApprovedQuantity();
      if (next.compareTo(cap) > 0 && !Boolean.TRUE.equals(item.allowOverReceive())) throw new ApiException(HttpStatus.CONFLICT, "ORDER_RECEIVE_EXCEEDS_APPROVED");
      if (next.compareTo(cap) > 0) audit("PURCHASE_ORDER", order.getId(), "QUANTITY_OVERRIDDEN", actor, cap.toPlainString(), next.toPlainString(), item.note());
      line.setReceivedQuantity(next);
    }
    boolean allReceived = purchaseOrderLines.findByPurchaseOrderIdOrderByIdAsc(order.getId()).stream().allMatch(line -> line.getReceivedQuantity().compareTo(line.getApprovedQuantity() == null ? line.getRequestedQuantity() : line.getApprovedQuantity()) >= 0);
    order.setStatus(allReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
    order.setReceivedBy(actor);
    order.setReceivedAt(Instant.now());
    audit("PURCHASE_ORDER", order.getId(), allReceived ? "RECEIVED" : "PARTIALLY_RECEIVED", actor, null, order.getStatus().name(), null);
    return order;
  }

  @Transactional
  public PurchaseOrder cancel(Employee actor, Long id) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() != PurchaseOrderStatus.RECEIVED, "ORDER_NOT_CANCELLABLE");
    order.setStatus(PurchaseOrderStatus.CANCELLED);
    audit("PURCHASE_ORDER", order.getId(), "CANCELLED", actor, null, order.getStatus().name(), null);
    return order;
  }

  @Transactional
  public PurchaseOrder reopen(Employee actor, Long id) {
    requireBusinessPartner(actor);
    PurchaseOrder order = getOrder(actor, id);
    requireStatus(order.getStatus() == PurchaseOrderStatus.APPROVED || order.getStatus() == PurchaseOrderStatus.REJECTED, "ORDER_NOT_REOPENABLE");
    PurchaseOrderStatus oldStatus = order.getStatus();
    order.setStatus(PurchaseOrderStatus.DRAFT);
    audit("PURCHASE_ORDER", order.getId(), "REOPENED", actor, oldStatus.name(), order.getStatus().name(), null);
    return order;
  }

  public List<PurchaseOrder> listOrders(Employee actor, StoreCode locationCode, PurchaseOrderStatus status, OrderBusiness orderBusiness) {
    requireStoreAccess(actor, locationCode);
    if (orderBusiness != null && status != null) return purchaseOrders.findByLocationCodeAndOrderBusinessAndStatusOrderByBusinessDateDescIdDesc(locationCode, orderBusiness, status);
    if (orderBusiness != null) return purchaseOrders.findByLocationCodeAndOrderBusinessOrderByBusinessDateDescIdDesc(locationCode, orderBusiness);
    return status == null ? purchaseOrders.findByLocationCodeOrderByBusinessDateDescIdDesc(locationCode) : purchaseOrders.findByLocationCodeAndStatusOrderByBusinessDateDescIdDesc(locationCode, status);
  }

  public List<PurchaseOrder> listOrders(Employee actor, StoreCode locationCode, PurchaseOrderStatus status) {
    return listOrders(actor, locationCode, status, null);
  }

  public OrderingDtos.OrderInventoryReferenceResponse orderInventoryReference(Employee actor, StoreCode locationCode, OrderBusiness business) {
    requireStoreAccess(actor, locationCode);
    InventoryBusiness inventoryBusiness = inventoryBusiness(business);
    InventoryCountSession source = inventorySessions.findByLocationCodeAndInventoryBusinessOrderByBusinessDateDescIdDesc(locationCode, inventoryBusiness).stream()
        .filter(this::isEligibleInventorySource)
        .max((left, right) -> inventoryReferenceTime(left).compareTo(inventoryReferenceTime(right)))
        .orElse(null);
    if (source == null) {
      return new OrderingDtos.OrderInventoryReferenceResponse(null, locationCode, business, business.displayName(), null, List.of());
    }
    return new OrderingDtos.OrderInventoryReferenceResponse(
        source.getId(),
        locationCode,
        business,
        business.displayName(),
        inventoryReferenceTime(source),
        inventoryLines(source.getId()).stream()
            .map(line -> new OrderingDtos.OrderInventoryReferenceLineResponse(
                line.getProduct().getId(),
                line.getProduct().getName(),
                key(line.getProduct().getName()),
                line.getProduct().getInventoryUnitLabel() == null ? line.getUnit().name() : line.getProduct().getInventoryUnitLabel(),
                line.getQuantityOnHand()))
            .toList()
    );
  }

  public PurchaseOrder getOrder(Employee actor, Long id) {
    PurchaseOrder order = purchaseOrders.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND"));
    requireStoreAccess(actor, order.getLocationCode());
    return order;
  }

  public List<PurchaseOrderLine> orderLines(Long orderId) {
    return purchaseOrderLines.findByPurchaseOrderIdOrderByIdAsc(orderId);
  }

  public List<InventoryCountLine> inventoryLines(Long sessionId) {
    return inventoryLines.findBySessionIdOrderByProductDisplayOrderAsc(sessionId);
  }

  public byte[] inventoryCountPdf(Employee actor, Long sessionId) {
    InventoryCountSession session = inventorySession(actor, sessionId);
    if (!isEligibleInventorySource(session)) throw new ApiException(HttpStatus.CONFLICT, "INVENTORY_PDF_NOT_EXPORTABLE");
    return inventoryCountPdfService.generate(session, inventoryLines(session.getId()));
  }

  public List<ProductPriceHistory> priceHistory(Employee actor, Long productId) {
    getProduct(actor, productId);
    return priceHistory.findByProductIdOrderByEffectiveDateDescCreatedAtDesc(productId);
  }

  public List<OrderAuditEvent> auditEvents(String entityType, Long entityId) {
    return auditEvents.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId);
  }

  public PurchaseOrderPdfDocument currentPdf(Employee actor, Long orderId) {
    PurchaseOrder order = getOrder(actor, orderId);
    return pdfDocuments.findByPurchaseOrderIdAndCurrentVersionTrue(order.getId()).orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "ORDER_PDF_NOT_READY"));
  }

  public List<PurchaseOrderPdfDocument> pdfVersions(Employee actor, Long orderId) {
    PurchaseOrder order = getOrder(actor, orderId);
    return pdfDocuments.findByPurchaseOrderIdOrderByVersionNumberAsc(order.getId());
  }

  public OrderPlanPdfDocument currentOrderPlanPdf(Employee actor, Long orderPlanId) {
    OrderPlanSession plan = getOrderPlan(actor, orderPlanId);
    return orderPlanPdfDocuments.findByOrderPlanIdAndCurrentVersionTrue(plan.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_PLAN_PDF_NOT_FOUND"));
  }

  private PurchaseOrderPdfDocument createPdfDocument(PurchaseOrder order, Employee approver) {
    pdfDocuments.findByPurchaseOrderIdAndCurrentVersionTrue(order.getId()).ifPresent(existing -> {
      existing.setCurrentVersion(false);
      existing.setSupersededAt(Instant.now());
    });
    int version = (int) pdfDocuments.countByPurchaseOrderId(order.getId()) + 1;
    PurchaseOrderPdfDocument document = pdfService.generate(order, purchaseOrderLines.findByPurchaseOrderIdOrderByIdAsc(order.getId()), approver, version);
    return pdfDocuments.save(document);
  }

  private OrderPlanPdfDocument createOrderPlanPdfDocument(OrderPlanSession plan, Employee approver) {
    orderPlanPdfDocuments.findByOrderPlanIdAndCurrentVersionTrue(plan.getId()).ifPresent(existing -> {
      existing.setCurrentVersion(false);
      existing.setSupersededAt(Instant.now());
    });
    int version = (int) orderPlanPdfDocuments.countByOrderPlanId(plan.getId()) + 1;
    OrderPlanPdfDocument document = orderPlanPdfService.generate(plan, orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()), approver, version);
    return orderPlanPdfDocuments.save(document);
  }

  private void seedOrderPlanLines(OrderPlanSession plan, Employee actor) {
    Map<Long, InventoryCountLine> counted = plan.getSourceInventorySession() == null ? Map.of() : inventoryLines.findBySessionIdOrderByProductDisplayOrderAsc(plan.getSourceInventorySession().getId()).stream()
        .collect(Collectors.toMap(line -> line.getProduct().getId(), line -> line));
    products.findByLocationCodeOrderByDisplayOrderAscNameAsc(plan.getLocationCode()).stream()
        .filter(OrderCatalogProduct::isActive)
        .filter(product -> product.getOrderBusiness() == plan.getOrderBusiness())
        .forEach(product -> {
          InventoryCountLine countLine = counted.get(product.getId());
          BigDecimal count = countLine == null ? BigDecimal.ZERO : countLine.getQuantityOnHand();
          OrderPlanLine line = new OrderPlanLine();
          line.setOrderPlanSession(plan);
          line.setProduct(product);
          line.setVendor(product.getVendor());
          line.setVendorNameSnapshot(product.getVendor().getName());
          line.setProductNameSnapshot(product.getName());
          line.setProductCodeSnapshot(product.getVendorProductCode());
          line.setPackageSpecificationSnapshot(product.getPackageSpecification());
          line.setInventoryUnitSnapshot(product.getInventoryUnit());
          line.setOrderUnitSnapshot(product.getOrderUnit());
          line.setUnitPriceSnapshot(product.getUnitPrice());
          line.setSourceInventoryQuantity(count);
          line.setParLevelSnapshot(product.getParLevel());
          line.setReorderPointSnapshot(product.getReorderPoint());
          line.setPreviousOrderQuantity(null);
          BigDecimal suggested = suggestedQuantity(product.getParLevel(), count);
          line.setSuggestedOrderQuantity(suggested);
          line.setFinalOrderQuantity(suggested);
          line.setUpdatedBy(actor);
          orderPlanLines.save(line);
        });
    audit("ORDER_PLAN", plan.getId(), "SUGGESTED_QUANTITY_GENERATED", actor, null, String.valueOf(orderPlanLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()).size()), null);
  }

  private PurchaseOrder createVendorOrderFromPlan(Employee actor, OrderPlanSession plan, Vendor vendor, List<OrderPlanLine> planLines) {
    PurchaseOrder order = new PurchaseOrder();
    order.setLocationCode(plan.getLocationCode());
    order.setOrderBusiness(orderBusiness(plan.getSourceInventorySession().getInventoryBusiness()));
    order.setVendor(vendor);
    order.setSourceInventorySession(plan.getSourceInventorySession());
    order.setOrderPlanSession(plan);
    order.setBusinessDate(plan.getBusinessDate());
    order.setCreatedBy(actor);
    order.setOrderNumber(nextOrderNumber(plan.getLocationCode(), plan.getBusinessDate(), vendor));
    PurchaseOrder saved = purchaseOrders.save(order);
    for (OrderPlanLine planLine : planLines) {
      PurchaseOrderLine line = new PurchaseOrderLine();
      line.setPurchaseOrder(saved);
      line.setProduct(planLine.getProduct());
      line.setProductNameSnapshot(planLine.getProductNameSnapshot());
      line.setProductCodeSnapshot(planLine.getProductCodeSnapshot());
      line.setPackageSpecificationSnapshot(planLine.getPackageSpecificationSnapshot());
      line.setOrderUnitSnapshot(planLine.getOrderUnitSnapshot());
      line.setUnitPriceSnapshot(money(planLine.getUnitPriceSnapshot()));
      line.setSourceInventoryQuantitySnapshot(planLine.getSourceInventoryQuantity());
      line.setSuggestedOrderQuantity(planLine.getSuggestedOrderQuantity());
      line.setFinalOrderQuantity(planLine.getFinalOrderQuantity());
      line.setCurrentInventoryQuantity(planLine.getSourceInventoryQuantity());
      line.setRequestedQuantity(planLine.getFinalOrderQuantity());
      line.setNotes(planLine.getNotes());
      line.setLineTotal(lineTotal(line));
      purchaseOrderLines.save(line);
    }
    recalculate(saved);
    audit("PURCHASE_ORDER", saved.getId(), "CREATED_FROM_ORDER_PLAN", actor, null, plan.getId().toString(), null);
    return saved;
  }

  private BigDecimal suggestedQuantity(BigDecimal parLevel, BigDecimal countedQuantity) {
    if (parLevel == null) return null;
    BigDecimal suggestion = parLevel.subtract(countedQuantity == null ? BigDecimal.ZERO : countedQuantity);
    return suggestion.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : suggestion;
  }

  private boolean isEligibleInventorySource(InventoryCountSession source) {
    return source.getStatus() == InventoryCountStatus.SUBMITTED
        || source.getStatus() == InventoryCountStatus.REVIEWED
        || source.getStatus() == InventoryCountStatus.LOCKED
        || source.getStatus() == InventoryCountStatus.COMPLETED;
  }

  private void recalculate(PurchaseOrder order) {
    BigDecimal subtotal = purchaseOrderLines.findByPurchaseOrderIdOrderByIdAsc(order.getId()).stream()
        .peek(line -> line.setLineTotal(lineTotal(line)))
        .map(PurchaseOrderLine::getLineTotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
    order.setSubtotal(subtotal);
    order.setTax(money(order.getTax()));
    order.setFees(money(order.getFees()));
    order.setTotal(subtotal.add(order.getTax()).add(order.getFees()).setScale(2, RoundingMode.HALF_UP));
  }

  private BigDecimal lineTotal(PurchaseOrderLine line) {
    BigDecimal quantity = line.getApprovedQuantity() == null ? line.getRequestedQuantity() : line.getApprovedQuantity();
    return quantity.multiply(money(line.getUnitPriceSnapshot())).setScale(2, RoundingMode.HALF_UP);
  }

  private String nextOrderNumber(StoreCode locationCode, LocalDate businessDate, Vendor vendor) {
    String code = vendor.getVendorCode() == null || vendor.getVendorCode().isBlank() ? vendor.getName().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT) : vendor.getVendorCode().toUpperCase(Locale.ROOT);
    if (code.length() > 8) code = code.substring(0, 8);
    long sequence = purchaseOrders.countByLocationCodeAndBusinessDateAndVendorId(locationCode, businessDate, vendor.getId()) + 1;
    String orderNumber;
    do {
      orderNumber = "PO-" + businessDate.toString().replace("-", "") + "-" + code + "-" + String.format("%03d", sequence++);
    } while (purchaseOrders.existsByOrderNumber(orderNumber));
    return orderNumber;
  }

  private void recordPrice(OrderCatalogProduct product, BigDecimal price, LocalDate date, PurchaseOrder order, Employee actor) {
    ProductPriceHistory history = new ProductPriceHistory();
    history.setProduct(product);
    history.setUnitPrice(price);
    history.setEffectiveDate(date);
    history.setPurchaseOrder(order);
    history.setEnteredBy(actor);
    priceHistory.save(history);
  }

  private void audit(String entityType, Long entityId, String action, Employee actor, String oldValue, String newValue, String reason) {
    audit(entityType, entityId, action, actor, oldValue, newValue, reason, null);
  }

  private void audit(String entityType, Long entityId, String action, Employee actor, String oldValue, String newValue, String reason, PurchaseOrderPdfDocument document) {
    if (entityId == null) return;
    OrderAuditEvent event = new OrderAuditEvent();
    event.setEntityType(entityType);
    event.setEntityId(entityId);
    event.setAction(action);
    event.setActor(actor);
    event.setActorNameSnapshot(actor.getDisplayName());
    event.setOldValue(oldValue);
    event.setNewValue(newValue);
    event.setReason(reason);
    if (document != null) {
      event.setPdfDocument(document);
      event.setPdfVersion(document.getVersionNumber());
    }
    auditEvents.save(event);
  }

  private Vendor vendor(Long id) {
    return vendors.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "VENDOR_NOT_FOUND"));
  }

  private OrderCatalogProduct product(Long id) {
    return products.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_PRODUCT_NOT_FOUND"));
  }

  private OrderCatalogProduct inventoryCatalogProduct(Employee actor, Long id) {
    OrderCatalogProduct product = getProduct(actor, id);
    if (product.getInventoryBusiness() == null) throw new ApiException(HttpStatus.NOT_FOUND, "INVENTORY_CATALOG_ITEM_NOT_FOUND");
    return product;
  }

  private OrderCatalogProduct orderCatalogProduct(Employee actor, Long id) {
    OrderCatalogProduct product = getProduct(actor, id);
    if (product.getOrderBusiness() == null) throw new ApiException(HttpStatus.NOT_FOUND, "ORDER_CATALOG_ITEM_NOT_FOUND");
    return product;
  }

  private OrderBusiness orderBusiness(InventoryBusiness business) {
    return business == InventoryBusiness.PAPER_FAN ? OrderBusiness.PAPER_FAN : OrderBusiness.BIANGBIANG_FRONT;
  }

  private InventoryBusiness inventoryBusiness(OrderBusiness business) {
    return business == OrderBusiness.PAPER_FAN ? InventoryBusiness.PAPER_FAN : InventoryBusiness.BIANGBIANG_FRONT;
  }

  private Instant inventoryReferenceTime(InventoryCountSession session) {
    if (session.getCompletedAt() != null) return session.getCompletedAt();
    if (session.getSubmittedAt() != null) return session.getSubmittedAt();
    if (session.getUpdatedAt() != null) return session.getUpdatedAt();
    return session.getCreatedAt();
  }

  private Vendor inventoryCatalogVendor(StoreCode locationCode, String vendorName) {
    String displayName = vendorName == null || vendorName.isBlank() ? "Unknown vendor" : vendorName.trim();
    return vendors.findByLocationCodeAndNormalizedName(locationCode, key(displayName)).orElseGet(() -> {
      Vendor vendor = new Vendor();
      vendor.setLocationCode(locationCode);
      vendor.setName(displayName);
      vendor.setNormalizedName(key(displayName));
      vendor.setActive(true);
      vendor.setDisplayOrder((int) Math.min(Integer.MAX_VALUE, vendors.count() + 1));
      return vendors.save(vendor);
    });
  }

  private CatalogUnit inventoryCatalogUnit(String unit) {
    String normalized = key(unit);
    return switch (normalized) {
      case "bt", "bottle", "bottles" -> CatalogUnit.BOTTLE;
      case "can", "cans" -> CatalogUnit.CAN;
      case "box", "boxes" -> CatalogUnit.BOX;
      case "case", "cases" -> CatalogUnit.CASE;
      case "pack", "packs" -> CatalogUnit.PACK;
      case "ea", "each", "pc", "pcs" -> CatalogUnit.EA;
      default -> CatalogUnit.OTHER;
    };
  }

  private void requireBusinessPartner(Employee actor) {
    if (!authorization.isBusinessPartner(actor)) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_BUSINESS_PARTNER_REQUIRED");
  }

  private void requireOrderPlanDecisionAuthority(Employee actor, OrderPlanSession plan) {
    requireBusinessPartner(actor);
    requireExplicitStoreAccess(actor, plan.getLocationCode());
  }

  private boolean isEditableStatus(OrderPlanStatus status) {
    return status == OrderPlanStatus.DRAFT || status == OrderPlanStatus.IN_PROGRESS || status == OrderPlanStatus.RETURNED || status == OrderPlanStatus.REJECTED;
  }

  private void requireOwnedEditableSession(Employee actor, OrderPlanSession plan) {
    if (!Objects.equals(plan.getCreatedBy().getId(), actor.getId())) throw new ApiException(HttpStatus.FORBIDDEN, "ORDER_SESSION_NOT_OWNED");
  }

  private void requireStoreAccess(Employee actor, StoreCode locationCode) {
    if (actor.getStatus() != com.restaurant.ops.employee.EmployeeStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    if (!authorization.isBusinessPartner(actor) && actor.getHomeStore() != locationCode && !actor.getEligibleStores().contains(locationCode)) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_STORE_REQUIRED");
  }

  private void requireExplicitStoreAccess(Employee actor, StoreCode locationCode) {
    if (actor.getStatus() != com.restaurant.ops.employee.EmployeeStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    if (actor.getHomeStore() != locationCode && !actor.getEligibleStores().contains(locationCode)) throw new ApiException(HttpStatus.FORBIDDEN, "AUTH_STORE_REQUIRED");
  }

  private void requireStatus(boolean condition, String code) {
    if (!condition) throw new ApiException(HttpStatus.CONFLICT, code);
  }

  private BigDecimal nonNegative(BigDecimal value, String code) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) throw new ApiException(HttpStatus.BAD_REQUEST, code);
    return value;
  }

  private BigDecimal money(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : value.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal moneyOrNull(BigDecimal value) {
    return value == null ? null : money(value);
  }

  private String key(String value) {
    return OrderingKeys.key(value);
  }

  private String productKey(String code, String name) {
    return OrderingKeys.productKey(code, name);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
