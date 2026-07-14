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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderPlanPdfService {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));
  private static final int MAX_LINE_CHARS = 104;
  private static final int LINES_PER_PAGE = 54;

  OrderPlanPdfDocument generate(OrderPlanSession plan, List<OrderPlanLine> lines, Employee approver, int version) {
    Instant generatedAt = Instant.now();
    byte[] content = renderPdf(lines(plan, stableLines(lines), version, generatedAt));
    OrderPlanPdfDocument document = new OrderPlanPdfDocument();
    document.setOrderPlan(plan);
    document.setVersionNumber(version);
    document.setFilename("order-plan-" + plan.getId() + "-v" + version + ".pdf");
    document.setMimeType("application/pdf");
    document.setGeneratedAt(generatedAt);
    document.setGeneratedBy(approver);
    document.setGeneratedByNameSnapshot(approver.getDisplayName());
    document.setContent(content);
    document.setByteSize(content.length);
    document.setChecksumSha256(sha256(content));
    document.setCurrentVersion(true);
    return document;
  }

  private List<OrderPlanLine> stableLines(List<OrderPlanLine> lines) {
    return lines.stream()
        .sorted(Comparator.comparing((OrderPlanLine line) -> blank(line.getVendorNameSnapshot()))
            .thenComparing(line -> blank(line.getProductNameSnapshot()))
            .thenComparing(OrderPlanLine::getId, Comparator.nullsLast(Long::compareTo)))
        .toList();
  }

  private List<String> lines(OrderPlanSession plan, List<OrderPlanLine> planLines, int version, Instant generatedAt) {
    List<String> lines = new ArrayList<>();
    lines.add("Biangbiang Restaurant Operations");
    lines.add("Order Plan");
    lines.add("Order Plan ID: " + plan.getId());
    lines.add("PDF version: " + version);
    lines.add("StoreCode: " + plan.getLocationCode());
    lines.add("Order Plan business date: " + plan.getBusinessDate());
    lines.add("Source Inventory Count ID: " + plan.getSourceInventorySession().getId());
    lines.add("Source Inventory business date: " + plan.getInventoryBusinessDate());
    lines.add("Order Plan status: " + plan.getStatus());
    lines.add("Submitted by: " + blank(plan.getSubmittedByNameSnapshot()));
    lines.add("Submitted at: " + instant(plan.getSubmittedAt()));
    lines.add("Approved by: " + blank(plan.getCompletedByNameSnapshot()));
    lines.add("Approved at: " + instant(plan.getCompletedAt()));
    lines.add("Generated at: " + STAMP.format(generatedAt));
    lines.add("");
    addTable(lines, planLines);
    lines.add("");
    Set<String> vendors = planLines.stream()
        .map(OrderPlanLine::getVendorNameSnapshot)
        .filter(value -> value != null && !value.isBlank())
        .collect(Collectors.toCollection(java.util.TreeSet::new));
    lines.add("Summary");
    lines.add("Vendor count: " + vendors.size());
    lines.add("Product line count: " + planLines.size());
    lines.add("Total final quantity: " + decimal(planLines.stream()
        .map(OrderPlanLine::getFinalOrderQuantity)
        .filter(value -> value != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add)));
    return wrap(lines);
  }

  private void addTable(List<String> lines, List<OrderPlanLine> planLines) {
    lines.add("Product table");
    lines.add("Vendor | Product name | Unit | Counted quantity | Par quantity | Suggested quantity | Final quantity");
    int row = 0;
    for (OrderPlanLine line : planLines) {
      if (row > 0 && row % 36 == 0) {
        lines.add("");
        lines.add("Product table continued");
        lines.add("Vendor | Product name | Unit | Counted quantity | Par quantity | Suggested quantity | Final quantity");
      }
      lines.add(String.join(" | ",
          blank(line.getVendorNameSnapshot()),
          blank(line.getProductNameSnapshot()),
          line.getOrderUnitSnapshot() == null ? "" : line.getOrderUnitSnapshot().name(),
          decimal(line.getSourceInventoryQuantity()),
          decimal(line.getParLevelSnapshot()),
          decimal(line.getSuggestedOrderQuantity()),
          decimal(line.getFinalOrderQuantity())));
      row++;
    }
  }

  private List<String> wrap(List<String> values) {
    List<String> wrapped = new ArrayList<>();
    for (String value : values) {
      String safe = safeText(value);
      if (safe.length() <= MAX_LINE_CHARS) {
        wrapped.add(safe);
        continue;
      }
      int index = 0;
      while (index < safe.length()) {
        int end = Math.min(index + MAX_LINE_CHARS, safe.length());
        int breakAt = end;
        if (end < safe.length()) {
          int space = safe.lastIndexOf(' ', end);
          if (space > index + 32) breakAt = space;
        }
        wrapped.add((index == 0 ? "" : "  ") + safe.substring(index, breakAt).trim());
        index = breakAt;
      }
    }
    return wrapped;
  }

  private byte[] renderPdf(List<String> textLines) {
    List<List<String>> pages = new ArrayList<>();
    for (int i = 0; i < textLines.size(); i += LINES_PER_PAGE) {
      pages.add(textLines.subList(i, Math.min(i + LINES_PER_PAGE, textLines.size())));
    }
    if (pages.isEmpty()) pages.add(List.of(""));

    List<byte[]> objects = new ArrayList<>();
    objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
    String kids = java.util.stream.IntStream.range(0, pages.size())
        .mapToObj(i -> (3 + i * 2) + " 0 R")
        .collect(Collectors.joining(" "));
    objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(StandardCharsets.ISO_8859_1));
    for (int i = 0; i < pages.size(); i++) {
      int contentObject = 4 + i * 2;
      objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 " + (3 + pages.size() * 2) + " 0 R >> >> /Contents " + contentObject + " 0 R >>").getBytes(StandardCharsets.ISO_8859_1));
      objects.add(stream(pages.get(i)));
    }
    objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));

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

  private byte[] stream(List<String> pageLines) {
    StringBuilder stream = new StringBuilder("BT\n/F1 9 Tf\n40 760 Td\n12 TL\n");
    for (String line : pageLines) {
      stream.append("(").append(escape(line)).append(") Tj\nT*\n");
    }
    stream.append("ET\n");
    byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
    return ("<< /Length " + streamBytes.length + " >>\nstream\n" + new String(streamBytes, StandardCharsets.ISO_8859_1) + "endstream").getBytes(StandardCharsets.ISO_8859_1);
  }

  private void write(ByteArrayOutputStream out, String value) {
    out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
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

  private String safeText(String value) {
    StringBuilder builder = new StringBuilder();
    for (char ch : blank(value).toCharArray()) {
      builder.append(ch >= 32 && ch <= 126 ? ch : '?');
    }
    return builder.toString();
  }

  private String instant(Instant value) {
    return value == null ? "" : STAMP.format(value);
  }

  private String blank(String value) {
    return value == null ? "" : value;
  }

  private String decimal(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }
}
