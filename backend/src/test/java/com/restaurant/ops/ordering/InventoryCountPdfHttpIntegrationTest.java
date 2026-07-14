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
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.EnumSet;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InventoryCountPdfHttpIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_inventory_pdf_http")
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
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;

  @Test
  void authenticatedStoreEmployeeExportsSubmittedInventoryCountPdf() throws Exception {
    Employee manager = employee("inventory-pdf-manager@example.com", Position.MANAGER, StoreCode.SEATTLE, "7951", EmployeeStatus.ACTIVE);
    Employee counter = employee("inventory-pdf-counter@example.com", Position.HOST, StoreCode.SEATTLE, "7952", EmployeeStatus.ACTIVE);
    counter.setPreferredName("Inventory PDF Counter");
    employees.save(counter);
    Employee reader = employee("inventory-pdf-reader@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7953", EmployeeStatus.ACTIVE);
    Employee redmond = employee("inventory-pdf-redmond@example.com", Position.HOST, StoreCode.REDMOND, "7954", EmployeeStatus.ACTIVE);

    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Inventory PDF Vendor", "IPDF", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "IPDF-1", "Inventory PDF Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("3.50"), "USD", null, null, null, true, 1, null), null);

    InventoryCountSession submitted = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 10, 1), "pdf export source"));
    submitted = ordering.upsertInventoryLines(counter, submitted.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), new BigDecimal("0"), CatalogUnit.CASE, "zero is real")
    )));
    submitted = ordering.submitInventory(counter, submitted.getId());

    mvc.perform(get("/api/inventory-counts/{id}/pdf", submitted.getId()))
        .andExpect(status().isForbidden());

    byte[] pdf = mvc.perform(get("/api/inventory-counts/{id}/pdf", submitted.getId()).with(auth(reader)))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"inventory-count-" + submitted.getId() + ".pdf\""))
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andReturn()
        .getResponse()
        .getContentAsByteArray();

    assertThat(pdf).startsWith("%PDF-".getBytes(StandardCharsets.ISO_8859_1));
    assertThat(pdf.length).isGreaterThan(500);
    String text = new String(pdf, StandardCharsets.ISO_8859_1);
    assertThat(text).contains("Inventory Count", "Date:", "Counted by:", "Inventory PDF Counter", "Vendor", "ITEM", "Qt.", "Unit", "Inventory PDF Vendor", "Inventory PDF Product", "0", "CASE", "Page 1 of");

    mvc.perform(get("/api/inventory-counts/{id}/pdf", submitted.getId()).with(auth(redmond)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_STORE_REQUIRED"));

    InventoryCountSession draft = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 10, 2), "draft no pdf"));
    mvc.perform(get("/api/inventory-counts/{id}/pdf", draft.getId()).with(auth(reader)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVENTORY_PDF_NOT_EXPORTABLE"));

    InventoryCountSession inProgress = ordering.startInventory(counter, ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 10, 3), "in progress no pdf")).getId());
    mvc.perform(get("/api/inventory-counts/{id}/pdf", inProgress.getId()).with(auth(reader)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVENTORY_PDF_NOT_EXPORTABLE"));
  }

  private RequestPostProcessor auth(Employee employee) {
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
}
