package com.restaurant.ops.ordering;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.ops.config.AppProperties;
import com.restaurant.ops.employee.StoreCode;
import com.restaurant.ops.ordering.OrderingDtos.CatalogImportReport;
import com.restaurant.ops.ordering.OrderingEnums.CatalogCategory;
import com.restaurant.ops.ordering.OrderingEnums.CatalogUnit;
import com.restaurant.ops.ordering.OrderingEnums.InventoryBusiness;
import com.restaurant.ops.ordering.OrderingEnums.OrderBusiness;
import java.math.BigDecimal;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FrontOrderCatalogImporter implements ApplicationRunner {
  private static final String RESOURCE = "data/front-order-catalog.json";

  private final AppProperties properties;
  private final ObjectMapper objectMapper;
  private final VendorRepository vendors;
  private final OrderCatalogProductRepository products;

  public FrontOrderCatalogImporter(AppProperties properties, ObjectMapper objectMapper, VendorRepository vendors, OrderCatalogProductRepository products) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.vendors = vendors;
    this.products = products;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (properties.orderingSeed().enabled()) {
      importCatalog();
    }
  }

  @Transactional
  public CatalogImportReport importCatalog() {
    CatalogSource source = readSource();
    StoreCode locationCode = StoreCode.valueOf(source.locationCode());
    String sourceVersion = source.sourceVersion() == null || source.sourceVersion().isBlank() ? "unknown" : source.sourceVersion();
    int vendorCount = 0;
    int productCount = 0;
    int vendorOrder = 0;
    for (CatalogVendor sourceVendor : source.vendors()) {
      Vendor vendor = findVendor(locationCode, sourceVendor).orElseGet(Vendor::new);
      boolean createdVendor = vendor.getId() == null;
      if (createdVendor) vendorCount++;
      boolean writableVendor = createdVendor || !vendor.isImportedFromReference();
      vendor.setLocationCode(locationCode);
      vendor.setSourceKey(sourceVendor.sourceKey());
      vendor.setSourceVersion(sourceVersion);
      vendor.setImportedFromReference(true);
      if (writableVendor || isBlank(vendor.getName())) vendor.setName(sourceVendor.name());
      if (writableVendor || isBlank(vendor.getNormalizedName())) vendor.setNormalizedName(key(sourceVendor.name()));
      if (writableVendor || isBlank(vendor.getVendorCode())) vendor.setVendorCode(sourceVendor.vendorCode());
      if (sourceVendor.deliveryDays() != null && (writableVendor || vendor.getDeliveryDays().isEmpty())) vendor.setDeliveryDays(sourceVendor.deliveryDays());
      if (writableVendor || isBlank(vendor.getNotes())) vendor.setNotes(sourceVendor.notes());
      if (createdVendor) vendor.setActive(true);
      if (writableVendor) vendor.setDisplayOrder(vendorOrder);
      vendorOrder++;
      Vendor savedVendor = vendors.save(vendor);

      int productOrder = 0;
      for (CatalogProduct sourceProduct : sourceVendor.products()) {
        OrderCatalogProduct product = findProduct(locationCode, savedVendor.getId(), sourceProduct).orElseGet(OrderCatalogProduct::new);
        boolean createdProduct = product.getId() == null;
        if (createdProduct) productCount++;
        boolean writableProduct = createdProduct || !product.isImportedFromReference();
        product.setLocationCode(locationCode);
        product.setVendor(savedVendor);
        product.setSourceKey(sourceProduct.sourceKey());
        product.setSourceVersion(sourceVersion);
        product.setImportedFromReference(true);
        if (writableProduct || isBlank(product.getVendorProductCode())) product.setVendorProductCode(sourceProduct.code());
        if (writableProduct || isBlank(product.getName())) product.setName(sourceProduct.name());
        if (writableProduct || isBlank(product.getNormalizedName())) product.setNormalizedName(productKey(sourceProduct.code(), sourceProduct.name()));
        if (writableProduct || product.getCategory() == null) product.setCategory(sourceProduct.category());
        InventoryBusiness business = inventoryBusiness(sourceProduct.sourceKey());
        product.setInventoryBusiness(business);
        product.setOrderBusiness(orderBusiness(business));
        if (writableProduct || product.getInventoryUnit() == null || product.getInventoryUnit() == CatalogUnit.OTHER) product.setInventoryUnit(sourceProduct.inventoryUnit() == null ? CatalogUnit.OTHER : sourceProduct.inventoryUnit());
        if (writableProduct || isBlank(product.getInventoryUnitLabel())) product.setInventoryUnitLabel(sourceProduct.inventoryUnitLabel());
        if (writableProduct || product.getOrderUnit() == null || product.getOrderUnit() == CatalogUnit.OTHER) product.setOrderUnit(sourceProduct.orderUnit() == null ? CatalogUnit.OTHER : sourceProduct.orderUnit());
        if (writableProduct || isBlank(product.getOrderUnitLabel())) product.setOrderUnitLabel(sourceProduct.orderUnitLabel());
        if (writableProduct || isBlank(product.getPackageSpecification())) product.setPackageSpecification(sourceProduct.packageSpecification());
        if (writableProduct || product.getUnitPrice() == null) product.setUnitPrice(sourceProduct.unitPrice());
        if (writableProduct || isBlank(product.getCurrency())) product.setCurrency("USD");
        if (writableProduct || isBlank(product.getNotes())) product.setNotes(sourceProduct.notes());
        if (createdProduct) product.setActive(true);
        if (writableProduct) product.setDisplayOrder(productOrder);
        productOrder++;
        products.save(product);
      }
    }
    return new CatalogImportReport(vendorCount, productCount, source.correctedValues(), source.ambiguities(), source.duplicateCandidates(), List.of(source.sourceStatus()));
  }

  private CatalogSource readSource() {
    try {
      return objectMapper.readValue(new ClassPathResource(RESOURCE).getInputStream(), CatalogSource.class);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to read " + RESOURCE, exception);
    }
  }

  private java.util.Optional<Vendor> findVendor(StoreCode locationCode, CatalogVendor sourceVendor) {
    if (!isBlank(sourceVendor.sourceKey())) {
      var bySource = vendors.findByLocationCodeAndSourceKey(locationCode, sourceVendor.sourceKey());
      if (bySource.isPresent()) return bySource;
    }
    List<String> candidates = new ArrayList<>();
    candidates.add(sourceVendor.name());
    if (sourceVendor.legacyNames() != null) candidates.addAll(sourceVendor.legacyNames());
    return candidates.stream()
        .filter(value -> !isBlank(value))
        .map(value -> vendors.findByLocationCodeAndNormalizedName(locationCode, key(value)))
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .findFirst();
  }

  private java.util.Optional<OrderCatalogProduct> findProduct(StoreCode locationCode, Long vendorId, CatalogProduct sourceProduct) {
    if (!isBlank(sourceProduct.sourceKey())) {
      var bySource = products.findByLocationCodeAndVendorIdAndSourceKey(locationCode, vendorId, sourceProduct.sourceKey());
      if (bySource.isPresent()) return bySource;
    }
    List<String> candidates = new ArrayList<>();
    candidates.add(sourceProduct.name());
    if (sourceProduct.legacyNames() != null) candidates.addAll(sourceProduct.legacyNames());
    return candidates.stream()
        .filter(value -> !isBlank(value))
        .map(value -> products.findByLocationCodeAndVendorIdAndNormalizedName(locationCode, vendorId, productKey(sourceProduct.code(), value)))
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .findFirst();
  }

  private String key(String value) {
    return OrderingKeys.key(value);
  }

  private String productKey(String code, String name) {
    return OrderingKeys.productKey(code, name);
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private InventoryBusiness inventoryBusiness(String sourceKey) {
    if (sourceKey == null) return null;
    if (sourceKey.startsWith("p3-")) return InventoryBusiness.PAPER_FAN;
    if (sourceKey.startsWith("p2-") || sourceKey.startsWith("p1-p2-") || sourceKey.contains("-p2-")) return InventoryBusiness.BIANGBIANG_FRONT;
    return null;
  }

  private OrderBusiness orderBusiness(InventoryBusiness business) {
    if (business == null) return null;
    return business == InventoryBusiness.PAPER_FAN ? OrderBusiness.PAPER_FAN : OrderBusiness.BIANGBIANG_FRONT;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CatalogSource(String source, String sourceStatus, String sourceVersion, String locationCode, List<CatalogVendor> vendors, List<String> ambiguities, List<String> correctedValues, List<String> duplicateCandidates) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CatalogVendor(String sourceKey, String name, List<String> legacyNames, String vendorCode, Set<DayOfWeek> deliveryDays, String notes, List<CatalogProduct> products) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record CatalogProduct(String sourceKey, String code, String name, List<String> legacyNames, CatalogCategory category, CatalogUnit inventoryUnit, String inventoryUnitLabel, CatalogUnit orderUnit, String orderUnitLabel, String packageSpecification, BigDecimal unitPrice, String notes) {}
}
