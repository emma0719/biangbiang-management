package com.restaurant.ops.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.restaurant.ops.auth.AppPrincipal;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventorySessionRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderPlanRequest;
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.RejectOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderPlanPdfHttpIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_order_plan_pdf_http")
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

  @Autowired MockMvc mvc;
  @Autowired OrderingService ordering;
  @Autowired EmployeeRepository employees;
  @Autowired OrderPlanPdfDocumentRepository orderPlanPdfDocuments;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;

  @Test
  void authorizedStoreEmployeeCanViewAndDownloadApprovedOrderPlanPdf() throws Exception {
    Employee manager = employee("pdf-http-manager@example.com", Position.MANAGER, StoreCode.SEATTLE, "7931", EmployeeStatus.ACTIVE);
    Employee creator = employee("pdf-http-creator@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7932", EmployeeStatus.ACTIVE);
    Employee submitter = employee("pdf-http-submitter@example.com", Position.HOST, StoreCode.SEATTLE, "7933", EmployeeStatus.ACTIVE);
    Employee approver = employee("pdf-http-approver@example.com", Position.OWNER, StoreCode.SEATTLE, "7934", EmployeeStatus.ACTIVE);
    approver.setPreferredName("HTTP PDF Approver");
    employees.save(approver);
    Employee reader = employee("pdf-http-reader@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7935", EmployeeStatus.ACTIVE);
    Employee inactive = employee("pdf-http-inactive@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7936", EmployeeStatus.DEACTIVATED);
    Employee redmond = employee("pdf-http-redmond@example.com", Position.BARTENDER, StoreCode.REDMOND, "7937", EmployeeStatus.ACTIVE);

    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "HTTP PDF Vendor", "HPDF", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "HPDF-1", "HTTP PDF Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("7.25"), "USD", new BigDecimal("5"), null, null, true, 1, null), null);

    OrderPlanSession plan = submittedPlan(creator, submitter, manager, product, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 2));
    ordering.approveOrderPlan(approver, plan.getId());
    OrderPlanPdfDocument document = orderPlanPdfDocuments.findByOrderPlanIdAndCurrentVersionTrue(plan.getId()).orElseThrow();
    long countBefore = orderPlanPdfDocuments.countByOrderPlanId(plan.getId());
    String checksumBefore = document.getChecksumSha256();

    mvc.perform(get("/api/order-plans/{id}/pdf", plan.getId()).with(auth(reader)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.documentId").value(document.getId()))
        .andExpect(jsonPath("$.orderPlanId").value(plan.getId()))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.filename").value(document.getFilename()))
        .andExpect(jsonPath("$.mimeType").value("application/pdf"))
        .andExpect(jsonPath("$.byteSize").value(document.getContent().length))
        .andExpect(jsonPath("$.sha256").value(sha256(document.getContent())))
        .andExpect(jsonPath("$.generatedByEmployeeId").value(approver.getId()))
        .andExpect(jsonPath("$.generatedByNameSnapshot").value("HTTP PDF Approver"))
        .andExpect(jsonPath("$.current").value(true))
        .andExpect(jsonPath("$.pdfBytes").doesNotExist())
        .andExpect(jsonPath("$.content").doesNotExist())
        .andExpect(jsonPath("$.generatedBy").doesNotExist())
        .andExpect(jsonPath("$.orderPlan").doesNotExist());

    byte[] viewed = mvc.perform(get("/api/order-plans/{id}/pdf/view", plan.getId()).with(auth(reader)))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + document.getFilename() + "\""))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(document.getContent().length)))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andReturn()
        .getResponse()
        .getContentAsByteArray();
    assertThat(viewed).startsWith("%PDF-".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
    assertThat(viewed).isEqualTo(document.getContent());

    byte[] downloaded = mvc.perform(get("/api/order-plans/{id}/pdf/download", plan.getId()).with(auth(reader)))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFilename() + "\""))
        .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, String.valueOf(document.getContent().length)))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andReturn()
        .getResponse()
        .getContentAsByteArray();
    assertThat(downloaded).isEqualTo(document.getContent());
    assertThat(orderPlanPdfDocuments.countByOrderPlanId(plan.getId())).isEqualTo(countBefore);
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdAndCurrentVersionTrue(plan.getId()).orElseThrow().getChecksumSha256()).isEqualTo(checksumBefore);

    OrderPlanSession completedViaLegacyEndpoint = submittedPlan(creator, submitter, manager, product, LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 4));
    ordering.completeOrderPlan(approver, completedViaLegacyEndpoint.getId());
    mvc.perform(get("/api/order-plans/{id}/pdf", completedViaLegacyEndpoint.getId()).with(auth(reader)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.version").value(1));

    OrderPlanSession draft = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, sourceInventory(creator, manager, product, LocalDate.of(2026, 9, 5)).getId(), LocalDate.of(2026, 9, 6), null, null, null, "draft no pdf"));
    expectPdfNotFound(reader, draft.getId());

    OrderPlanSession submitted = submittedPlan(creator, submitter, manager, product, LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8));
    expectPdfNotFound(reader, submitted.getId());

    OrderPlanSession rejected = submittedPlan(creator, submitter, manager, product, LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 10));
    ordering.rejectOrderPlan(approver, rejected.getId(), new RejectOrderRequest("not approved"));
    expectPdfNotFound(reader, rejected.getId());

    for (String path : List.of("/api/order-plans/{id}/pdf", "/api/order-plans/{id}/pdf/view", "/api/order-plans/{id}/pdf/download")) {
      mvc.perform(get(path, plan.getId()).with(auth(redmond)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("AUTH_STORE_REQUIRED"));
      mvc.perform(get(path, plan.getId()).with(auth(inactive)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("AUTH_ACTIVE_EMPLOYEE_REQUIRED"));
    }
  }

  private void expectPdfNotFound(Employee actor, Long planId) throws Exception {
    mvc.perform(get("/api/order-plans/{id}/pdf", planId).with(auth(actor)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("ORDER_PLAN_PDF_NOT_FOUND"));
  }

  private OrderPlanSession submittedPlan(Employee creator, Employee submitter, Employee manager, OrderCatalogProduct product, LocalDate inventoryDate, LocalDate planDate) {
    InventoryCountSession inventory = sourceInventory(creator, manager, product, inventoryDate);
    OrderPlanSession plan = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, inventory.getId(), planDate, null, null, null, "http pdf plan"));
    ordering.upsertOrderPlanLines(creator, plan.getId(), new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), new BigDecimal("4"), "http final")
    )));
    return ordering.submitOrderPlan(submitter, plan.getId());
  }

  private InventoryCountSession sourceInventory(Employee counter, Employee manager, OrderCatalogProduct product, LocalDate businessDate) {
    InventoryCountSession inventory = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, businessDate, "http pdf inventory"));
    inventory = ordering.upsertInventoryLines(counter, inventory.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), BigDecimal.ONE, CatalogUnit.CASE, "counted")
    )));
    return ordering.reviewInventory(manager, ordering.submitInventory(counter, inventory.getId()).getId());
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor auth(Employee employee) {
    List<SimpleGrantedAuthority> authorities = employee.getPositions().stream()
        .map(position -> new SimpleGrantedAuthority("ROLE_" + position.name()))
        .toList();
    return authentication(new UsernamePasswordAuthenticationToken(new AppPrincipal(employee, authorities), null, authorities));
  }

  private Employee employee(String email, Position position, StoreCode store, String toastPin, EmployeeStatus status) {
    return employees.findByNormalizedEmail(email).orElseGet(() -> {
      Employee employee = new Employee();
      employee.setEnglishName(email);
      employee.setPreferredName(email);
      employee.setNormalizedEmail(email);
      employee.setNormalizedPhone("+1206555" + toastPin);
      employee.setPasswordHash(passwordEncoder.encode("password123"));
      employee.setToastPinHash(tokens.hash(toastPin));
      employee.setToastPinCiphertext(valueProtector.protect(toastPin));
      employee.setHomeStore(store);
      employee.setEligibleStores(EnumSet.of(store));
      employee.setPositions(Set.of(position));
      employee.setStatus(status);
      return employees.save(employee);
    });
  }

  private String sha256(byte[] value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }
}
