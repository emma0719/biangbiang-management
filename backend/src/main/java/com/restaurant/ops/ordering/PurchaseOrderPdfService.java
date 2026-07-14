package com.restaurant.ops.ordering;

import com.restaurant.ops.employee.Employee;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PurchaseOrderPdfService {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));

  PurchaseOrderPdfDocument generate(PurchaseOrder order, List<PurchaseOrderLine> lines, Employee approver, int version) {
    Instant generatedAt = Instant.now();
    String filename = filename(order, version);
    byte[] content = renderPdf(lines(order, lines, version, generatedAt));
    PurchaseOrderPdfDocument document = new PurchaseOrderPdfDocument();
    document.setPurchaseOrder(order);
    document.setVersionNumber(version);
    document.setFilename(filename);
    document.setMimeType("application/pdf");
    document.setGeneratedAt(generatedAt);
    document.setGeneratedBy(approver);
    document.setContent(content);
    document.setChecksumSha256(sha256(content));
    document.setCurrentVersion(true);
    return document;
  }

  private List<String> lines(PurchaseOrder order, List<PurchaseOrderLine> orderLines, int version, Instant generatedAt) {
    List<String> lines = new ArrayList<>();
    lines.add("Biangbiang Restaurant Operations");
    lines.add("Approved Order Plan PDF");
    lines.add("Store/location: " + order.getLocationCode());
    lines.add("Business: " + order.getOrderBusiness().displayName());
    lines.add("Order-plan number: " + order.getOrderNumber());
    lines.add("Order business date: " + order.getBusinessDate());
    lines.add("Source inventory count number: " + (order.getSourceInventorySession() == null ? "" : order.getSourceInventorySession().getId()));
    lines.add("Source inventory business date: " + (order.getSourceInventorySession() == null ? "" : order.getSourceInventorySession().getBusinessDate()));
    lines.add("Inventory completed by: " + (order.getSourceInventorySession() == null ? "" : nullToBlank(order.getSourceInventorySession().getCompletedByNameSnapshot())));
    lines.add("Inventory completed at: " + (order.getSourceInventorySession() == null || order.getSourceInventorySession().getCompletedAt() == null ? "" : STAMP.format(order.getSourceInventorySession().getCompletedAt())));
    lines.add("Order submitted by: " + nullToBlank(order.getSubmittedByNameSnapshot()));
    lines.add("Order submitted at: " + (order.getSubmittedAt() == null ? "" : STAMP.format(order.getSubmittedAt())));
    lines.add("Approved by: " + nullToBlank(order.getApprovedByNameSnapshot()));
    lines.add("Approved at: " + (order.getApprovedAt() == null ? "" : STAMP.format(order.getApprovedAt())));
    lines.add("PDF version: " + version);
    lines.add("Generated timestamp: " + STAMP.format(generatedAt));
    lines.add("");
    lines.add("Vendor section: " + order.getVendor().getName());
    lines.add("vendor | code | product | package | inventory unit | order unit | counted | par | suggested | submitted | approved | unit price | line total | notes");
    for (PurchaseOrderLine line : orderLines) {
      lines.add(String.join(" | ",
          order.getVendor().getName(),
          nullToBlank(line.getProductCodeSnapshot()),
          line.getProductNameSnapshot(),
          nullToBlank(line.getPackageSpecificationSnapshot()),
          line.getProduct().getInventoryUnit().name(),
          line.getOrderUnitSnapshot().name(),
          decimal(line.getCurrentInventoryQuantity()),
          decimal(line.getProduct().getParLevel()),
          decimal(line.getRequestedQuantity()),
          decimal(line.getRequestedQuantity()),
          decimal(line.getApprovedQuantity() == null ? line.getRequestedQuantity() : line.getApprovedQuantity()),
          money(line.getUnitPriceSnapshot()),
          money(line.getLineTotal()),
          nullToBlank(line.getNotes())
      ));
    }
    lines.add("");
    lines.add("Subtotal by vendor - " + order.getVendor().getName() + ": " + money(order.getSubtotal()));
    lines.add("Overall subtotal: " + money(order.getSubtotal()));
    lines.add("Taxes: " + money(order.getTax()));
    lines.add("Fees: " + money(order.getFees()));
    lines.add("Grand total: " + money(order.getTotal()));
    lines.add("Currency: " + order.getCurrency());
    lines.add("");
    lines.add("Prepared and submitted by: " + nullToBlank(order.getSubmittedByNameSnapshot()));
    lines.add("Submission timestamp: " + (order.getSubmittedAt() == null ? "" : STAMP.format(order.getSubmittedAt())));
    lines.add("Approved by: " + nullToBlank(order.getApprovedByNameSnapshot()));
    lines.add("Approval timestamp: " + (order.getApprovedAt() == null ? "" : STAMP.format(order.getApprovedAt())));
    lines.add("Order-plan number: " + order.getOrderNumber());
    lines.add("Page 1");
    return lines;
  }

  private byte[] renderPdf(List<String> textLines) {
    StringBuilder stream = new StringBuilder("BT\n/F1 9 Tf\n50 760 Td\n12 TL\n");
    for (String line : textLines) {
      stream.append("(").append(escape(line)).append(") Tj\nT*\n");
    }
    stream.append("ET\n");
    byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
    List<byte[]> objects = List.of(
        "<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1),
        "<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1),
        "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>".getBytes(StandardCharsets.ISO_8859_1),
        "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1),
        ("<< /Length " + streamBytes.length + " >>\nstream\n" + new String(streamBytes, StandardCharsets.ISO_8859_1) + "endstream").getBytes(StandardCharsets.ISO_8859_1)
    );
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    write(out, "%PDF-1.4\n");
    List<Integer> offsets = new ArrayList<>();
    for (int i = 0; i < objects.size(); i++) {
      offsets.add(out.size());
      write(out, (i + 1) + " 0 obj\n");
      out.writeBytes(objects.get(i));
      write(out, "\nendobj\n");
    }
    int xref = out.size();
    write(out, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
    for (Integer offset : offsets) {
      write(out, String.format("%010d 00000 n \n", offset));
    }
    write(out, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
    return out.toByteArray();
  }

  private void write(ByteArrayOutputStream out, String value) {
    out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
  }

  private String filename(PurchaseOrder order, int version) {
    return "OrderPlan_" + order.getLocationCode().name().substring(0, 1) + order.getLocationCode().name().substring(1).toLowerCase()
        + "_" + order.getBusinessDate() + "_" + order.getOrderNumber() + "_v" + version + ".pdf";
  }

  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (Exception e) {
      throw new IllegalStateException("PDF_CHECKSUM_FAILED", e);
    }
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
  }

  private String nullToBlank(String value) {
    return value == null ? "" : value;
  }

  private String decimal(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private String money(BigDecimal value) {
    return value == null ? "$0.00" : "$" + value.setScale(2).toPlainString();
  }
}
