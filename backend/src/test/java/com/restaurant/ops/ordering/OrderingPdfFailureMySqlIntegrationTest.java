package com.restaurant.ops.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventorySessionRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderPlanRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class OrderingPdfFailureMySqlIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_ordering_pdf_failure")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", mysql::getJdbcUrl);
    registry.add("spring.datasource.username", mysql::getUsername);
    registry.add("spring.datasource.password", mysql::getPassword);
    registry.add("app.jwt.secret", () -> "01234567890123456789012345678901");
    registry.add("app.toast-pin.encryption-key", () -> "toast-pin-test-key-32-bytes-long");
    registry.add("app.ordering-seed.enabled", () -> "false");
  }

  @Autowired OrderingService ordering;
  @Autowired EmployeeRepository employees;
  @Autowired PurchaseOrderRepository purchaseOrders;
  @Autowired PurchaseOrderPdfDocumentRepository pdfDocuments;
  @Autowired OrderPlanSessionRepository orderPlans;
  @Autowired OrderPlanPdfDocumentRepository orderPlanPdfDocuments;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;
  @MockBean PurchaseOrderPdfService pdfService;
  @MockBean OrderPlanPdfService orderPlanPdfService;

  @Test
  void pdfGenerationFailureRollsBackApprovalAndPersistsFailureAudit() {
    Employee orderer = employee("pdf-failure-orderer@example.com", Position.HOST, "7401");
    Employee approver = employee("pdf-failure-approver@example.com", Position.OWNER, "7402");
    var vendor = ordering.saveVendor(approver, new VendorRequest(StoreCode.SEATTLE, "PDF Failure Vendor", null, null, null, null, null, Set.of(), null, null, true, 1), null);
    var product = ordering.saveProduct(approver, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "FAIL-1", "Failure Rice", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("11.00"), "USD", null, null, null, true, 1, null), null);
    var order = ordering.createOrder(orderer, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, vendor.getId(), null, LocalDate.of(2026, 6, 28), null, null));
    order = ordering.upsertOrderLines(orderer, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(product.getId(), BigDecimal.ONE, BigDecimal.ONE, null, null))));
    order = ordering.submit(orderer, order.getId());
    Long orderId = order.getId();
    when(pdfService.generate(any(PurchaseOrder.class), anyList(), any(Employee.class), anyInt()))
        .thenThrow(new IllegalStateException("PDF_RENDER_FAILED"));

    assertThatThrownBy(() -> ordering.approve(approver, orderId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("PDF_RENDER_FAILED");

    assertThat(purchaseOrders.findById(orderId).orElseThrow().getStatus()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
    assertThat(pdfDocuments.findByPurchaseOrderIdOrderByVersionNumberAsc(orderId)).isEmpty();
    assertThat(ordering.auditEvents("PURCHASE_ORDER", orderId))
        .anySatisfy(event -> {
          assertThat(event.getAction()).isEqualTo("APPROVED_PDF_GENERATION_FAILED");
          assertThat(event.getActorNameSnapshot()).isEqualTo(approver.getDisplayName());
          assertThat(event.getReason()).isEqualTo("PDF_RENDER_FAILED");
        });
  }

  @Test
  void orderPlanPdfGenerationFailureRollsBackApproval() {
    Employee orderer = employee("order-plan-pdf-failure-orderer@example.com", Position.HOST, "7411");
    Employee approver = employee("order-plan-pdf-failure-approver@example.com", Position.OWNER, "7412");
    var vendor = ordering.saveVendor(approver, new VendorRequest(StoreCode.SEATTLE, "Order Plan PDF Failure Vendor", null, null, null, null, null, Set.of(), null, null, true, 1), null);
    var product = ordering.saveProduct(approver, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "OPFAIL-1", "Order Plan Failure Rice", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("11.00"), "USD", new BigDecimal("4"), null, null, true, 1, null), null);
    var inventory = ordering.createInventory(orderer, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 8, 25), "pdf failure count"));
    inventory = ordering.upsertInventoryLines(orderer, inventory.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), BigDecimal.ONE, CatalogUnit.CASE, "counted")
    )));
    inventory = ordering.submitInventory(orderer, inventory.getId());
    var plan = ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, inventory.getId(), LocalDate.of(2026, 8, 26), null, null, null, "pdf failure plan"));
    plan = ordering.upsertOrderPlanLines(orderer, plan.getId(), new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), new BigDecimal("3"), "final")
    )));
    plan = ordering.submitOrderPlan(orderer, plan.getId());
    Long planId = plan.getId();
    when(orderPlanPdfService.generate(any(OrderPlanSession.class), anyList(), any(Employee.class), anyInt()))
        .thenThrow(new IllegalStateException("ORDER_PLAN_PDF_RENDER_FAILED"));

    assertThatThrownBy(() -> ordering.approveOrderPlan(approver, planId))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("ORDER_PLAN_PDF_RENDER_FAILED");

    OrderPlanSession persisted = orderPlans.findById(planId).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(OrderPlanStatus.SUBMITTED);
    assertThat(persisted.getCompletedBy()).isNull();
    assertThat(persisted.getCompletedByNameSnapshot()).isNull();
    assertThat(persisted.getCompletedAt()).isNull();
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(planId)).isEmpty();
  }

  private Employee employee(String email, Position position, String toastPin) {
    return employees.findByNormalizedEmail(email).orElseGet(() -> {
      Employee employee = new Employee();
      employee.setEnglishName(email);
      employee.setPreferredName(email);
      employee.setNormalizedEmail(email);
      employee.setNormalizedPhone("+1206555" + toastPin);
      employee.setPasswordHash(passwordEncoder.encode("password123"));
      employee.setToastPinHash(tokens.hash(toastPin));
      employee.setToastPinCiphertext(valueProtector.protect(toastPin));
      employee.setHomeStore(StoreCode.SEATTLE);
      employee.setEligibleStores(EnumSet.of(StoreCode.SEATTLE));
      employee.setPositions(Set.of(position));
      employee.setStatus(EmployeeStatus.ACTIVE);
      return employees.save(employee);
    });
  }
}
