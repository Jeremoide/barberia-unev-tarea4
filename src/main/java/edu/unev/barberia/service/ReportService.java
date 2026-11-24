package edu.unev.barberia.service;

import edu.unev.barberia.security.Session;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ReportService {
  public Path export(Session s, LocalDate from, LocalDate to, Path file) throws Exception {
    var rows = new BarberiaService().report(s, from, to);
    try (var book = new XSSFWorkbook()) {
      var sheet = book.createSheet("Reporte");
      sheet.createRow(0).createCell(0).setCellValue("Barbería UNEV · Reporte de citas");
      sheet
          .createRow(1)
          .createCell(0)
          .setCellValue(
              "Generado: "
                  + LocalDateTime.now()
                  + " · Usuario: "
                  + s.usuario()
                  + " · Rol: "
                  + s.rol());
      sheet.createRow(2).createCell(0).setCellValue("Desde " + from + " hasta " + to);
      List<String> cols =
          rows.isEmpty()
              ? (s.barbero()
                  ? List.of("fecha", "servicio", "estado")
                  : List.of("fecha", "cliente", "barbero", "servicio", "estado", "cobrado"))
              : new ArrayList<>(rows.getFirst().keySet());
      var head = sheet.createRow(4);
      var style = book.createCellStyle();
      var font = book.createFont();
      font.setBold(true);
      style.setFont(font);
      int col = 0;
      for (String key : cols) {
        var cell = head.createCell(col++);
        cell.setCellValue(key);
        cell.setCellStyle(style);
      }
      int row = 5;
      for (var value : rows) {
        var rr = sheet.createRow(row++);
        col = 0;
        for (String key : cols) {
          Object v = value.get(key);
          var cell = rr.createCell(col++);
          if (v instanceof Number n) cell.setCellValue(n.doubleValue());
          else cell.setCellValue(Objects.toString(v, ""));
        }
      }
      for (int i = 0; i < cols.size(); i++) sheet.setColumnWidth(i, i == 0 ? 6000 : 5000);
      sheet.createFreezePane(0, 5);
      sheet.setAutoFilter(
          new org.apache.poi.ss.util.CellRangeAddress(4, Math.max(4, row - 1), 0, cols.size() - 1));
      try (var out = Files.newOutputStream(file)) {
        book.write(out);
      }
    }
    return file;
  }
}
