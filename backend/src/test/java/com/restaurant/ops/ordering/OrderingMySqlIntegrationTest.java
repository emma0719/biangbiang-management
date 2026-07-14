package com.restaurant.ops.ordering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.ops.common.ApiException;
import com.restaurant.ops.employee.Employee;
import com.restaurant.ops.employee.EmployeeRepository;
import com.restaurant.ops.employee.EmployeeStatus;
import com.restaurant.ops.employee.Position;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreatePurchaseOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateInventorySessionRequest;
import com.restaurant.ops.ordering.OrderingDtos.CreateOrderPlanRequest;
import com.restaurant.ops.ordering.OrderingDtos.InventoryTransitionRequest;
import com.restaurant.ops.ordering.OrderingDtos.MarkOrderedRequest;
import com.restaurant.ops.ordering.OrderingDtos.ProductRequest;
import com.restaurant.ops.ordering.OrderingDtos.ReceiveLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.ReceiveOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.RejectOrderRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateInventoryCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpdateOrderCatalogItemRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertInventoryLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertOrderPlanLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLineRequest;
import com.restaurant.ops.ordering.OrderingDtos.UpsertPurchaseOrderLinesRequest;
import com.restaurant.ops.ordering.OrderingDtos.VendorRequest;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.InventoryCountStatus;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderPlanStatus;
import com.restaurant.ops.ordering.OrderingEnums.PurchaseOrderStatus;
import com.restaurant.ops.security.SecureTokenService;
import com.restaurant.ops.security.SensitiveValueProtector;
import com.restaurant.ops.security.JwtService;
import java.math.BigDecimal;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderingMySqlIntegrationTest {
  @Container
  static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("restaurant_ops_ordering")
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

  @Autowired FrontOrderCatalogImporter importer;
  @Autowired OrderingService ordering;
  @Autowired VendorRepository vendors;
  @Autowired OrderCatalogProductRepository products;
  @Autowired InventoryCountSessionRepository inventorySessions;
  @Autowired InventoryCountLineRepository inventoryLines;
  @Autowired PurchaseOrderRepository purchaseOrders;
  @Autowired PurchaseOrderLineRepository orderLines;
  @Autowired PurchaseOrderPdfDocumentRepository pdfDocuments;
  @Autowired OrderPlanPdfDocumentRepository orderPlanPdfDocuments;
  @Autowired OrderPlanSessionRepository orderPlans;
  @Autowired OrderPlanLineRepository planLines;
  @Autowired ProductPriceHistoryRepository priceHistory;
  @Autowired EmployeeRepository employees;
  @Autowired OrderingMapper mapper;
  @Autowired ObjectMapper objectMapper;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired SecureTokenService tokens;
  @Autowired SensitiveValueProtector valueProtector;
  @Autowired Flyway flyway;
  @Autowired JwtService jwtService;
  @Autowired MockMvc mvc;

  @Test
  void catalogImporterIsIdempotentAndPreservesSourceManagedEdits() {
    var first = importer.importCatalog();
    var countAfterFirst = List.of(vendors.count(), products.count());
    long importedVendorCount = vendors.findByLocationCodeOrderByDisplayOrderAscNameAsc(StoreCode.SEATTLE).stream()
        .filter(Vendor::isImportedFromReference)
        .count();
    long importedProductCount = products.findByLocationCodeOrderByDisplayOrderAscNameAsc(StoreCode.SEATTLE).stream()
        .filter(OrderCatalogProduct::isImportedFromReference)
        .count();
    Vendor jfc = vendors.findByLocationCodeAndSourceKey(StoreCode.SEATTLE, "p1-p3-jfc").orElseThrow();
    OrderCatalogProduct sapporo = products.findByLocationCodeAndVendorIdAndSourceKey(StoreCode.SEATTLE, jfc.getId(), "p1-p2-jfc-sapporo-beer").orElseThrow();
    sapporo.setName("Sapporo Beer - edited locally");
    sapporo.setUnitPrice(new BigDecimal("99.99"));
    products.save(sapporo);

    var second = importer.importCatalog();

    assertThat(first.importedVendors()).isGreaterThanOrEqualTo(0);
    assertThat(first.importedProducts()).isGreaterThanOrEqualTo(0);
    assertThat(importedVendorCount).isEqualTo(16);
    assertThat(importedProductCount).isEqualTo(123);
    assertThat(second.importedVendors()).isZero();
    assertThat(second.importedProducts()).isZero();
    assertThat(List.of(vendors.count(), products.count())).isEqualTo(countAfterFirst);
    assertThat(vendors.findByLocationCodeAndNormalizedName(StoreCode.SEATTLE, "wellpack")).isPresent();
    assertThat(vendors.findByLocationCodeAndNormalizedName(StoreCode.SEATTLE, "page 20 supplier")).isEmpty();
    assertThat(jfc.getDeliveryDays()).contains(java.time.DayOfWeek.WEDNESDAY);
    assertThat(products.findById(sapporo.getId()).orElseThrow().getName()).isEqualTo("Sapporo Beer - edited locally");
    assertThat(products.findById(sapporo.getId()).orElseThrow().getUnitPrice()).isEqualByComparingTo("99.99");
    assertThat(products.findByLocationCodeOrderByDisplayOrderAscNameAsc(StoreCode.SEATTLE))
        .anySatisfy(product -> {
          assertThat(product.getVendorProductCode()).isEqualTo("27366");
          assertThat(product.getName()).isEqualTo("Pork Gyoza");
          assertThat(product.getPackageSpecification()).isEqualTo("10/21oz");
          assertThat(product.getUnitPrice()).isEqualByComparingTo("33.00");
          assertThat(product.isImportedFromReference()).isTrue();
          assertThat(product.getSourceKey()).isEqualTo("p4-27366-pork-gyoza");
        })
        .anySatisfy(product -> {
          assertThat(product.getName()).isEqualTo("TACOMA TOFU EXTRA FIRM");
          assertThat(product.getVendorProductCode()).isNull();
          assertThat(product.getPackageSpecification()).isEqualTo("12pc/case");
          assertThat(product.getUnitPrice()).isEqualByComparingTo("12.84");
        })
        .anySatisfy(product -> {
          assertThat(product.getVendor().getName()).isEqualTo("Southern Glazer's");
          assertThat(product.getName()).isEqualTo("Ming River");
        })
        .anySatisfy(product -> {
          assertThat(product.getName()).isEqualTo("Glove Black (S/M/L)");
        });
    assertThat(first.ambiguities()).anyMatch(value -> value.contains("Page 4 has no visible vendor heading"));
  }

  @Test
  void inventoryBusinessSeparatesCatalogCountsAndPdfFromOrderOnlyItems() {
    importer.importCatalog();
    Employee manager = employee("manager-inventory-business@example.com", Position.MANAGER, StoreCode.SEATTLE, "7051");
    Employee counter = employee("counter-inventory-business@example.com", Position.HOST, StoreCode.SEATTLE, "7052");

    Vendor wellpack = vendors.findByLocationCodeAndSourceKey(StoreCode.SEATTLE, "p1-wellpack").orElseThrow();
    OrderCatalogProduct orderOnly = products.findByLocationCodeAndVendorIdAndSourceKey(StoreCode.SEATTLE, wellpack.getId(), "p1-wellpack-milk-tea-cup-iced").orElseThrow();
    Vendor coho = vendors.findByLocationCodeAndSourceKey(StoreCode.SEATTLE, "p1-p3-coho").orElseThrow();
    OrderCatalogProduct front = products.findByLocationCodeAndVendorIdAndSourceKey(StoreCode.SEATTLE, coho.getId(), "p1-p2-coho-jinro-original").orElseThrow();
    OrderCatalogProduct paperFan = products.findByLocationCodeAndVendorIdAndSourceKey(StoreCode.SEATTLE, coho.getId(), "p3-coho-fen-chiew").orElseThrow();

    assertThat(orderOnly.getInventoryBusiness()).isNull();
    assertThat(front.getInventoryBusiness()).isEqualTo(InventoryBusiness.BIANGBIANG_FRONT);
    assertThat(front.getInventoryUnitLabel()).isEqualTo("bt");
    assertThat(paperFan.getInventoryBusiness()).isEqualTo(InventoryBusiness.PAPER_FAN);

    assertThat(ordering.listProducts(manager, StoreCode.SEATTLE, null, null, InventoryBusiness.BIANGBIANG_FRONT, null, true))
        .extracting(OrderCatalogProduct::getName)
        .contains("JINRO SOJU Original")
        .doesNotContain("Milk tea Cup (Iced)", "Fen Chiew");
    assertThat(ordering.listProducts(manager, StoreCode.SEATTLE, null, null, InventoryBusiness.PAPER_FAN, null, true))
        .extracting(OrderCatalogProduct::getName)
        .contains("Fen Chiew")
        .doesNotContain("JINRO SOJU Original", "Milk tea Cup (Iced)");

    InventoryCountSession frontCount = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 20), null, null, null, "front count"));
    frontCount = ordering.upsertInventoryLines(counter, frontCount.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(front.getId(), new BigDecimal("2"), CatalogUnit.BOTTLE, null))));
    frontCount = ordering.submitInventory(counter, frontCount.getId());
    Long frontCountId = frontCount.getId();

    InventoryCountSession paperFanCount = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.PAPER_FAN, LocalDate.of(2026, 7, 21), null, null, null, "paper fan count"));
    paperFanCount = ordering.upsertInventoryLines(counter, paperFanCount.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(paperFan.getId(), new BigDecimal("1"), CatalogUnit.OTHER, null))));
    paperFanCount = ordering.submitInventory(counter, paperFanCount.getId());

    assertThat(ordering.listInventory(manager, StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT)).extracting(InventoryCountSession::getId).contains(frontCount.getId()).doesNotContain(paperFanCount.getId());
    assertThat(ordering.listInventory(manager, StoreCode.SEATTLE, InventoryBusiness.PAPER_FAN)).extracting(InventoryCountSession::getId).contains(paperFanCount.getId()).doesNotContain(frontCount.getId());
    List<Long> allInventoryIds = ordering.listInventory(manager, StoreCode.SEATTLE, null).stream()
        .map(InventoryCountSession::getId)
        .toList();
    assertThat(allInventoryIds).contains(frontCount.getId(), paperFanCount.getId());
    assertThat(allInventoryIds.indexOf(paperFanCount.getId())).isLessThan(allInventoryIds.indexOf(frontCount.getId()));

    String pdfText = new String(ordering.inventoryCountPdf(manager, frontCount.getId()), StandardCharsets.ISO_8859_1);
    assertThat(pdfText).contains("Inventory Count", "Business:", "BiangBiang Front", "Vendor", "ITEM", "Qt.", "Unit", "JINRO SOJU Original", "BOTTLE");
    assertThat(pdfText).doesNotContain("Milk tea Cup", "Fen Chiew", "Paper Fan");

    assertThatThrownBy(() -> ordering.upsertInventoryLines(counter, frontCountId, new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(paperFan.getId(), BigDecimal.ONE, CatalogUnit.OTHER, null)))))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_EDITABLE");

    InventoryCountSession newFrontCount = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 22), null, null, null, "mismatch count"));
    assertThatThrownBy(() -> ordering.upsertInventoryLines(counter, newFrontCount.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(paperFan.getId(), BigDecimal.ONE, CatalogUnit.OTHER, null)))))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_PRODUCT_BUSINESS_MISMATCH");
  }

  @Test
  void businessPartnerManagesInventoryCatalogWithSoftDeleteAndStoreIsolation() {
    importer.importCatalog();
    Employee alex = employee("alex-inventory-catalog@example.com", Position.MANAGER, StoreCode.SEATTLE, "7061");
    Employee ordinary = employee("ordinary-inventory-catalog@example.com", Position.HOST, StoreCode.SEATTLE, "7062");

    OrderCatalogProduct tsingdao = ordering.createInventoryCatalogItem(alex,
        new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, "Tsingdao beer", "bt", null));
    assertThat(tsingdao.getInventoryBusiness()).isEqualTo(InventoryBusiness.BIANGBIANG_FRONT);
    assertThat(tsingdao.getInventoryUnit()).isEqualTo(CatalogUnit.BOTTLE);
    assertThat(tsingdao.getInventoryUnitLabel()).isEqualTo("bt");
    assertThat(tsingdao.getVendor().getName()).isEqualTo("Unknown vendor");
    assertThat(tsingdao.isActive()).isTrue();

    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, InventoryBusiness.BIANGBIANG_FRONT, null, true))
        .extracting(OrderCatalogProduct::getName)
        .contains("Tsingdao beer")
        .doesNotContain("gunpowder gin");
    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, InventoryBusiness.PAPER_FAN, null, true))
        .extracting(OrderCatalogProduct::getName)
        .doesNotContain("Tsingdao beer");

    assertThatThrownBy(() -> ordering.createInventoryCatalogItem(ordinary,
        new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, "Staff beer", "bt", null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.updateInventoryCatalogItem(ordinary, tsingdao.getId(),
        new UpdateInventoryCatalogItemRequest("Tsingdao beer edited", "bt", null, true)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.deactivateInventoryCatalogItem(ordinary, tsingdao.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    OrderCatalogProduct yuzu = products.findByLocationCodeAndVendorIdAndSourceKey(StoreCode.SEATTLE,
        vendors.findByLocationCodeAndSourceKey(StoreCode.SEATTLE, "p1-p3-southern-glazers").orElseThrow().getId(),
        "p1-p2-sg-yuzu-sparkling-sake").orElseThrow();
    InventoryCountSession historicCount = ordering.createInventory(ordinary,
        new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 30), null, null, null, "historic yuzu"));
    historicCount = ordering.upsertInventoryLines(ordinary, historicCount.getId(),
        new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(yuzu.getId(), BigDecimal.ONE, CatalogUnit.BOTTLE, null))));
    historicCount = ordering.submitInventory(ordinary, historicCount.getId());
    Long historicCountId = historicCount.getId();
    Long yuzuId = yuzu.getId();

    OrderCatalogProduct deactivatedYuzu = ordering.deactivateInventoryCatalogItem(alex, yuzuId);
    assertThat(deactivatedYuzu.isActive()).isFalse();
    assertThat(products.findById(yuzuId)).isPresent();
    assertThat(inventoryLines.findBySessionIdOrderByProductDisplayOrderAsc(historicCountId))
        .extracting(line -> line.getProduct().getId())
        .contains(yuzuId);
    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, InventoryBusiness.BIANGBIANG_FRONT, null, true))
        .extracting(OrderCatalogProduct::getName)
        .doesNotContain("Yuzu Sparkling Sake");
    String historicPdf = new String(ordering.inventoryCountPdf(alex, historicCountId), StandardCharsets.ISO_8859_1);
    assertThat(historicPdf).contains("Yuzu Sparkling Sake");

    OrderCatalogProduct gunpowder = ordering.createInventoryCatalogItem(alex,
        new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.PAPER_FAN, "gunpowder gin", "bt", ""));
    assertThat(gunpowder.getInventoryBusiness()).isEqualTo(InventoryBusiness.PAPER_FAN);
    assertThat(gunpowder.getVendor().getName()).isEqualTo("Unknown vendor");
    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, InventoryBusiness.PAPER_FAN, null, true))
        .extracting(OrderCatalogProduct::getName)
        .contains("gunpowder gin")
        .doesNotContain("Tsingdao beer");
    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, InventoryBusiness.BIANGBIANG_FRONT, null, true))
        .extracting(OrderCatalogProduct::getName)
        .doesNotContain("gunpowder gin");

    InventoryCountSession frontCount = ordering.createInventory(ordinary,
        new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 31), null, null, null, "front mismatch"));
    assertThatThrownBy(() -> ordering.upsertInventoryLines(ordinary, frontCount.getId(),
        new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(gunpowder.getId(), BigDecimal.ONE, CatalogUnit.BOTTLE, null)))))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_PRODUCT_BUSINESS_MISMATCH");

    assertThat(ordering.listProducts(alex, StoreCode.SEATTLE, null, null, null, null, true))
        .filteredOn(product -> product.getInventoryBusiness() == null)
        .extracting(OrderCatalogProduct::getName)
        .contains("Milk tea Cup (Iced)");
  }

  @Test
  void inventoryCatalogHttpEndpointsAllowBusinessPartnersAndRejectRegularEmployees() throws Exception {
    importer.importCatalog();
    Employee alex = employee("alex-inventory-catalog-http@example.com", Position.MANAGER, StoreCode.SEATTLE, "7063");
    Employee ordinary = employee("ordinary-inventory-catalog-http@example.com", Position.HOST, StoreCode.SEATTLE, "7064");

    String alexAuth = "Bearer " + jwtService.createAccessToken(alex);
    String ordinaryAuth = "Bearer " + jwtService.createAccessToken(ordinary);
    String createBody = objectMapper.writeValueAsString(new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, "Tsingdao beer HTTP", "bt", null));

    String created = mvc.perform(post("/api/inventory-catalog")
            .header(HttpHeaders.AUTHORIZATION, alexAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tsingdao beer HTTP"))
        .andExpect(jsonPath("$.inventoryBusiness").value("BIANGBIANG_FRONT"))
        .andExpect(jsonPath("$.inventoryUnit").value("BOTTLE"))
        .andExpect(jsonPath("$.inventoryUnitLabel").value("bt"))
        .andExpect(jsonPath("$.vendorName").value("Unknown vendor"))
        .andReturn().getResponse().getContentAsString();
    Long createdId = objectMapper.readTree(created).get("id").asLong();

    mvc.perform(get("/api/inventory-catalog")
            .header(HttpHeaders.AUTHORIZATION, alexAuth)
            .param("locationCode", "SEATTLE")
            .param("business", "BIANGBIANG_FRONT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'Tsingdao beer HTTP')]").exists());

    mvc.perform(get("/api/inventory-catalog")
            .header(HttpHeaders.AUTHORIZATION, alexAuth)
            .param("locationCode", "SEATTLE")
            .param("business", "PAPER_FAN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'Tsingdao beer HTTP')]").doesNotExist());

    mvc.perform(post("/api/inventory-catalog")
            .header(HttpHeaders.AUTHORIZATION, ordinaryAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_BUSINESS_PARTNER_REQUIRED"));

    String updateBody = objectMapper.writeValueAsString(new UpdateInventoryCatalogItemRequest("Tsingdao lager", "bt", "", true));
    mvc.perform(patch("/api/inventory-catalog/{id}", createdId)
            .header(HttpHeaders.AUTHORIZATION, alexAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Tsingdao lager"));
    mvc.perform(patch("/api/inventory-catalog/{id}", createdId)
            .header(HttpHeaders.AUTHORIZATION, ordinaryAuth)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateBody))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_BUSINESS_PARTNER_REQUIRED"));

    mvc.perform(delete("/api/inventory-catalog/{id}", createdId)
            .header(HttpHeaders.AUTHORIZATION, ordinaryAuth))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH_BUSINESS_PARTNER_REQUIRED"));
    mvc.perform(delete("/api/inventory-catalog/{id}", createdId)
            .header(HttpHeaders.AUTHORIZATION, alexAuth))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));
  }

  @Test
  void orderBusinessScopesCatalogOrdersLatestInventoryReferenceAndPdfHeader() {
    Employee manager = employee("manager-order-business@example.com", Position.MANAGER, StoreCode.SEATTLE, "7065");
    Employee host = employee("host-order-business@example.com", Position.HOST, StoreCode.SEATTLE, "7066");

    OrderCatalogProduct frontOrderItem = ordering.createOrderCatalogItem(manager, new CreateOrderCatalogItemRequest(StoreCode.SEATTLE, OrderBusiness.BIANGBIANG_FRONT, "Shared Soy Sauce", "case", "Front Vendor"));
    OrderCatalogProduct paperFanOrderItem = ordering.createOrderCatalogItem(manager, new CreateOrderCatalogItemRequest(StoreCode.SEATTLE, OrderBusiness.PAPER_FAN, "Paper Cups", "box", "Paper Vendor"));
    OrderCatalogProduct frontInventoryItem = ordering.createInventoryCatalogItem(manager, new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, "Shared Soy Sauce", "case", "Inventory Vendor"));
    OrderCatalogProduct draftOnlyInventoryItem = ordering.createInventoryCatalogItem(manager, new CreateInventoryCatalogItemRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, "Draft Only", "case", "Inventory Vendor"));

    assertThat(ordering.listProducts(host, StoreCode.SEATTLE, null, null, null, OrderBusiness.BIANGBIANG_FRONT, null, true))
        .extracting(OrderCatalogProduct::getId)
        .contains(frontOrderItem.getId())
        .doesNotContain(paperFanOrderItem.getId());
    assertThat(ordering.listProducts(host, StoreCode.SEATTLE, null, null, InventoryBusiness.BIANGBIANG_FRONT, null, null, true))
        .extracting(OrderCatalogProduct::getId)
        .contains(frontInventoryItem.getId())
        .doesNotContain(frontOrderItem.getId());

    var draft = ordering.createInventory(host, new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 1), null, null, null, "draft reference ignored"));
    ordering.upsertInventoryLines(host, draft.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(draftOnlyInventoryItem.getId(), new BigDecimal("99"), CatalogUnit.CASE, "draft"))));

    var submitted = ordering.createInventory(host, new CreateInventorySessionRequest(StoreCode.SEATTLE, InventoryBusiness.BIANGBIANG_FRONT, LocalDate.of(2026, 7, 2), null, null, null, "submitted reference"));
    submitted = ordering.upsertInventoryLines(host, submitted.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(frontInventoryItem.getId(), new BigDecimal("12"), CatalogUnit.CASE, "submitted"))));
    submitted = ordering.submitInventory(host, submitted.getId());

    var reference = ordering.orderInventoryReference(host, StoreCode.SEATTLE, OrderBusiness.BIANGBIANG_FRONT);
    assertThat(reference.inventoryCountId()).isEqualTo(submitted.getId());
    assertThat(reference.lines()).singleElement().satisfies(line -> {
      assertThat(line.itemName()).isEqualTo("Shared Soy Sauce");
      assertThat(line.quantity()).isEqualByComparingTo("12");
    });

    Vendor frontVendor = frontOrderItem.getVendor();
    var order = ordering.createOrder(host, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, frontVendor.getId(), null, LocalDate.of(2026, 7, 3), null, null, OrderBusiness.BIANGBIANG_FRONT));
    order = ordering.upsertOrderLines(host, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(frontOrderItem.getId(), null, new BigDecimal("3"), null, "front order"))));
    assertThat(order.getOrderBusiness()).isEqualTo(OrderBusiness.BIANGBIANG_FRONT);
    var frontOrderId = order.getId();
    assertThatThrownBy(() -> ordering.upsertOrderLines(host, frontOrderId, new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(paperFanOrderItem.getId(), null, BigDecimal.ONE, null, "wrong business")))))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PRODUCT_BUSINESS_MISMATCH");

    order = ordering.submit(host, order.getId());
    order = ordering.approve(manager, order.getId());
    assertThat(ordering.listOrders(host, StoreCode.SEATTLE, null, OrderBusiness.BIANGBIANG_FRONT)).extracting(PurchaseOrder::getId).contains(order.getId());
    assertThat(ordering.listOrders(host, StoreCode.SEATTLE, null, OrderBusiness.PAPER_FAN)).extracting(PurchaseOrder::getId).doesNotContain(order.getId());
    String pdfText = new String(pdfDocuments.findByPurchaseOrderIdOrderByVersionNumberAsc(order.getId()).getFirst().getContent(), StandardCharsets.ISO_8859_1);
    assertThat(pdfText).contains("Business: BiangBiang Front");

    assertThatThrownBy(() -> ordering.createOrderCatalogItem(host, new CreateOrderCatalogItemRequest(StoreCode.SEATTLE, OrderBusiness.BIANGBIANG_FRONT, "Forbidden", "case", null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    OrderCatalogProduct updated = ordering.updateOrderCatalogItem(manager, frontOrderItem.getId(), new UpdateOrderCatalogItemRequest("Shared Soy Sauce Updated", "case", null, true));
    assertThat(updated.getVendor().getName()).isEqualTo("Unknown vendor");
    assertThat(ordering.deactivateOrderCatalogItem(manager, updated.getId()).isActive()).isFalse();
  }

  @Test
  void purchaseOrderLifecycleCalculatesTotalsAndReceivesPartials() {
    Employee manager = employee("manager-ordering@example.com", Position.MANAGER, StoreCode.SEATTLE, "7101");
    Employee host = employee("host-ordering@example.com", Position.HOST, StoreCode.SEATTLE, "7102");
    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Lifecycle Vendor", null, null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "LV-1", "Lifecycle Noodles", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "12 bags", new BigDecimal("4.235"), "USD", null, null, null, true, 1, null), null);
    product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "LV-1", "Lifecycle Noodles", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "12 bags", new BigDecimal("4.235"), "USD", null, null, null, true, 1, null), product.getId());
    Long productId = product.getId();
    assertThat(priceHistory.findByProductIdOrderByEffectiveDateDescCreatedAtDesc(product.getId())).hasSize(1);

    var order = ordering.createOrder(host, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, vendor.getId(), null, LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 18), "Call vendor"));
    order = ordering.upsertOrderLines(host, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(product.getId(), new BigDecimal("3"), new BigDecimal("2"), null, "Need for weekend"))));
    assertThat(order.getSubtotal()).isEqualByComparingTo("8.48");
    assertThat(order.getTotal()).isEqualByComparingTo("8.48");

    order = ordering.submit(host, order.getId());
    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.SUBMITTED);
    Long submittedOrderId = order.getId();
    assertThatThrownBy(() -> ordering.receive(manager, submittedOrderId, new ReceiveOrderRequest(List.of())))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_NOT_RECEIVABLE");
    order = ordering.approve(manager, order.getId());
    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.APPROVED);
    assertThat(order.getSubtotal()).isEqualByComparingTo("8.48");
    var approvedOrder = order;
    assertThatThrownBy(() -> ordering.upsertOrderLines(host, approvedOrder.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(productId, null, new BigDecimal("2"), new BigDecimal("1"), null)))))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    order = ordering.upsertOrderLines(manager, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(productId, null, new BigDecimal("2"), new BigDecimal("2"), "manager confirmed"))));
    assertThat(order.getSubtotal()).isEqualByComparingTo("8.48");

    order = ordering.markOrdered(manager, order.getId(), new MarkOrderedRequest("CONF-1", "Placed by phone"));
    Long lineId = orderLines.findByPurchaseOrderIdOrderByIdAsc(order.getId()).getFirst().getId();
    order = ordering.receive(manager, order.getId(), new ReceiveOrderRequest(List.of(new ReceiveLineRequest(lineId, new BigDecimal("1"), false, "one case arrived"))));
    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
    order = ordering.receive(manager, order.getId(), new ReceiveOrderRequest(List.of(new ReceiveLineRequest(lineId, new BigDecimal("1"), false, "remaining case arrived"))));
    assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.RECEIVED);
    assertThat(ordering.auditEvents("PURCHASE_ORDER", order.getId())).extracting(OrderAuditEvent::getAction).contains("CREATED", "SUBMITTED", "APPROVED", "ORDERED", "PARTIALLY_RECEIVED", "RECEIVED");
  }

  @Test
  void authorizationPreventsCrossLocationAccessAndSelfApproval() {
    Employee manager = employee("manager-self@example.com", Position.MANAGER, StoreCode.SEATTLE, "7201");
    Employee host = employee("redmond-host@example.com", Position.HOST, StoreCode.REDMOND, "7202");
    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Self Approval Vendor", null, null, null, null, null, Set.of(), null, null, true, 1), null);

    var order = ordering.createOrder(manager, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, vendor.getId(), null, LocalDate.of(2026, 6, 16), null, null));
    ordering.submit(manager, order.getId());
    assertThatThrownBy(() -> ordering.approve(manager, order.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_SELF_APPROVAL_FORBIDDEN");

    assertThatThrownBy(() -> ordering.listVendors(host, StoreCode.SEATTLE, false))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
  }

  @Test
  void operationalEmployeesSubmitAndBusinessPartnerApprovalGeneratesImmutableVersionedPdf() {
    Employee counter = employee("counter-ordering@example.com", Position.HOST, StoreCode.SEATTLE, "7301");
    Employee orderer = employee("orderer-ordering@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7302");
    Employee approver = employee("approver-ordering@example.com", Position.FINANCIAL_MANAGER, StoreCode.SEATTLE, "7303");
    Employee otherPartner = employee("other-partner-ordering@example.com", Position.OWNER, StoreCode.SEATTLE, "7304");
    Employee crossStore = employee("cross-store-ordering@example.com", Position.HOST, StoreCode.REDMOND, "7305");

    Vendor vendor = ordering.saveVendor(approver, new VendorRequest(StoreCode.SEATTLE, "PDF Vendor", "PDFV", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(approver, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "PDF-1", "PDF Noodles", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "10 bags", new BigDecimal("5.00"), "USD", new BigDecimal("8"), null, null, true, 1, null), null);

    var count = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 6, 25), "inventory day"));
    count = ordering.upsertInventoryLines(counter, count.getId(), new UpsertInventoryLinesRequest(List.of(new UpsertInventoryLineRequest(product.getId(), new BigDecimal("3"), CatalogUnit.CASE, "counted"))));
    count = ordering.completeInventory(counter, count.getId());
    assertThat(count.getCompletedByNameSnapshot()).isEqualTo(counter.getDisplayName());

    var order = ordering.createOrder(orderer, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, vendor.getId(), count.getId(), LocalDate.of(2026, 6, 26), null, null));
    order = ordering.upsertOrderLines(orderer, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(product.getId(), new BigDecimal("3"), new BigDecimal("5"), null, "submit five"))));
    order = ordering.submit(orderer, order.getId());
    assertThat(order.getSubmittedBy().getId()).isEqualTo(orderer.getId());
    assertThat(order.getSubmittedByNameSnapshot()).isEqualTo(orderer.getDisplayName());

    Long orderId = order.getId();
    assertThatThrownBy(() -> ordering.approve(orderer, orderId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    var selfSubmitted = ordering.createOrder(approver, new CreatePurchaseOrderRequest(StoreCode.SEATTLE, vendor.getId(), null, LocalDate.of(2026, 6, 27), null, null));
    ordering.submit(approver, selfSubmitted.getId());
    assertThatThrownBy(() -> ordering.approve(approver, selfSubmitted.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_SELF_APPROVAL_FORBIDDEN");

    order = ordering.approve(approver, order.getId());
    assertThat(order.getApprovedByNameSnapshot()).isEqualTo(approver.getDisplayName());
    List<PurchaseOrderPdfDocument> versions = pdfDocuments.findByPurchaseOrderIdOrderByVersionNumberAsc(order.getId());
    assertThat(versions).hasSize(1);
    String pdfText = new String(versions.getFirst().getContent(), java.nio.charset.StandardCharsets.ISO_8859_1);
    assertThat(versions.getFirst().getMimeType()).isEqualTo("application/pdf");
    assertThat(pdfText).contains(order.getOrderNumber(), orderer.getDisplayName(), approver.getDisplayName(), "Vendor section: PDF Vendor", "PDF Noodles", "5");

    product.setName("PDF Noodles Renamed");
    products.save(product);
    orderer.setPreferredName("Changed Orderer");
    employees.save(orderer);
    String historicalPdfText = new String(ordering.currentPdf(approver, order.getId()).getContent(), java.nio.charset.StandardCharsets.ISO_8859_1);
    assertThat(historicalPdfText).contains("PDF Noodles", "orderer-ordering@example.com").doesNotContain("PDF Noodles Renamed", "Changed Orderer");

    assertThatThrownBy(() -> ordering.currentPdf(crossStore, orderId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");

    order = ordering.reopen(otherPartner, order.getId());
    order = ordering.upsertOrderLines(otherPartner, order.getId(), new UpsertPurchaseOrderLinesRequest(List.of(new UpsertPurchaseOrderLineRequest(product.getId(), new BigDecimal("3"), new BigDecimal("6"), null, "version two"))));
    order = ordering.submit(orderer, order.getId());
    order = ordering.approve(approver, order.getId());
    versions = pdfDocuments.findByPurchaseOrderIdOrderByVersionNumberAsc(order.getId());
    assertThat(versions).hasSize(2);
    assertThat(versions).extracting(PurchaseOrderPdfDocument::getVersionNumber).containsExactly(1, 2);
    assertThat(versions.getFirst().isCurrentVersion()).isFalse();
    assertThat(versions.get(1).isCurrentVersion()).isTrue();
    assertThat(ordering.auditEvents("PURCHASE_ORDER", order.getId())).extracting(OrderAuditEvent::getAction)
        .contains("SUBMITTED", "APPROVED", "APPROVED_PDF_GENERATED", "REOPENED");
  }

  @Test
  void flywayAndInventoryLifecycleRejectIllegalTransitionsWithoutImplicitOrdering() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("9");

    Employee counter = employee("counter-lifecycle@example.com", Position.HOST, StoreCode.SEATTLE, "7401");
    Employee manager = employee("manager-lifecycle@example.com", Position.MANAGER, StoreCode.SEATTLE, "7402");
    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Inventory Lifecycle Vendor", "ILV", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "IL-1", "Inventory Lifecycle Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "6 bags", new BigDecimal("11.25"), "USD", new BigDecimal("5"), null, null, true, 1, null), null);

    long purchaseOrdersBeforeSubmit = purchaseOrders.count();
    long orderPlansBeforeSubmit = orderPlans.count();

    var count = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 6, 28), "lifecycle count"));
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.DRAFT);
    count = ordering.startInventory(counter, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.IN_PROGRESS);
    Long countId = count.getId();
    assertThatThrownBy(() -> ordering.startInventory(counter, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_STARTABLE");

    count = ordering.upsertInventoryLines(counter, count.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), new BigDecimal("2"), CatalogUnit.CASE, "counted")
    )));
    count = ordering.submitInventory(counter, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.SUBMITTED);
    assertThat(count.getSubmittedBy().getId()).isEqualTo(counter.getId());
    assertThat(purchaseOrders.count()).isEqualTo(purchaseOrdersBeforeSubmit);
    assertThat(orderPlans.count()).isEqualTo(orderPlansBeforeSubmit);
    assertThatThrownBy(() -> ordering.submitInventory(counter, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_SUBMITTABLE");
    assertThatThrownBy(() -> ordering.reviewInventory(counter, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    count = ordering.reviewInventory(manager, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.REVIEWED);
    assertThat(count.getReviewedBy().getId()).isEqualTo(manager.getId());
    assertThatThrownBy(() -> ordering.reviewInventory(manager, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_REVIEWABLE");

    count = ordering.lockInventory(manager, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.LOCKED);
    assertThat(count.getLockedBy().getId()).isEqualTo(manager.getId());
    assertThatThrownBy(() -> ordering.cancelInventory(counter, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_CANCELLABLE");
    assertThatThrownBy(() -> ordering.upsertInventoryLines(counter, countId, new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), new BigDecimal("3"), CatalogUnit.CASE, "late edit")
    ))))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_EDITABLE");

    var cancellable = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 6, 29), "cancel before submit"));
    cancellable = ordering.cancelInventory(counter, cancellable.getId());
    assertThat(cancellable.getStatus()).isEqualTo(InventoryCountStatus.CANCELLED);
    Long cancellableId = cancellable.getId();
    assertThatThrownBy(() -> ordering.startInventory(counter, cancellableId))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_NOT_STARTABLE");
  }

  @Test
  void separatedInventoryAndOrderPlanningGeneratesMultiVendorOrders() {
    Employee counter = employee("counter-plan@example.com", Position.HOST, StoreCode.SEATTLE, "7501");
    Employee orderer = employee("orderer-plan@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7502");
    Employee manager = employee("manager-plan@example.com", Position.MANAGER, StoreCode.SEATTLE, "7503");
    Employee redmond = employee("redmond-plan@example.com", Position.HOST, StoreCode.REDMOND, "7504");
    Vendor wellpack = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Plan Wellpack", "PW", null, null, null, null, Set.of(), null, null, true, 1), null);
    Vendor jfc = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Plan JFC", "PJFC", null, null, null, null, Set.of(), null, null, true, 2), null);
    OrderCatalogProduct gloves = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, wellpack.getId(), "G-1", "Plan Gloves", null, CatalogCategory.PACKAGING, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "1000 ct", new BigDecimal("10.00"), "USD", new BigDecimal("10"), null, null, true, 1, null), null);
    OrderCatalogProduct rice = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, jfc.getId(), "R-1", "Plan Rice", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.BAG, null, CatalogUnit.BAG, null, "50 lb", new BigDecimal("20.00"), "USD", null, null, null, true, 1, null), null);

    var draft = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 6, 20), "draft source"));
    Long draftId = draft.getId();
    assertThatThrownBy(() -> ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, draftId, LocalDate.of(2026, 6, 22), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_INVENTORY_NOT_ELIGIBLE");
    draft = ordering.startInventory(counter, draft.getId());
    var inProgressDraft = draft;
    assertThatThrownBy(() -> ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, inProgressDraft.getId(), LocalDate.of(2026, 6, 22), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_INVENTORY_NOT_ELIGIBLE");

    var count = ordering.createInventory(manager, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 6, 21), counter.getId(), LocalDate.of(2026, 6, 21), null, "inventory day"));
    count = ordering.upsertInventoryLines(counter, count.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(gloves.getId(), new BigDecimal("4"), CatalogUnit.CASE, "counted gloves"),
        new UpsertInventoryLineRequest(rice.getId(), new BigDecimal("2"), CatalogUnit.BAG, "counted rice")
    )));
    count = ordering.submitInventory(counter, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.SUBMITTED);
    assertThat(count.getCompletedByNameSnapshot()).isEqualTo(counter.getDisplayName());
    long purchaseOrdersAfterInventorySubmit = purchaseOrders.count();
    long orderPlansAfterInventorySubmit = orderPlans.count();

    Long countId = count.getId();
    assertThatThrownBy(() -> ordering.createOrderPlan(redmond, new CreateOrderPlanRequest(StoreCode.SEATTLE, countId, LocalDate.of(2026, 6, 22), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");

    var plan = ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, count.getId(), LocalDate.of(2026, 6, 23), null, LocalDate.of(2026, 6, 23), null, "order day"));
    assertThat(purchaseOrders.count()).isEqualTo(purchaseOrdersAfterInventorySubmit);
    assertThat(orderPlans.count()).isEqualTo(orderPlansAfterInventorySubmit + 1);
    assertThat(plan.getInventoryBusinessDate()).isEqualTo(LocalDate.of(2026, 6, 21));
    assertThat(plan.getInventoryCompletedByNameSnapshot()).isEqualTo(counter.getDisplayName());
    assertThat(plan.getBusinessDate()).isEqualTo(LocalDate.of(2026, 6, 23));
    assertThat(plan.getCreatedBy().getId()).isEqualTo(orderer.getId());
    plan = ordering.startOrderPlan(orderer, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.IN_PROGRESS);
    Long startedPlanId = plan.getId();
    assertThatThrownBy(() -> ordering.startOrderPlan(orderer, startedPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_STARTABLE");
    List<OrderPlanLine> lines = planLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId());
    assertThat(lines).anySatisfy(line -> {
      assertThat(line.getProductNameSnapshot()).isEqualTo("Plan Gloves");
      assertThat(line.getSourceInventoryQuantity()).isEqualByComparingTo("4");
      assertThat(line.getSuggestedOrderQuantity()).isEqualByComparingTo("6");
    }).anySatisfy(line -> {
      assertThat(line.getProductNameSnapshot()).isEqualTo("Plan Rice");
      assertThat(line.getSuggestedOrderQuantity()).isNull();
    });

    var finalQuantities = new java.util.ArrayList<UpsertOrderPlanLineRequest>();
    for (OrderPlanLine line : lines) {
      BigDecimal quantity = BigDecimal.ZERO;
      String note = "outside this scenario";
      if (line.getProduct().getId().equals(gloves.getId())) {
        quantity = new BigDecimal("7");
        note = "override for weekend";
      } else if (line.getProduct().getId().equals(rice.getId())) {
        quantity = new BigDecimal("2");
        note = "manual rice";
      }
      finalQuantities.add(new UpsertOrderPlanLineRequest(line.getProduct().getId(), quantity, note));
    }
    ordering.upsertOrderPlanLines(orderer, plan.getId(), new UpsertOrderPlanLinesRequest(finalQuantities));
    var vendorOrders = ordering.generateVendorOrders(orderer, plan.getId());
    Long submittedCountId = count.getId();
    Long planId = plan.getId();
    assertThat(vendorOrders).hasSize(2);
    assertThat(vendorOrders).extracting(order -> order.getVendor().getName()).containsExactly("Plan Wellpack", "Plan JFC");
    assertThat(vendorOrders).allSatisfy(order -> assertThat(order.getSourceInventorySession().getId()).isEqualTo(submittedCountId));
    assertThat(vendorOrders).allSatisfy(order -> assertThat(order.getOrderPlanSession().getId()).isEqualTo(planId));
    assertThat(orderLines.findByPurchaseOrderIdOrderByIdAsc(vendorOrders.getFirst().getId()).getFirst().getSuggestedOrderQuantity()).isEqualByComparingTo("6");
    assertThat(orderLines.findByPurchaseOrderIdOrderByIdAsc(vendorOrders.getFirst().getId()).getFirst().getFinalOrderQuantity()).isEqualByComparingTo("7");

    plan = ordering.submitOrderPlan(orderer, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.SUBMITTED);
    Long submittedPlanId = plan.getId();
    assertThatThrownBy(() -> ordering.upsertOrderPlanLines(orderer, submittedPlanId, new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(gloves.getId(), new BigDecimal("8"), "late edit")
    ))))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_EDITABLE");
    plan = ordering.completeOrderPlan(manager, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.COMPLETED);
    var completedPlan = plan;
    assertThatThrownBy(() -> ordering.submitOrderPlan(orderer, completedPlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_SUBMITTABLE");
    assertThat(ordering.auditEvents("ORDER_PLAN", plan.getId())).extracting(OrderAuditEvent::getAction)
        .contains("ORDER_PLAN_CREATED", "SOURCE_INVENTORY_SELECTED", "SUGGESTED_QUANTITY_GENERATED", "ORDER_PLAN_STARTED", "FINAL_QUANTITY_OVERRIDDEN", "VENDOR_ORDERS_GENERATED", "ORDER_PLAN_SUBMITTED", "ORDER_PLAN_COMPLETED");

    assertThatThrownBy(() -> ordering.overrideInventory(manager, submittedCountId, new InventoryTransitionRequest(null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("INVENTORY_OVERRIDE_REASON_REQUIRED");
  }

  @Test
  void reviewedAndLockedInventorySourcesRemainIsolatedAndSelectableForOrderPlans() {
    Employee counter = employee("counter-source@example.com", Position.HOST, StoreCode.SEATTLE, "7601");
    Employee orderer = employee("orderer-source@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7602");
    Employee manager = employee("manager-source@example.com", Position.MANAGER, StoreCode.SEATTLE, "7603");
    Employee redmondCounter = employee("redmond-source@example.com", Position.HOST, StoreCode.REDMOND, "7604");
    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Source Vendor", "SRC", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "SRC-1", "Source Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "4 bags", new BigDecimal("9.00"), "USD", new BigDecimal("8"), null, null, true, 1, null), null);

    var reviewed = eligibleInventory(counter, manager, product, LocalDate.of(2026, 7, 1), new BigDecimal("3"), true, false);
    var reviewedPlan = ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, reviewed.getId(), LocalDate.of(2026, 7, 3), null, null, null, "reviewed source"));
    assertThat(reviewedPlan.getSourceInventorySession().getId()).isEqualTo(reviewed.getId());
    assertThat(reviewedPlan.getInventoryBusinessDate()).isEqualTo(LocalDate.of(2026, 7, 1));
    assertThat(planLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(reviewedPlan.getId()))
        .filteredOn(line -> line.getProduct().getId().equals(product.getId()))
        .singleElement()
        .satisfies(line -> {
          assertThat(line.getSourceInventoryQuantity()).isEqualByComparingTo("3");
          assertThat(line.getSuggestedOrderQuantity()).isEqualByComparingTo("5");
          assertThat(line.getFinalOrderQuantity()).isEqualByComparingTo("5");
        });

    var newer = eligibleInventory(counter, manager, product, LocalDate.of(2026, 7, 2), BigDecimal.ZERO, false, false);
    assertThat(newer.getId()).isNotEqualTo(reviewedPlan.getSourceInventorySession().getId());
    assertThat(orderPlans.findById(reviewedPlan.getId()).orElseThrow().getSourceInventorySession().getId()).isEqualTo(reviewed.getId());

    var locked = eligibleInventory(counter, manager, product, LocalDate.of(2026, 7, 4), new BigDecimal("10"), true, true);
    var lockedPlan = ordering.createOrderPlan(orderer, new CreateOrderPlanRequest(StoreCode.SEATTLE, locked.getId(), LocalDate.of(2026, 7, 5), null, null, null, "locked source"));
    assertThat(lockedPlan.getSourceInventorySession().getId()).isEqualTo(locked.getId());
    assertThat(planLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(lockedPlan.getId()))
        .filteredOn(line -> line.getProduct().getId().equals(product.getId()))
        .singleElement()
        .satisfies(line -> assertThat(line.getSuggestedOrderQuantity()).isEqualByComparingTo("0"));

    var cancelledPlan = ordering.cancelOrderPlan(orderer, lockedPlan.getId());
    assertThat(cancelledPlan.getStatus()).isEqualTo(OrderPlanStatus.CANCELLED);
    Long cancelledPlanId = cancelledPlan.getId();
    assertThatThrownBy(() -> ordering.startOrderPlan(orderer, cancelledPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_STARTABLE");

    var redmondCount = ordering.createInventory(redmondCounter, new CreateInventorySessionRequest(StoreCode.REDMOND, LocalDate.of(2026, 7, 6), "redmond source"));
    redmondCount = ordering.submitInventory(redmondCounter, redmondCount.getId());
    Long redmondCountId = redmondCount.getId();
    assertThatThrownBy(() -> ordering.createOrderPlan(manager, new CreateOrderPlanRequest(StoreCode.SEATTLE, redmondCountId, LocalDate.of(2026, 7, 7), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_INVENTORY_LOCATION_MISMATCH");
  }

  @Test
  void activeStoreEmployeesCanOperateInventoryAndOrderPlansWithoutAssignmentOrBusinessPartnerRole() {
    Employee manager = employee("manager-permissions@example.com", Position.MANAGER, StoreCode.SEATTLE, "7701");
    Employee creator = employee("creator-permissions@example.com", Position.HOST, StoreCode.SEATTLE, "7702");
    Employee assignedCounter = employee("assigned-counter-permissions@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7703");
    Employee operator = employee("operator-permissions@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7704");
    Employee assignedOrderer = employee("assigned-orderer-permissions@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7705");
    Employee shiftLeader = employee("shift-leader-permissions@example.com", Position.SHIFT_LEADER, StoreCode.SEATTLE, "7706");
    Employee redmond = employee("redmond-permissions@example.com", Position.HOST, StoreCode.REDMOND, "7707");
    Employee inactive = employee("inactive-permissions@example.com", Position.HOST, StoreCode.SEATTLE, "7708");
    inactive.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(inactive);

    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Permission Vendor", "PV", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "PV-1", "Permission Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("12.00"), "USD", new BigDecimal("6"), null, null, true, 1, null), null);

    var count = ordering.createInventory(creator, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 7, 8), assignedCounter.getId(), null, null, "assigned to someone else"));
    assertThat(count.getCreatedBy().getId()).isEqualTo(creator.getId());
    assertThat(count.getAssignedCounter().getId()).isEqualTo(assignedCounter.getId());
    assertThat(ordering.listInventory(operator, StoreCode.SEATTLE)).extracting(InventoryCountSession::getId).contains(count.getId());

    count = ordering.startInventory(operator, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.IN_PROGRESS);
    count = ordering.upsertInventoryLines(operator, count.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), new BigDecimal("2"), CatalogUnit.CASE, "not assigned counter")
    )));
    assertThat(ordering.inventoryLines(count.getId())).singleElement().satisfies(line -> {
      assertThat(line.getUpdatedBy().getId()).isEqualTo(operator.getId());
      assertThat(line.getQuantityOnHand()).isEqualByComparingTo("2");
    });
    count = ordering.submitInventory(operator, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.SUBMITTED);
    assertThat(count.getSubmittedBy().getId()).isEqualTo(operator.getId());
    Long countId = count.getId();

    assertThatThrownBy(() -> ordering.createInventory(inactive, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 7, 9), "inactive create")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> ordering.upsertInventoryLines(inactive, countId, new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), BigDecimal.ONE, CatalogUnit.CASE, "inactive edit")
    ))))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> ordering.createInventory(redmond, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 7, 9), "wrong store create")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.listInventory(redmond, StoreCode.SEATTLE))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.upsertInventoryLines(redmond, countId, new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), BigDecimal.ONE, CatalogUnit.CASE, "wrong store edit")
    ))))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.reviewInventory(operator, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.lockInventory(shiftLeader, countId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.overrideInventory(operator, countId, new InventoryTransitionRequest("ordinary override")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    var plan = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, count.getId(), LocalDate.of(2026, 7, 10), assignedOrderer.getId(), null, null, "assigned to someone else"));
    assertThat(plan.getCreatedBy().getId()).isEqualTo(creator.getId());
    assertThat(plan.getAssignedOrderer().getId()).isEqualTo(assignedOrderer.getId());
    assertThat(ordering.listOrderPlans(operator, StoreCode.SEATTLE, null)).extracting(OrderPlanSession::getId).contains(plan.getId());

    plan = ordering.upsertOrderPlanLines(operator, plan.getId(), new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), new BigDecimal("4"), "not assigned orderer")
    )));
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.IN_PROGRESS);
    assertThat(planLines.findByOrderPlanSessionIdOrderByVendorDisplayOrderAscVendorNameSnapshotAscProductDisplayOrderAscProductNameSnapshotAsc(plan.getId()))
        .filteredOn(line -> line.getProduct().getId().equals(product.getId()))
        .singleElement()
        .satisfies(line -> {
          assertThat(line.getUpdatedBy().getId()).isEqualTo(operator.getId());
          assertThat(line.getSuggestedOrderQuantity()).isEqualByComparingTo("4");
          assertThat(line.getFinalOrderQuantity()).isEqualByComparingTo("4");
        });
    plan = ordering.submitOrderPlan(operator, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.SUBMITTED);
    assertThat(plan.getSubmittedBy().getId()).isEqualTo(operator.getId());
    Long planId = plan.getId();

    assertThatThrownBy(() -> ordering.createOrderPlan(inactive, new CreateOrderPlanRequest(StoreCode.SEATTLE, countId, LocalDate.of(2026, 7, 11), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> ordering.createOrderPlan(redmond, new CreateOrderPlanRequest(StoreCode.SEATTLE, countId, LocalDate.of(2026, 7, 11), null, null, null, null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.getOrderPlan(redmond, planId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.upsertOrderPlanLines(operator, planId, new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), new BigDecimal("5"), "submitted edit")
    ))))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_EDITABLE");
    assertThatThrownBy(() -> ordering.completeOrderPlan(operator, planId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.completeOrderPlan(shiftLeader, planId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");

    count = ordering.reviewInventory(manager, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.REVIEWED);
    count = ordering.lockInventory(manager, count.getId());
    assertThat(count.getStatus()).isEqualTo(InventoryCountStatus.LOCKED);
    plan = ordering.completeOrderPlan(manager, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.COMPLETED);
  }

  @Test
  void inventoryAndOrderPlanSubmissionsCaptureImmutableActorSnapshots() throws Exception {
    Employee manager = employee("manager-signature@example.com", Position.MANAGER, StoreCode.SEATTLE, "7801");
    Employee inventoryCreator = employee("inventory-creator-signature@example.com", Position.HOST, StoreCode.SEATTLE, "7802");
    Employee inventorySubmitter = employee("inventory-submitter-signature@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7803");
    inventorySubmitter.setEnglishName("Inventory Submitter Legal");
    inventorySubmitter.setPreferredName("Inventory Submitter");
    inventorySubmitter = employees.save(inventorySubmitter);
    Employee assignedCounter = employee("assigned-counter-signature@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7804");
    Employee planCreator = employee("plan-creator-signature@example.com", Position.HOST, StoreCode.SEATTLE, "7805");
    Employee planSubmitter = employee("plan-submitter-signature@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7806");
    planSubmitter.setEnglishName("Plan Submitter Legal");
    planSubmitter.setPreferredName("Plan Submitter");
    planSubmitter = employees.save(planSubmitter);
    Employee assignedOrderer = employee("assigned-orderer-signature@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7807");
    Employee inactive = employee("inactive-signature@example.com", Position.HOST, StoreCode.SEATTLE, "7808");
    inactive.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(inactive);
    Employee redmond = employee("redmond-signature@example.com", Position.HOST, StoreCode.REDMOND, "7809");

    assertThat(recordComponentNames(CreateInventorySessionRequest.class))
        .doesNotContain("submittedByEmployeeId", "submittedByName", "submittedByNameSnapshot", "submittedAt");
    assertThat(recordComponentNames(CreateOrderPlanRequest.class))
        .doesNotContain("submittedByEmployeeId", "submittedByName", "submittedByNameSnapshot", "submittedAt");
    assertThat(recordComponentNames(UpsertInventoryLinesRequest.class))
        .doesNotContain("submittedByEmployeeId", "submittedByName", "submittedByNameSnapshot", "submittedAt");
    assertThat(recordComponentNames(UpsertOrderPlanLinesRequest.class))
        .doesNotContain("submittedByEmployeeId", "submittedByName", "submittedByNameSnapshot", "submittedAt");

    Vendor vendor = ordering.saveVendor(manager, new VendorRequest(StoreCode.SEATTLE, "Signature Vendor", "SIG", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(manager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "SIG-1", "Signature Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("10.00"), "USD", new BigDecimal("5"), null, null, true, 1, null), null);

    var count = ordering.createInventory(inventoryCreator, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 7, 12), assignedCounter.getId(), null, null, "signature source"));
    count = ordering.upsertInventoryLines(inventoryCreator, count.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), BigDecimal.ONE, CatalogUnit.CASE, "counted by creator")
    )));
    count = ordering.submitInventory(inventorySubmitter, count.getId());

    assertThat(count.getSubmittedBy().getId()).isEqualTo(inventorySubmitter.getId());
    assertThat(count.getSubmittedBy().getId()).isNotEqualTo(assignedCounter.getId());
    assertThat(count.getSubmittedByNameSnapshot()).isEqualTo("Inventory Submitter");
    assertThat(count.getSubmittedAt()).isNotNull();
    assertThat(count.getCompletedBy().getId()).isEqualTo(inventorySubmitter.getId());
    assertThat(count.getCompletedByNameSnapshot()).isEqualTo("Inventory Submitter");
    assertThat(count.getCompletedAt()).isNotNull();
    var inventoryResponse = mapper.inventorySession(count, ordering.inventoryLines(count.getId()));
    assertThat(inventoryResponse.submittedByName()).isEqualTo("Inventory Submitter");
    assertThat(objectMapper.writeValueAsString(inventoryResponse)).contains("\"submittedByName\":\"Inventory Submitter\"");

    var plan = ordering.createOrderPlan(planCreator, new CreateOrderPlanRequest(StoreCode.SEATTLE, count.getId(), LocalDate.of(2026, 7, 13), assignedOrderer.getId(), null, null, "signature plan"));
    plan = ordering.upsertOrderPlanLines(planCreator, plan.getId(), new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), new BigDecimal("4"), "planned by creator")
    )));
    plan = ordering.submitOrderPlan(planSubmitter, plan.getId());

    assertThat(plan.getSubmittedBy().getId()).isEqualTo(planSubmitter.getId());
    assertThat(plan.getSubmittedBy().getId()).isNotEqualTo(assignedOrderer.getId());
    assertThat(plan.getSubmittedBy().getId()).isNotEqualTo(inventorySubmitter.getId());
    assertThat(plan.getSubmittedByNameSnapshot()).isEqualTo("Plan Submitter");
    assertThat(plan.getSubmittedAt()).isNotNull();
    var planResponse = mapper.orderPlan(plan, ordering.orderPlanLines(plan.getId()), ordering.vendorOrdersForPlan(plan.getId()), ordering.auditEvents("ORDER_PLAN", plan.getId()));
    assertThat(planResponse.submittedByName()).isEqualTo("Plan Submitter");
    assertThat(objectMapper.writeValueAsString(planResponse)).contains("\"submittedByName\":\"Plan Submitter\"");

    inventorySubmitter.setEnglishName("Renamed Inventory Submitter Legal");
    inventorySubmitter.setPreferredName("Renamed Inventory Submitter");
    inventorySubmitter.setPositions(Set.of(Position.SERVER_ONE_STAR));
    inventorySubmitter.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(inventorySubmitter);
    planSubmitter.setEnglishName("Renamed Plan Submitter Legal");
    planSubmitter.setPreferredName("Renamed Plan Submitter");
    planSubmitter.setPositions(Set.of(Position.FOOD_RUNNER));
    planSubmitter.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(planSubmitter);

    assertThat(employees.findById(inventorySubmitter.getId()).orElseThrow().getDisplayName()).isEqualTo("Renamed Inventory Submitter");
    assertThat(employees.findById(planSubmitter.getId()).orElseThrow().getDisplayName()).isEqualTo("Renamed Plan Submitter");
    InventoryCountSession persistedCount = inventorySessions.findById(count.getId()).orElseThrow();
    OrderPlanSession persistedPlan = orderPlans.findById(plan.getId()).orElseThrow();
    assertThat(persistedCount.getSubmittedByNameSnapshot()).isEqualTo("Inventory Submitter");
    assertThat(persistedCount.getCompletedByNameSnapshot()).isEqualTo("Inventory Submitter");
    assertThat(persistedPlan.getSubmittedByNameSnapshot()).isEqualTo("Plan Submitter");
    assertThat(persistedPlan.getInventoryCompletedByNameSnapshot()).isEqualTo("Inventory Submitter");

    var rejectedInventory = ordering.createInventory(inventoryCreator, new CreateInventorySessionRequest(StoreCode.SEATTLE, LocalDate.of(2026, 7, 14), "rejected submit"));
    Long rejectedInventoryId = rejectedInventory.getId();
    assertThatThrownBy(() -> ordering.submitInventory(inactive, rejectedInventoryId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> ordering.submitInventory(redmond, rejectedInventoryId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");

    var rejectedPlan = ordering.createOrderPlan(planCreator, new CreateOrderPlanRequest(StoreCode.SEATTLE, count.getId(), LocalDate.of(2026, 7, 15), null, null, null, "rejected plan submit"));
    Long rejectedPlanId = rejectedPlan.getId();
    assertThatThrownBy(() -> ordering.submitOrderPlan(inactive, rejectedPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");
    assertThatThrownBy(() -> ordering.submitOrderPlan(redmond, rejectedPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
  }

  @Test
  void businessPartnersApproveAndRejectSubmittedOrderPlansWithDecisionGuards() {
    Employee setupManager = employee("setup-order-plan-decision@example.com", Position.MANAGER, StoreCode.SEATTLE, "7901");
    Employee submitter = employee("submitter-order-plan-decision@example.com", Position.HOST, StoreCode.SEATTLE, "7902");
    Employee planCreator = employee("creator-order-plan-decision@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7903");
    Employee approver = employee("approver-order-plan-decision@example.com", Position.MANAGER, StoreCode.SEATTLE, "7904");
    approver.setPreferredName("Decision Approver");
    employees.save(approver);
    Employee rejecter = employee("rejecter-order-plan-decision@example.com", Position.FINANCIAL_MANAGER, StoreCode.SEATTLE, "7905");
    rejecter.setPreferredName("Decision Rejecter");
    employees.save(rejecter);
    Employee ownerSubmitter = employee("owner-submitter-order-plan-decision@example.com", Position.OWNER, StoreCode.SEATTLE, "7906");
    Employee shiftLeader = employee("shift-order-plan-decision@example.com", Position.SHIFT_LEADER, StoreCode.SEATTLE, "7907");
    Employee ordinary = employee("ordinary-order-plan-decision@example.com", Position.BARTENDER, StoreCode.SEATTLE, "7908");
    Employee redmondManager = employee("redmond-manager-order-plan-decision@example.com", Position.MANAGER, StoreCode.REDMOND, "7909");
    Employee inactiveManager = employee("inactive-manager-order-plan-decision@example.com", Position.MANAGER, StoreCode.SEATTLE, "7910");
    inactiveManager.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(inactiveManager);

    assertThat(recordComponentNames(RejectOrderRequest.class))
        .containsExactly("reason")
        .doesNotContain("rejectedByEmployeeId", "rejectedByName", "rejectedByNameSnapshot", "rejectedAt", "approvedByEmployeeId", "approvedByName", "approvedAt");

    Vendor vendor = ordering.saveVendor(setupManager, new VendorRequest(StoreCode.SEATTLE, "Decision Vendor", "DEC", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(setupManager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "DEC-1", "Decision Product", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("8.00"), "USD", new BigDecimal("3"), null, null, true, 1, null), null);

    var approvalPlan = submittedOrderPlan(planCreator, submitter, setupManager, product, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2));
    approvalPlan = ordering.approveOrderPlan(approver, approvalPlan.getId());
    assertThat(approvalPlan.getStatus()).isEqualTo(OrderPlanStatus.COMPLETED);
    assertThat(approvalPlan.getCompletedBy().getId()).isEqualTo(approver.getId());
    assertThat(approvalPlan.getCompletedByNameSnapshot()).isEqualTo("Decision Approver");
    assertThat(approvalPlan.getCompletedAt()).isNotNull();
    assertThat(ordering.auditEvents("ORDER_PLAN", approvalPlan.getId())).extracting(OrderAuditEvent::getAction)
        .contains("ORDER_PLAN_APPROVED", "ORDER_PLAN_COMPLETED");

    var rejectionPlan = submittedOrderPlan(planCreator, submitter, setupManager, product, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4));
    rejectionPlan = ordering.rejectOrderPlan(rejecter, rejectionPlan.getId(), new RejectOrderRequest("Quantity requires correction"));
    assertThat(rejectionPlan.getStatus()).isEqualTo(OrderPlanStatus.REJECTED);
    assertThat(rejectionPlan.getRejectedBy().getId()).isEqualTo(rejecter.getId());
    assertThat(rejectionPlan.getRejectedByNameSnapshot()).isEqualTo("Decision Rejecter");
    assertThat(rejectionPlan.getRejectedAt()).isNotNull();
    assertThat(rejectionPlan.getRejectionReason()).isEqualTo("Quantity requires correction");

    approver.setPreferredName("Renamed Decision Approver");
    approver.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(approver);
    rejecter.setPreferredName("Renamed Decision Rejecter");
    rejecter.setStatus(EmployeeStatus.DEACTIVATED);
    employees.save(rejecter);
    assertThat(orderPlans.findById(approvalPlan.getId()).orElseThrow().getCompletedByNameSnapshot()).isEqualTo("Decision Approver");
    assertThat(orderPlans.findById(rejectionPlan.getId()).orElseThrow().getRejectedByNameSnapshot()).isEqualTo("Decision Rejecter");

    var selfApprovePlan = submittedOrderPlan(planCreator, ownerSubmitter, setupManager, product, LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 6));
    assertThatThrownBy(() -> ordering.approveOrderPlan(ownerSubmitter, selfApprovePlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_SELF_DECISION_FORBIDDEN");
    assertThatThrownBy(() -> ordering.completeOrderPlan(ownerSubmitter, selfApprovePlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_SELF_DECISION_FORBIDDEN");
    var selfRejectPlan = submittedOrderPlan(planCreator, ownerSubmitter, setupManager, product, LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 8));
    assertThatThrownBy(() -> ordering.rejectOrderPlan(ownerSubmitter, selfRejectPlan.getId(), new RejectOrderRequest("self reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_SELF_DECISION_FORBIDDEN");

    var rolePlan = submittedOrderPlan(planCreator, submitter, setupManager, product, LocalDate.of(2026, 8, 9), LocalDate.of(2026, 8, 10));
    assertThatThrownBy(() -> ordering.approveOrderPlan(shiftLeader, rolePlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.rejectOrderPlan(ordinary, rolePlan.getId(), new RejectOrderRequest("ordinary reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_BUSINESS_PARTNER_REQUIRED");
    assertThatThrownBy(() -> ordering.approveOrderPlan(redmondManager, rolePlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_STORE_REQUIRED");
    assertThatThrownBy(() -> ordering.rejectOrderPlan(inactiveManager, rolePlan.getId(), new RejectOrderRequest("inactive reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("AUTH_ACTIVE_EMPLOYEE_REQUIRED");

    var draftPlan = ordering.createOrderPlan(planCreator, new CreateOrderPlanRequest(StoreCode.SEATTLE, eligibleInventory(planCreator, setupManager, product, LocalDate.of(2026, 8, 11), BigDecimal.ONE, false, false).getId(), LocalDate.of(2026, 8, 12), null, null, null, "draft decision"));
    assertThatThrownBy(() -> ordering.approveOrderPlan(setupManager, draftPlan.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");
    assertThatThrownBy(() -> ordering.rejectOrderPlan(setupManager, draftPlan.getId(), new RejectOrderRequest("draft reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_REJECTABLE");

    Long approvedPlanId = approvalPlan.getId();
    assertThatThrownBy(() -> ordering.approveOrderPlan(setupManager, approvedPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");
    assertThatThrownBy(() -> ordering.rejectOrderPlan(setupManager, approvedPlanId, new RejectOrderRequest("approved reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_REJECTABLE");

    Long rejectedPlanId = rejectionPlan.getId();
    assertThatThrownBy(() -> ordering.rejectOrderPlan(setupManager, rejectedPlanId, new RejectOrderRequest("reject twice")))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_REJECTABLE");
    assertThatThrownBy(() -> ordering.approveOrderPlan(setupManager, rejectedPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");

    var cancelledPlan = ordering.createOrderPlan(planCreator, new CreateOrderPlanRequest(StoreCode.SEATTLE, eligibleInventory(planCreator, setupManager, product, LocalDate.of(2026, 8, 13), BigDecimal.ONE, false, false).getId(), LocalDate.of(2026, 8, 14), null, null, null, "cancel decision"));
    cancelledPlan = ordering.cancelOrderPlan(planCreator, cancelledPlan.getId());
    Long cancelledPlanId = cancelledPlan.getId();
    assertThatThrownBy(() -> ordering.approveOrderPlan(setupManager, cancelledPlanId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");
    assertThatThrownBy(() -> ordering.rejectOrderPlan(setupManager, cancelledPlanId, new RejectOrderRequest("cancel reject")))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_REJECTABLE");
  }

  @Test
  void orderPlanApprovalPersistsImmutableVersionedPdfDocument() throws Exception {
    Employee setupManager = employee("setup-order-plan-pdf@example.com", Position.MANAGER, StoreCode.SEATTLE, "7921");
    Employee creator = employee("creator-order-plan-pdf@example.com", Position.FOOD_RUNNER, StoreCode.SEATTLE, "7922");
    Employee submitter = employee("submitter-order-plan-pdf@example.com", Position.HOST, StoreCode.SEATTLE, "7923");
    submitter.setPreferredName("PDF Plan Submitter");
    employees.save(submitter);
    Employee approver = employee("approver-order-plan-pdf@example.com", Position.FINANCIAL_MANAGER, StoreCode.SEATTLE, "7924");
    approver.setPreferredName("PDF Plan Approver");
    employees.save(approver);
    Employee ownerSubmitter = employee("owner-submitter-order-plan-pdf@example.com", Position.OWNER, StoreCode.SEATTLE, "7925");

    Vendor vendor = ordering.saveVendor(setupManager, new VendorRequest(StoreCode.SEATTLE, "Order Plan PDF Vendor", "OPDF", null, null, null, null, Set.of(), null, null, true, 1), null);
    OrderCatalogProduct product = ordering.saveProduct(setupManager, new ProductRequest(StoreCode.SEATTLE, vendor.getId(), "OPDF-1", "Order Plan PDF Product With A Long Readable Name", null, CatalogCategory.DRY_GOODS, null, CatalogUnit.CASE, null, CatalogUnit.CASE, null, "case", new BigDecimal("12.50"), "USD", new BigDecimal("6"), null, null, true, 1, null), null);

    OrderPlanSession plan = submittedOrderPlanWithLine(creator, submitter, setupManager, product, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 16), new BigDecimal("2"), new BigDecimal("5"));
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(plan.getId())).isEmpty();

    OrderPlanSession rejected = submittedOrderPlanWithLine(creator, submitter, setupManager, product, LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 18), BigDecimal.ONE, BigDecimal.ONE);
    ordering.rejectOrderPlan(approver, rejected.getId(), new RejectOrderRequest("not ready for PDF"));
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(rejected.getId())).isEmpty();

    OrderPlanSession cancelled = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, eligibleInventory(creator, setupManager, product, LocalDate.of(2026, 8, 19), BigDecimal.ONE, false, false).getId(), LocalDate.of(2026, 8, 20), null, null, null, "cancel no pdf"));
    ordering.cancelOrderPlan(creator, cancelled.getId());
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(cancelled.getId())).isEmpty();

    plan = ordering.approveOrderPlan(approver, plan.getId());
    assertThat(plan.getStatus()).isEqualTo(OrderPlanStatus.COMPLETED);
    List<OrderPlanPdfDocument> versions = orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(plan.getId());
    assertThat(versions).hasSize(1);
    OrderPlanPdfDocument document = versions.getFirst();
    assertThat(document.getVersionNumber()).isEqualTo(1);
    assertThat(document.getMimeType()).isEqualTo("application/pdf");
    assertThat(document.getFilename()).isEqualTo("order-plan-" + plan.getId() + "-v1.pdf");
    assertThat(document.getGeneratedBy().getId()).isEqualTo(approver.getId());
    assertThat(document.getGeneratedByNameSnapshot()).isEqualTo("PDF Plan Approver");
    assertThat(document.getGeneratedAt()).isNotNull();
    assertThat(document.isCurrentVersion()).isTrue();
    assertThat(document.getContent()).isNotEmpty();
    assertThat(document.getByteSize()).isEqualTo(document.getContent().length);
    assertThat(document.getChecksumSha256()).isEqualTo(sha256(document.getContent()));

    String pdfText = new String(document.getContent(), StandardCharsets.ISO_8859_1);
    assertThat(pdfText).startsWith("%PDF-");
    assertThat(pdfText).contains("xref", "trailer", "%%EOF");
    assertThat(pdfText).contains(
        "Order Plan",
        "StoreCode: SEATTLE",
        "PDF Plan Submitter",
        "PDF Plan Approver",
        "Order Plan PDF Product With A Long Readable Name",
        "Order Plan PDF Vendor",
        "Suggested quantity",
        "Final quantity",
        "4",
        "5",
        "Total final quantity");

    Long planId = plan.getId();
    assertThatThrownBy(() -> ordering.approveOrderPlan(setupManager, planId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");
    assertThatThrownBy(() -> ordering.completeOrderPlan(setupManager, planId))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_NOT_APPROVABLE");
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(planId)).hasSize(1);

    OrderPlanSession completedViaLegacyEndpoint = submittedOrderPlanWithLine(creator, submitter, setupManager, product, LocalDate.of(2026, 8, 21), LocalDate.of(2026, 8, 22), BigDecimal.ZERO, new BigDecimal("3"));
    completedViaLegacyEndpoint = ordering.completeOrderPlan(approver, completedViaLegacyEndpoint.getId());
    assertThat(completedViaLegacyEndpoint.getStatus()).isEqualTo(OrderPlanStatus.COMPLETED);
    Long legacyPlanId = completedViaLegacyEndpoint.getId();
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(legacyPlanId))
        .singleElement()
        .satisfies(pdf -> {
          assertThat(pdf.getVersionNumber()).isEqualTo(1);
          assertThat(pdf.getFilename()).isEqualTo("order-plan-" + legacyPlanId + "-v1.pdf");
        });

    OrderPlanSession selfApproval = submittedOrderPlanWithLine(creator, ownerSubmitter, setupManager, product, LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 24), BigDecimal.ZERO, BigDecimal.ONE);
    assertThatThrownBy(() -> ordering.approveOrderPlan(ownerSubmitter, selfApproval.getId()))
        .isInstanceOf(ApiException.class)
        .hasMessage("ORDER_PLAN_SELF_DECISION_FORBIDDEN");
    assertThat(orderPlanPdfDocuments.findByOrderPlanIdOrderByVersionNumberAsc(selfApproval.getId())).isEmpty();
  }

  private List<String> recordComponentNames(Class<?> recordType) {
    return Arrays.stream(recordType.getRecordComponents()).map(RecordComponent::getName).toList();
  }

  private String sha256(byte[] value) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
  }

  private Employee employee(String email, Position position, StoreCode store, String toastPin) {
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
      return employees.save(employee);
    });
  }

  private InventoryCountSession eligibleInventory(Employee counter, Employee manager, OrderCatalogProduct product, LocalDate businessDate, BigDecimal quantity, boolean reviewed, boolean locked) {
    var count = ordering.createInventory(counter, new CreateInventorySessionRequest(StoreCode.SEATTLE, businessDate, "eligible source"));
    count = ordering.upsertInventoryLines(counter, count.getId(), new UpsertInventoryLinesRequest(List.of(
        new UpsertInventoryLineRequest(product.getId(), quantity, CatalogUnit.CASE, "source count")
    )));
    count = ordering.submitInventory(counter, count.getId());
    if (reviewed || locked) count = ordering.reviewInventory(manager, count.getId());
    if (locked) count = ordering.lockInventory(manager, count.getId());
    return inventorySessions.findById(count.getId()).orElseThrow();
  }

  private OrderPlanSession submittedOrderPlan(Employee creator, Employee submitter, Employee inventoryManager, OrderCatalogProduct product, LocalDate inventoryDate, LocalDate planDate) {
    InventoryCountSession source = eligibleInventory(creator, inventoryManager, product, inventoryDate, BigDecimal.ONE, false, false);
    OrderPlanSession plan = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, source.getId(), planDate, null, null, null, "submitted decision plan"));
    return ordering.submitOrderPlan(submitter, plan.getId());
  }

  private OrderPlanSession submittedOrderPlanWithLine(Employee creator, Employee submitter, Employee inventoryManager, OrderCatalogProduct product, LocalDate inventoryDate, LocalDate planDate, BigDecimal countedQuantity, BigDecimal finalQuantity) {
    InventoryCountSession source = eligibleInventory(creator, inventoryManager, product, inventoryDate, countedQuantity, false, false);
    OrderPlanSession plan = ordering.createOrderPlan(creator, new CreateOrderPlanRequest(StoreCode.SEATTLE, source.getId(), planDate, null, null, null, "submitted pdf plan"));
    plan = ordering.upsertOrderPlanLines(creator, plan.getId(), new UpsertOrderPlanLinesRequest(List.of(
        new UpsertOrderPlanLineRequest(product.getId(), finalQuantity, "pdf final quantity")
    )));
    return ordering.submitOrderPlan(submitter, plan.getId());
  }
}
