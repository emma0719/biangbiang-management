package com.restaurant.ops.ordering;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class InventoryCountPdfService {
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("UTC"));
  private static final double PAGE_WIDTH = 792;
  private static final double PAGE_HEIGHT = 612;
  private static final double MARGIN = 32;
  private static final double TABLE_TOP = 486;
  private static final double ROW_HEIGHT = 20;
  private static final double FOOTER_Y = 24;
  private static final int ROWS_PER_PAGE = 22;

  byte[] generate(InventoryCountSession session, List<InventoryCountLine> inventoryLines) {
    PdfHeader header = new PdfHeader("Inventory Count", session.getId(), date(session), session.getLocationCode().name(), session.getInventoryBusiness().displayName(), "Counted by", countedBy(session), session.getStatus().name());
    return renderPdf(pages(header, tableRows(stableLines(inventoryLines)), Instant.now(), "No submitted inventory lines"));
  }

  byte[] generateOrder(OrderPlanSession session, List<OrderPlanLine> lines) {
    PdfHeader header = new PdfHeader("Order", session.getId(), orderDate(session), session.getLocationCode().name(), session.getOrderBusiness().displayName(), "Submitted By", submittedBy(session), session.getStatus().name());
    List<TableRow> rows = new ArrayList<>();
    String currentVendor = null;
    for (OrderPlanLine line : lines.stream()
        .filter(value -> value.getFinalOrderQuantity() != null && value.getFinalOrderQuantity().compareTo(BigDecimal.ZERO) > 0)
        .toList()) {
      String vendor = blank(line.getVendorNameSnapshot());
      if (!vendor.equals(currentVendor)) {
        currentVendor = vendor;
        rows.add(TableRow.vendor(vendor));
      }
      rows.add(TableRow.item(vendor, blank(line.getProductNameSnapshot()), decimal(line.getFinalOrderQuantity()), line.getOrderUnitSnapshot() == null ? "" : line.getOrderUnitSnapshot().name()));
    }
    return renderPdf(pages(header, rows, Instant.now(), "No submitted order lines"));
  }

  private List<InventoryCountLine> stableLines(List<InventoryCountLine> inventoryLines) {
    return inventoryLines.stream()
        .sorted(Comparator.comparing((InventoryCountLine line) -> blank(line.getProduct().getVendor().getName()))
            .thenComparing(line -> blank(line.getProduct().getName()))
            .thenComparing(InventoryCountLine::getId, Comparator.nullsLast(Long::compareTo)))
        .toList();
  }

  private List<PageModel> pages(PdfHeader header, List<TableRow> rows, Instant generatedAt, String emptyMessage) {
    List<PageModel> pages = new ArrayList<>();
    if (rows.isEmpty()) {
      pages.add(new PageModel(header, generatedAt, List.of(TableRow.item("", emptyMessage, "", ""))));
      return pages;
    }
    for (int index = 0; index < rows.size(); index += ROWS_PER_PAGE) {
      pages.add(new PageModel(header, generatedAt, rows.subList(index, Math.min(index + ROWS_PER_PAGE, rows.size()))));
    }
    return pages;
  }

  private List<TableRow> tableRows(List<InventoryCountLine> inventoryLines) {
    List<TableRow> rows = new ArrayList<>();
    String currentVendor = null;
    for (InventoryCountLine line : inventoryLines) {
      String vendor = blank(line.getProduct().getVendor().getName());
      if (!vendor.equals(currentVendor)) {
        currentVendor = vendor;
        rows.add(TableRow.vendor(vendor));
      }
      rows.add(TableRow.item(vendor, blank(line.getProduct().getName()), decimal(line.getQuantityOnHand()), line.getUnit() == null ? "" : line.getUnit().name()));
    }
    return rows;
  }

  private byte[] renderPdf(List<PageModel> pages) {
    List<byte[]> objects = new ArrayList<>();
    objects.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1));
    String kids = java.util.stream.IntStream.range(0, pages.size())
        .mapToObj(i -> (3 + i * 2) + " 0 R")
        .collect(Collectors.joining(" "));
    objects.add(("<< /Type /Pages /Kids [" + kids + "] /Count " + pages.size() + " >>").getBytes(StandardCharsets.ISO_8859_1));
    for (int i = 0; i < pages.size(); i++) {
      int contentObject = 4 + i * 2;
      int fontObject = 3 + pages.size() * 2;
      objects.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_WIDTH + " " + (int) PAGE_HEIGHT + "] /Resources << /Font << /F1 " + fontObject + " 0 R /F2 " + (fontObject + 1) + " 0 R >> >> /Contents " + contentObject + " 0 R >>").getBytes(StandardCharsets.ISO_8859_1));
      objects.add(stream(pages.get(i), i + 1, pages.size()));
    }
    objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1));
    objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>".getBytes(StandardCharsets.ISO_8859_1));

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

  private byte[] stream(PageModel page, int pageNumber, int totalPages) {
    StringBuilder stream = new StringBuilder();
    drawHeader(stream, page.header());
    drawTable(stream, page.rows());
    text(stream, "F1", 8, MARGIN, FOOTER_Y, "Generated: " + STAMP.format(page.generatedAt()));
    text(stream, "F1", 8, 356, FOOTER_Y, "Page " + pageNumber + " of " + totalPages);
    text(stream, "F1", 8, 560, FOOTER_Y, page.header().title() + " Session ID " + (page.header().sessionId() == null ? "" : page.header().sessionId()));
    byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
    return ("<< /Length " + streamBytes.length + " >>\nstream\n" + new String(streamBytes, StandardCharsets.ISO_8859_1) + "endstream").getBytes(StandardCharsets.ISO_8859_1);
  }

  private void drawHeader(StringBuilder stream, PdfHeader header) {
    text(stream, "F2", 20, MARGIN, 566, header.title());
    text(stream, "F1", 9, MARGIN, 544, "Biangbiang Restaurant Operations");
    line(stream, MARGIN, 536, PAGE_WIDTH - MARGIN, 536);
    text(stream, "F2", 9, MARGIN, 516, "Date:");
    text(stream, "F1", 9, MARGIN + 34, 516, header.date());
    text(stream, "F2", 9, 258, 516, "Location:");
    text(stream, "F1", 9, 310, 516, header.location());
    text(stream, "F2", 9, 420, 516, "Business:");
    text(stream, "F1", 9, 478, 516, truncate(header.business(), 24));
    text(stream, "F2", 9, MARGIN, 500, header.actorLabel() + ":");
    text(stream, "F1", 9, MARGIN + 82, 500, truncate(header.actor(), 32));
    text(stream, "F2", 9, 258, 500, "Status:");
    text(stream, "F1", 9, 306, 500, header.status());
  }

  private void drawTable(StringBuilder stream, List<TableRow> rows) {
    double tableLeft = MARGIN;
    double tableWidth = PAGE_WIDTH - MARGIN * 2;
    double[] widths = {150, 398, 70, tableWidth - 150 - 398 - 70};
    double[] x = {tableLeft, tableLeft + widths[0], tableLeft + widths[0] + widths[1], tableLeft + widths[0] + widths[1] + widths[2], tableLeft + tableWidth};

    fill(stream, tableLeft, TABLE_TOP, tableWidth, ROW_HEIGHT, "0.90 0.92 0.94");
    rect(stream, tableLeft, TABLE_TOP, tableWidth, ROW_HEIGHT);
    text(stream, "F2", 9, x[0] + 6, TABLE_TOP + 7, "Vendor");
    text(stream, "F2", 9, x[1] + 6, TABLE_TOP + 7, "ITEM");
    text(stream, "F2", 9, x[2] + 6, TABLE_TOP + 7, "Qty.");
    text(stream, "F2", 9, x[3] + 6, TABLE_TOP + 7, "Unit");
    for (double columnX : x) {
      line(stream, columnX, TABLE_TOP, columnX, TABLE_TOP + ROW_HEIGHT);
    }

    double y = TABLE_TOP - ROW_HEIGHT;
    for (TableRow row : rows) {
      if (row.vendorHeader()) {
        fill(stream, tableLeft, y, tableWidth, ROW_HEIGHT, "0.96 0.96 0.92");
        rect(stream, tableLeft, y, tableWidth, ROW_HEIGHT);
        text(stream, "F2", 8, x[0] + 6, y + 7, "Vendor: " + truncate(row.vendor(), 70));
      } else {
        rect(stream, tableLeft, y, tableWidth, ROW_HEIGHT);
        for (double columnX : x) {
          line(stream, columnX, y, columnX, y + ROW_HEIGHT);
        }
        text(stream, "F1", 8, x[0] + 6, y + 7, truncate(row.vendor(), 24));
        text(stream, "F1", 8, x[1] + 6, y + 7, truncate(row.item(), 62));
        text(stream, "F1", 8, x[2] + 6, y + 7, row.quantity());
        text(stream, "F1", 8, x[3] + 6, y + 7, row.unit());
      }
      y -= ROW_HEIGHT;
    }
  }

  private void text(StringBuilder stream, String font, int size, double x, double y, String value) {
    stream.append("BT\n/")
        .append(font)
        .append(" ")
        .append(size)
        .append(" Tf\n")
        .append(n(x))
        .append(" ")
        .append(n(y))
        .append(" Td\n(")
        .append(escape(safeText(value)))
        .append(") Tj\nET\n");
  }

  private void rect(StringBuilder stream, double x, double y, double width, double height) {
    stream.append("0.70 0.70 0.70 RG\n0.5 w\n")
        .append(n(x)).append(" ").append(n(y)).append(" ").append(n(width)).append(" ").append(n(height)).append(" re S\n");
  }

  private void fill(StringBuilder stream, double x, double y, double width, double height, String rgb) {
    stream.append(rgb).append(" rg\n")
        .append(n(x)).append(" ").append(n(y)).append(" ").append(n(width)).append(" ").append(n(height)).append(" re f\n")
        .append("0 0 0 rg\n");
  }

  private void line(StringBuilder stream, double x1, double y1, double x2, double y2) {
    stream.append("0.70 0.70 0.70 RG\n0.5 w\n")
        .append(n(x1)).append(" ").append(n(y1)).append(" m ")
        .append(n(x2)).append(" ").append(n(y2)).append(" l S\n");
  }

  private String n(double value) {
    if (value == Math.rint(value)) return String.valueOf((int) value);
    return String.format(java.util.Locale.ROOT, "%.2f", value);
  }

  private String date(InventoryCountSession session) {
    if (session.getCompletedAt() != null) return STAMP.format(session.getCompletedAt());
    if (session.getSubmittedAt() != null) return STAMP.format(session.getSubmittedAt());
    return session.getBusinessDate().toString();
  }

  private String countedBy(InventoryCountSession session) {
    if (session.getCompletedByNameSnapshot() != null && !session.getCompletedByNameSnapshot().isBlank()) return session.getCompletedByNameSnapshot();
    if (session.getSubmittedByNameSnapshot() != null && !session.getSubmittedByNameSnapshot().isBlank()) return session.getSubmittedByNameSnapshot();
    if (session.getCreatedBy() != null) return session.getCreatedBy().getDisplayName();
    return "Unknown employee";
  }

  private String orderDate(OrderPlanSession session) {
    return session.getSubmittedAt() == null ? session.getBusinessDate().toString() : STAMP.format(session.getSubmittedAt());
  }

  private String submittedBy(OrderPlanSession session) {
    if (session.getSubmittedByNameSnapshot() != null && !session.getSubmittedByNameSnapshot().isBlank()) return session.getSubmittedByNameSnapshot();
    return session.getCreatedBy() == null ? "Unknown employee" : session.getCreatedBy().getDisplayName();
  }

  private void write(ByteArrayOutputStream out, String value) {
    out.writeBytes(value.getBytes(StandardCharsets.ISO_8859_1));
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

  private String truncate(String value, int max) {
    String safe = safeText(value);
    return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 3)) + "...";
  }

  private String blank(String value) {
    return value == null ? "" : value;
  }

  private String decimal(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  private record PdfHeader(String title, Long sessionId, String date, String location, String business, String actorLabel, String actor, String status) {}

  private record PageModel(PdfHeader header, Instant generatedAt, List<TableRow> rows) {}

  private record TableRow(boolean vendorHeader, String vendor, String item, String quantity, String unit) {
    static TableRow vendor(String vendor) {
      return new TableRow(true, vendor, "", "", "");
    }

    static TableRow item(String vendor, String item, String quantity, String unit) {
      return new TableRow(false, vendor, item, quantity, unit);
    }
  }
}
