package edu.unev.barberia.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Exportador PDF vectorial sin dependencias externas. Texto seleccionable, páginas numeradas. */
public final class PdfReport {
  public static List<String> lines(String title, List<Map<String, Object>> rows) {
    List<String> out = new ArrayList<>();
    out.add("BARBERÍA UNEV 3.0 · " + title);
    out.add("Generado: " + java.time.LocalDateTime.now().withNano(0));
    out.add("");
    if (rows.isEmpty()) out.add("Sin registros para los filtros seleccionados.");
    if (title.startsWith("Factura") && !rows.isEmpty()) {
      var first = rows.getFirst();
      for (String key : List.of("numero", "fecha", "cliente", "metodo", "estado"))
        out.add(key.toUpperCase(Locale.ROOT) + ": " + Objects.toString(first.get(key), ""));
      out.add("");
      out.add("CANTIDAD   PRODUCTO                                  PRECIO UNITARIO");
      for (var row : rows)
        out.add(
            Objects.toString(row.get("cantidad"))
                + "   "
                + Objects.toString(row.get("producto"))
                + "   RD$"
                + Objects.toString(row.get("precio")));
      out.add("");
      for (String key : List.of("descuento", "impuesto", "total", "recibido", "cambio"))
        out.add(key.toUpperCase(Locale.ROOT) + ": RD$" + Objects.toString(first.get(key), "0"));
      out.add("");
      out.add("Comprobante académico de práctica. Sin numeración fiscal certificada.");
      return out;
    }
    int index = 1;
    for (var row : rows) {
      out.add("Registro " + index++);
      for (var e : row.entrySet()) {
        String text = e.getKey().replace('_', ' ') + ": " + Objects.toString(e.getValue(), "—");
        while (text.length() > 95) {
          out.add(text.substring(0, 95));
          text = text.substring(95);
        }
        out.add(text);
      }
      out.add("");
    }
    return out;
  }

  static String escape(String s) {
    return s.replace("\\", "\\\\")
        .replace("(", "\\(")
        .replace(")", "\\)")
        .replace('\n', ' ')
        .replace('\r', ' ')
        .replace("—", "-")
        .replace("→", "-");
  }

  public static void write(Path path, String title, List<Map<String, Object>> rows)
      throws IOException {
    List<String> lines = lines(title, rows);
    int pages = Math.max(1, (lines.size() + 43) / 44);
    List<byte[]> objects = new ArrayList<>();
    objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
    StringBuilder kids = new StringBuilder();
    for (int i = 0; i < pages; i++) kids.append(4 + i * 2).append(" 0 R ");
    objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pages + " >>"));
    objects.add(
        bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
    for (int i = 0; i < pages; i++) {
      int content = 5 + i * 2;
      objects.add(
          bytes(
              "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 3 0"
                  + " R >> >> /Contents "
                  + content
                  + " 0 R >>"));
      StringBuilder body = new StringBuilder("BT /F1 10 Tf 48 748 Td 15 TL\n");
      for (int j = i * 44; j < Math.min(lines.size(), (i + 1) * 44); j++)
        body.append('(').append(escape(lines.get(j))).append(") Tj T*\n");
      body.append("ET\nBT /F1 9 Tf 48 35 Td (Página ")
          .append(i + 1)
          .append(" de ")
          .append(pages)
          .append(") Tj ET\n");
      byte[] data = bytes(body.toString());
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      stream.write(bytes("<< /Length " + data.length + " >>\nstream\n"));
      stream.write(data);
      stream.write(bytes("endstream"));
      objects.add(stream.toByteArray());
    }
    ByteArrayOutputStream pdf = new ByteArrayOutputStream();
    pdf.write(bytes("%PDF-1.4\n"));
    List<Integer> offsets = new ArrayList<>();
    for (int i = 0; i < objects.size(); i++) {
      offsets.add(pdf.size());
      pdf.write(bytes((i + 1) + " 0 obj\n"));
      pdf.write(objects.get(i));
      pdf.write(bytes("\nendobj\n"));
    }
    int xref = pdf.size();
    pdf.write(bytes("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n"));
    for (int off : offsets) pdf.write(bytes(String.format(Locale.ROOT, "%010d 00000 n \n", off)));
    pdf.write(
        bytes(
            "trailer << /Size "
                + (objects.size() + 1)
                + " /Root 1 0 R >>\nstartxref\n"
                + xref
                + "\n%%EOF\n"));
    Files.write(path, pdf.toByteArray());
  }

  static byte[] bytes(String s) {
    return s.getBytes(StandardCharsets.ISO_8859_1);
  }
}
