package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.InventoryCountStatus;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface VendorRepository extends JpaRepository<Vendor, Long> {
  List<Vendor> findByLocationCodeOrderByDisplayOrderAscNameAsc(StoreCode locationCode);
  List<Vendor> findByLocationCodeAndActiveTrueOrderByDisplayOrderAscNameAsc(StoreCode locationCode);
  Optional<Vendor> findByLocationCodeAndNormalizedName(StoreCode locationCode, String normalizedName);
  Optional<Vendor> findByLocationCodeAndSourceKey(StoreCode locationCode, String sourceKey);
}

interface OrderCatalogProductRepository extends JpaRepository<OrderCatalogProduct, Long> {
  List<OrderCatalogProduct> findByLocationCodeOrderByDisplayOrderAscNameAsc(StoreCode locationCode);
  List<OrderCatalogProduct> findByLocationCodeAndInventoryBusinessOrderByDisplayOrderAscNameAsc(StoreCode locationCode, InventoryBusiness inventoryBusiness);
  List<OrderCatalogProduct> findByLocationCodeAndOrderBusinessOrderByDisplayOrderAscNameAsc(StoreCode locationCode, OrderBusiness orderBusiness);
  List<OrderCatalogProduct> findByVendorIdOrderByDisplayOrderAscNameAsc(Long vendorId);
  List<OrderCatalogProduct> findByLocationCodeAndCategoryOrderByDisplayOrderAscNameAsc(StoreCode locationCode, CatalogCategory category);
  Optional<OrderCatalogProduct> findByLocationCodeAndVendorIdAndNormalizedName(StoreCode locationCode, Long vendorId, String normalizedName);
  Optional<OrderCatalogProduct> findByLocationCodeAndVendorIdAndSourceKey(StoreCode locationCode, Long vendorId, String sourceKey);
}

interface InventoryCountSessionRepository extends JpaRepository<InventoryCountSession, Long> {
  List<InventoryCountSession> findByLocationCodeOrderByBusinessDateDescIdDesc(StoreCode locationCode);
  List<InventoryCountSession> findByLocationCodeAndInventoryBusinessOrderByBusinessDateDescIdDesc(StoreCode locationCode, InventoryBusiness inventoryBusiness);
  List<InventoryCountSession> findByLocationCodeAndStatusOrderByBusinessDateDescIdDesc(StoreCode locationCode, InventoryCountStatus status);
}

interface InventoryCountLineRepository extends JpaRepository<InventoryCountLine, Long> {
  List<InventoryCountLine> findBySessionIdOrderByProductDisplayOrderAsc(Long sessionId);
  Optional<InventoryCountLine> findBySessionIdAndProductId(Long sessionId, Long productId);
}

interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
  List<PurchaseOrder> findByLocationCodeOrderByBusinessDateDescIdDesc(StoreCode locationCode);
  List<PurchaseOrder> findByLocationCodeAndOrderBusinessOrderByBusinessDateDescIdDesc(StoreCode locationCode, OrderBusiness orderBusiness);
  List<PurchaseOrder> findByLocationCodeAndStatusOrderByBusinessDateDescIdDesc(StoreCode locationCode, PurchaseOrderStatus status);
  List<PurchaseOrder> findByLocationCodeAndOrderBusinessAndStatusOrderByBusinessDateDescIdDesc(StoreCode locationCode, OrderBusiness orderBusiness, PurchaseOrderStatus status);
  List<PurchaseOrder> findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameAsc(Long orderPlanSessionId);
  long countByLocationCodeAndBusinessDateAndVendorId(StoreCode locationCode, LocalDate businessDate, Long vendorId);
  boolean existsByOrderNumber(String orderNumber);
}

interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
  List<PurchaseOrderLine> findByPurchaseOrderIdOrderByIdAsc(Long purchaseOrderId);
  Optional<PurchaseOrderLine> findByPurchaseOrderIdAndProductId(Long purchaseOrderId, Long productId);
}

interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
  List<ProductPriceHistory> findByProductIdOrderByEffectiveDateDescCreatedAtDesc(Long productId);
}

interface OrderPlanSessionRepository extends JpaRepository<OrderPlanSession, Long> {
  List<OrderPlanSession> findByLocationCodeOrderByBusinessDateDescIdDesc(StoreCode locationCode);
  List<OrderPlanSession> findByLocationCodeAndStatusOrderByBusinessDateDescIdDesc(StoreCode locationCode, OrderingEnums.OrderPlanStatus status);
}

interface OrderPlanLineRepository extends JpaRepository<OrderPlanLine, Long> {
  List<OrderPlanLine> findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(Long orderPlanSessionId);
  List<OrderPlanLine> findByOrderPlanSessionIdAndVendorIdOrderByProductDisplayOrderAscProductNameSnapshotAsc(Long orderPlanSessionId, Long vendorId);
  Optional<OrderPlanLine> findByOrderPlanSessionIdAndProductId(Long orderPlanSessionId, Long productId);
}

interface OrderAuditEventRepository extends JpaRepository<OrderAuditEvent, Long> {
  List<OrderAuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(String entityType, Long entityId);
}

interface PurchaseOrderPdfDocumentRepository extends JpaRepository<PurchaseOrderPdfDocument, Long> {
  List<PurchaseOrderPdfDocument> findByPurchaseOrderIdOrderByVersionNumberAsc(Long purchaseOrderId);
  Optional<PurchaseOrderPdfDocument> findByPurchaseOrderIdAndCurrentVersionTrue(Long purchaseOrderId);
  long countByPurchaseOrderId(Long purchaseOrderId);
}

interface OrderPlanPdfDocumentRepository extends JpaRepository<OrderPlanPdfDocument, Long> {
  List<OrderPlanPdfDocument> findByOrderPlanIdOrderByVersionNumberAsc(Long orderPlanId);
  Optional<OrderPlanPdfDocument> findByOrderPlanIdAndCurrentVersionTrue(Long orderPlanId);
  long countByOrderPlanId(Long orderPlanId);
}
