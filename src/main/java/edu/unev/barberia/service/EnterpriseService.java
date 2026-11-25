package edu.unev.barberia.service;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.Session;
import java.math.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Procesos 3.0. Autorización, bloqueos e integridad se verifican también fuera de la interfaz. */
public final class EnterpriseService {
  public record Line(long product, int quantity) {}

  public record Totals(
      BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total) {}

  public static BigDecimal money(BigDecimal n) {
    if (n == null || n.signum() < 0 || n.scale() > 2 || n.compareTo(new BigDecimal("99999999")) > 0)
      throw new IllegalArgumentException("Importe entre 0 y 99,999,999 con dos decimales.");
    return n.setScale(2);
  }

  public static Totals totals(BigDecimal subtotal, BigDecimal discount, BigDecimal rate) {
    subtotal = money(subtotal);
    discount = money(discount);
    if (discount.compareTo(subtotal) > 0
        || rate == null
        || rate.signum() < 0
        || rate.compareTo(new BigDecimal("100")) > 0)
      throw new IllegalArgumentException("Descuento o porcentaje inválido.");
    BigDecimal tax =
        subtotal
            .subtract(discount)
            .multiply(rate)
            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    return new Totals(subtotal, discount, tax, subtotal.subtract(discount).add(tax));
  }

  public static void quantity(int n) {
    if (n < 1 || n > 10000) throw new IllegalArgumentException("Cantidad entre 1 y 10,000.");
  }

  private void access(Session s, boolean admin) throws Exception {
    s.require(
        admin ? new String[] {"ADMINISTRADOR"} : new String[] {"ADMINISTRADOR", "RECEPCIONISTA"});
    try (var c = Database.connection()) {
      var u =
          Store.one(
              c,
              "SELECT u.activo,r.nombre rol FROM usuarios u JOIN roles r ON r.id=u.rol_id WHERE"
                  + " u.id=?",
              s.id());
      if (u == null || !Store.yes(u, "activo") || !Store.text(u, "rol").equals(s.rol()))
        throw new SecurityException("La cuenta cambió; vuelva a iniciar sesión.");
    }
  }

  public static void audit(Connection c, Session s, String action, Object ref) throws Exception {
    Store.insert(
        c,
        "INSERT INTO auditoria(usuario_id,accion,referencia) VALUES (?,?,?)",
        s.id(),
        action,
        String.valueOf(ref));
  }

  public void supplier(Session s, String name, String phone) throws Exception {
    access(s, true);
    String n = BarberiaService.required(name, "Proveedor");
    if (phone == null || !phone.matches("[+0-9 ()-]{7,30}"))
      throw new IllegalArgumentException("Teléfono inválido.");
    Store.tx(c -> {
      long id = Store.insert(c, "INSERT INTO proveedores(nombre,telefono) VALUES (?,?)", n, phone);
      audit(c, s, "PROVEEDOR_CREADO", id);
      return null;
    });
  }

  public void product(
      Session s, String code, String name, String category, BigDecimal price, int minimum)
      throws Exception {
    access(s, true);
    String n = BarberiaService.required(name, "Producto"),
        cat = BarberiaService.required(category, "Categoría");
    if (code == null
        || !code.matches("[A-Za-z0-9_-]{2,40}")
        || minimum < 0
        || minimum > 10000
        || money(price).signum() == 0)
      throw new IllegalArgumentException("Código, precio o mínimo inválido.");
    Store.tx(c -> {
      long id =
          Store.insert(
              c,
              "INSERT INTO productos(codigo,nombre,categoria,precio,minimo) VALUES (?,?,?,?,?)",
              code,
              n,
              cat,
              price,
              minimum);
      audit(c, s, "PRODUCTO_CREADO", id);
      return null;
    });
  }

  public long purchase(
      Session s,
      long supplier,
      long product,
      String lot,
      LocalDate expiry,
      int qty,
      BigDecimal cost)
      throws Exception {
    access(s, true);
    quantity(qty);
    money(cost);
    String label = BarberiaService.required(lot, "Lote");
    if (label.length() > 60
        || cost.signum() == 0
        || expiry == null
        || expiry.isBefore(LocalDate.now()))
      throw new IllegalArgumentException("Lote, vencimiento o costo inválido.");
    return Store.tx(c -> {
      if (Store.one(c, "SELECT id FROM proveedores WHERE id=? AND activo=1", supplier) == null
          || Store.one(c, "SELECT id FROM productos WHERE id=? AND activo=1 FOR UPDATE", product)
              == null) throw new IllegalArgumentException("Proveedor o producto no disponible.");
      long buy =
          Store.insert(
              c,
              "INSERT INTO compras(proveedor_id,usuario_id,total) VALUES (?,?,?)",
              supplier,
              s.id(),
              cost.multiply(BigDecimal.valueOf(qty)));
      Store.insert(
          c,
          "INSERT INTO lotes(producto_id,compra_id,codigo,vencimiento,costo,cantidad) VALUES"
              + " (?,?,?,?,?,?)",
          product,
          buy,
          label,
          expiry,
          cost,
          qty);
      audit(c, s, "COMPRA", buy);
      return buy;
    });
  }

  private Map<String, Object> box(Connection c, Session s) throws Exception {
    var box =
        Store.one(
            c, "SELECT * FROM cajas WHERE usuario_id=? AND cierre IS NULL FOR UPDATE", s.id());
    if (box == null) throw new IllegalArgumentException("Abra su caja antes de cobrar.");
    return box;
  }

  public long open(Session s, BigDecimal opening) throws Exception {
    access(s, false);
    money(opening);
    return Store.tx(c -> {
      Store.one(c, "SELECT id FROM usuarios WHERE id=? FOR UPDATE", s.id());
      if (Store.one(c, "SELECT id FROM cajas WHERE usuario_id=? AND cierre IS NULL", s.id())
          != null) throw new IllegalArgumentException("Ya tiene una caja abierta.");
      long id =
          Store.insert(c, "INSERT INTO cajas(usuario_id,fondo) VALUES (?,?)", s.id(), opening);
      audit(c, s, "APERTURA_CAJA", id);
      return id;
    });
  }

  public BigDecimal close(Session s, BigDecimal counted) throws Exception {
    access(s, false);
    money(counted);
    return Store.tx(c -> {
      var b = box(c, s);
      long id = Store.id(b, "id");
      var r =
          Store.one(
              c,
              "SELECT COALESCE(SUM(importe),0) importe FROM movimientos_caja WHERE caja_id=? AND"
                  + " metodo='EFECTIVO'",
              id);
      BigDecimal expected =
          new BigDecimal(b.get("fondo").toString())
              .add(new BigDecimal(r.get("importe").toString()));
      Store.update(
          c,
          "UPDATE cajas SET cierre=CURRENT_TIMESTAMP,esperado=?,contado=?,diferencia=? WHERE id=?",
          expected,
          counted,
          counted.subtract(expected),
          id);
      audit(c, s, "CIERRE_CAJA", id);
      return counted.subtract(expected);
    });
  }

  public long sell(
      Session s,
      long client,
      List<Line> lines,
      BigDecimal discount,
      BigDecimal rate,
      String method,
      BigDecimal received,
      boolean redeem)
      throws Exception {
    access(s, false);
    if (lines == null || lines.isEmpty() || lines.size() > 100)
      throw new IllegalArgumentException("El carrito debe contener entre 1 y 100 productos.");
    if (!List.of("EFECTIVO", "TARJETA", "TRANSFERENCIA").contains(method))
      throw new IllegalArgumentException("Método de pago inválido.");
    money(received);
    money(discount);
    return Store.tx(c -> {
      var b = box(c, s);
      long bid = Store.id(b, "id");
      if (Store.one(c, "SELECT id FROM clientes WHERE id=? AND activo=1 FOR UPDATE", client)
          == null) throw new IllegalArgumentException("Cliente inactivo o inexistente.");
      // Orden global de bloqueo: evita interbloqueos de carritos con distinto orden visual.
      TreeMap<Long, Integer> grouped = new TreeMap<>();
      for (var l : lines) {
        quantity(l.quantity());
        grouped.merge(l.product(), l.quantity(), Integer::sum);
      }
      record Pick(long lot, int qty, BigDecimal price) {}
      List<Pick> picks = new ArrayList<>();
      BigDecimal subtotal = BigDecimal.ZERO;
      for (var entry : grouped.entrySet()) {
        quantity(entry.getValue());
        var p =
            Store.one(
                c, "SELECT * FROM productos WHERE id=? AND activo=1 FOR UPDATE", entry.getKey());
        if (p == null) throw new IllegalArgumentException("Producto no disponible.");
        int remaining = entry.getValue();
        BigDecimal price = new BigDecimal(p.get("precio").toString());
        var lots =
            Store.query(
                c,
                "SELECT id,cantidad FROM lotes WHERE producto_id=? AND vencimiento>=CURRENT_DATE"
                    + " AND cantidad>0 ORDER BY vencimiento,id FOR UPDATE",
                entry.getKey());
        for (var l : lots) {
          int take = Math.min(remaining, (int) Store.id(l, "cantidad"));
          if (take > 0) picks.add(new Pick(Store.id(l, "id"), take, price));
          remaining -= take;
          if (remaining == 0) break;
        }
        if (remaining > 0)
          throw new IllegalArgumentException(
              "Existencias insuficientes o lotes vencidos: " + Store.text(p, "nombre"));
        subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
      }
      BigDecimal appliedDiscount = discount;
      var rule = Store.one(c, "SELECT * FROM reglas_puntos WHERE id=1");
      int threshold = (int) Store.id(rule, "umbral");
      BigDecimal bonus = new BigDecimal(rule.get("bono").toString());
      if (redeem) {
        var points =
            Store.one(
                c, "SELECT COALESCE(SUM(valor),0) saldo FROM puntos WHERE cliente_id=?", client);
        if (Store.id(points, "saldo") < threshold)
          throw new IllegalArgumentException("El cliente no tiene puntos suficientes.");
        appliedDiscount = appliedDiscount.add(bonus);
      }
      var total = totals(subtotal, appliedDiscount, rate);
      if (total.total().signum() <= 0)
        throw new IllegalArgumentException("El total debe ser positivo.");
      if (method.equals("EFECTIVO") && received.compareTo(total.total()) < 0)
        throw new IllegalArgumentException("Efectivo recibido insuficiente.");
      BigDecimal paid = method.equals("EFECTIVO") ? received : total.total();
      long sale =
          Store.insert(
              c,
              "INSERT INTO"
                  + " ventas(numero,cliente_id,caja_id,usuario_id,subtotal,descuento,impuesto,total,metodo,recibido,cambio)"
                  + " VALUES (?,?,?,?,?,?,?,?,?,?,?)",
              "V3-" + UUID.randomUUID().toString(),
              client,
              bid,
              s.id(),
              subtotal,
              total.discount(),
              total.tax(),
              total.total(),
              method,
              paid,
              paid.subtract(total.total()));
      for (var p : picks) {
        Store.update(c, "UPDATE lotes SET cantidad=cantidad-? WHERE id=?", p.qty(), p.lot());
        Store.insert(
            c,
            "INSERT INTO venta_detalles(venta_id,lote_id,cantidad,precio) VALUES (?,?,?,?)",
            sale,
            p.lot(),
            p.qty(),
            p.price());
      }
      Store.insert(
          c,
          "INSERT INTO movimientos_caja(caja_id,usuario_id,venta_id,importe,concepto,metodo) VALUES"
              + " (?,?,?,?,?,?)",
          bid,
          s.id(),
          sale,
          total.total(),
          "VENTA",
          method);
      int points =
          total
              .total()
              .divide(new BigDecimal(rule.get("valor_compra").toString()), 0, RoundingMode.DOWN)
              .intValue();
      Store.insert(
          c,
          "INSERT INTO puntos(cliente_id,venta_id,valor,concepto) VALUES (?,?,?,?)",
          client,
          sale,
          points,
          "COMPRA");
      if (redeem)
        Store.insert(
            c,
            "INSERT INTO puntos(cliente_id,venta_id,valor,concepto) VALUES (?,?,?,?)",
            client,
            sale,
            -threshold,
            "CANJE");
      audit(c, s, "VENTA", sale);
      return sale;
    });
  }

  public void cancelSale(Session s, long sale, String reason) throws Exception {
    access(s, true);
    String why = BarberiaService.required(reason, "Motivo");
    Store.tx(c -> {
      var v = Store.one(c, "SELECT * FROM ventas WHERE id=? FOR UPDATE", sale);
      if (v == null || !Store.text(v, "estado").equals("EMITIDA"))
        throw new IllegalArgumentException("Solo se anula una venta emitida una vez.");
      var b = Store.one(c, "SELECT * FROM cajas WHERE id=? FOR UPDATE", v.get("caja_id"));
      if (b.get("cierre") != null)
        throw new IllegalArgumentException("La caja ya cerró; no se modifica el arqueo histórico.");
      long client = Store.id(v, "cliente_id");
      Store.one(c, "SELECT id FROM clientes WHERE id=? FOR UPDATE", client);
      var ps = Store.one(c, "SELECT COALESCE(SUM(valor),0) n FROM puntos WHERE venta_id=?", sale);
      var balance =
          Store.one(c, "SELECT COALESCE(SUM(valor),0) n FROM puntos WHERE cliente_id=?", client);
      if (Store.id(balance, "n") - Store.id(ps, "n") < 0)
        throw new IllegalArgumentException(
            "Los puntos de esta compra ya fueron canjeados; requiere resolver el canje primero.");
      for (var d :
          Store.query(c, "SELECT * FROM venta_detalles WHERE venta_id=? ORDER BY lote_id", sale))
        Store.update(
            c,
            "UPDATE lotes SET cantidad=cantidad+? WHERE id=?",
            d.get("cantidad"),
            d.get("lote_id"));
      Store.update(c, "UPDATE ventas SET estado='ANULADA',motivo=? WHERE id=?", why, sale);
      Store.insert(
          c,
          "INSERT INTO movimientos_caja(caja_id,usuario_id,venta_id,importe,concepto,metodo) VALUES"
              + " (?,?,?,?,?,?)",
          v.get("caja_id"),
          s.id(),
          sale,
          new BigDecimal(v.get("total").toString()).negate(),
          "ANULACION",
          v.get("metodo"));
      Store.insert(
          c,
          "INSERT INTO puntos(cliente_id,venta_id,valor,concepto) VALUES (?,?,?,?)",
          client,
          sale,
          -Store.id(ps, "n"),
          "ANULACION");
      audit(c, s, "ANULACION_VENTA", sale);
      return null;
    });
  }

  public void reminder(
      Session s,
      long client,
      String title,
      String category,
      String priority,
      LocalDateTime due,
      int days)
      throws Exception {
    access(s, false);
    String t = BarberiaService.required(title, "Actividad"),
        cat = BarberiaService.required(category, "Categoría");
    if (cat.length() > 40
        || due == null
        || days < 0
        || days > 365
        || !List.of("ALTA", "MEDIA", "BAJA").contains(priority))
      throw new IllegalArgumentException("Fecha, recurrencia o prioridad inválida.");
    Store.tx(c -> {
      long id =
          Store.insert(
              c,
              "INSERT INTO"
                  + " recordatorios(cliente_id,usuario_id,titulo,categoria,prioridad,fecha,repetir_dias)"
                  + " VALUES (?,?,?,?,?,?,?)",
              client,
              s.id(),
              t,
              cat,
              priority,
              due,
              days);
      audit(c, s, "RECORDATORIO_CREADO", id);
      return null;
    });
  }

  public void resolveReminder(Session s, long id, boolean postpone) throws Exception {
    access(s, false);
    Store.tx(c -> {
      var r = Store.one(c, "SELECT * FROM recordatorios WHERE id=? FOR UPDATE", id);
      if (r == null || !Store.text(r, "estado").equals("PENDIENTE"))
        throw new IllegalArgumentException("El recordatorio ya fue procesado.");
      Store.update(
          c,
          "UPDATE recordatorios SET estado=? WHERE id=?",
          postpone ? "POSPUESTA" : "COMPLETADA",
          id);
      long days = postpone ? 1 : Store.id(r, "repetir_dias");
      if (days > 0)
        Store.insert(
            c,
            "INSERT INTO"
                + " recordatorios(cliente_id,usuario_id,titulo,categoria,prioridad,fecha,repetir_dias)"
                + " VALUES (?,?,?,?,?,DATE_ADD(?,INTERVAL ? DAY),?)",
            r.get("cliente_id"),
            s.id(),
            r.get("titulo"),
            r.get("categoria"),
            r.get("prioridad"),
            r.get("fecha"),
            days,
            r.get("repetir_dias"));
      audit(c, s, postpone ? "RECORDATORIO_POSPUESTO" : "RECORDATORIO_COMPLETADO", id);
      return null;
    });
  }

  public Map<Long, BigDecimal> prices(Session s) throws Exception {
    access(s, false);
    try (var c = Database.connection()) {
      Map<Long, BigDecimal> out = new HashMap<>();
      for (var r : Store.query(c, "SELECT id,precio FROM productos WHERE activo=1"))
        out.put(Store.id(r, "id"), new BigDecimal(r.get("precio").toString()));
      return out;
    }
  }

  public Map<String, Number> summary(Session s) throws Exception {
    access(s, false);
    try (var c = Database.connection()) {
      Map<String, Number> result = new LinkedHashMap<>();
      result.put(
          "Productos con stock bajo",
          (Number)
              Store.one(
                      c,
                      "SELECT COUNT(*) n FROM productos p WHERE activo=1 AND (SELECT"
                          + " COALESCE(SUM(cantidad),0) FROM lotes l WHERE l.producto_id=p.id AND"
                          + " l.vencimiento>=CURRENT_DATE)<=p.minimo")
                  .get("n"));
      result.put(
          "Unidades vencidas",
          (Number)
              Store.one(
                      c,
                      "SELECT COALESCE(SUM(cantidad),0) n FROM lotes WHERE"
                          + " vencimiento<CURRENT_DATE")
                  .get("n"));
      result.put(
          "Seguimientos vencidos",
          (Number)
              Store.one(
                      c,
                      "SELECT COUNT(*) n FROM recordatorios WHERE estado='PENDIENTE' AND"
                          + " fecha<CURRENT_TIMESTAMP")
                  .get("n"));
      result.put(
          "Ventas de hoy RD$",
          (Number)
              Store.one(
                      c,
                      "SELECT COALESCE(SUM(total),0) n FROM ventas WHERE estado='EMITIDA' AND"
                          + " fecha>=CURRENT_DATE")
                  .get("n"));
      return result;
    }
  }

  public List<BarberiaService.Option> catalog(Session s, String kind) throws Exception {
    access(s, kind.equals("proveedores") || kind.equals("empleados"));
    String sql =
        switch (kind) {
          case "productos" ->
              "SELECT id,CONCAT(codigo,' · ',nombre) nombre FROM productos WHERE activo=1 ORDER BY"
                  + " nombre";
          case "proveedores" -> "SELECT id,nombre FROM proveedores WHERE activo=1 ORDER BY nombre";
          case "empleados" -> "SELECT id,nombre FROM empleados ORDER BY nombre";
          default -> "SELECT id,nombre FROM clientes WHERE activo=1 ORDER BY nombre";
        };
    try (var c = Database.connection()) {
      return Store.query(c, sql).stream()
          .map(r -> new BarberiaService.Option(Store.id(r, "id"), Store.text(r, "nombre")))
          .toList();
    }
  }

  public void editProduct(
      Session s, long id, String name, String category, BigDecimal price, int minimum)
      throws Exception {
    access(s, true);
    String n = BarberiaService.required(name, "Nombre"),
        cat = BarberiaService.required(category, "Categoría");
    money(price);
    if (price.signum() == 0 || minimum < 0 || minimum > 10000)
      throw new IllegalArgumentException("Precio o mínimo inválido.");
    Store.tx(c -> {
      if (Store.update(
              c,
              "UPDATE productos SET nombre=?,categoria=?,precio=?,minimo=? WHERE id=? AND activo=1",
              n,
              cat,
              price,
              minimum,
              id)
          != 1) throw new IllegalArgumentException("Producto no disponible.");
      audit(c, s, "PRODUCTO_EDITADO", id);
      return null;
    });
  }

  public void deactivateProduct(Session s, long id) throws Exception {
    access(s, true);
    Store.tx(c -> {
      Store.one(c, "SELECT id FROM productos WHERE id=? FOR UPDATE", id);
      if (Store.id(
              Store.one(c, "SELECT COALESCE(SUM(cantidad),0) n FROM lotes WHERE producto_id=?", id),
              "n")
          > 0)
        throw new IllegalArgumentException(
            "Resuelva el inventario del producto antes de desactivarlo.");
      Store.update(c, "UPDATE productos SET activo=0 WHERE id=?", id);
      audit(c, s, "PRODUCTO_DESACTIVADO", id);
      return null;
    });
  }

  public void adjust(Session s, long lot, int counted, String reason) throws Exception {
    access(s, true);
    String why = BarberiaService.required(reason, "Motivo");
    if (counted < 0 || counted > 10000)
      throw new IllegalArgumentException("Existencia entre 0 y 10,000.");
    Store.tx(c -> {
      var l = Store.one(c, "SELECT * FROM lotes WHERE id=? FOR UPDATE", lot);
      if (l == null) throw new IllegalArgumentException("Lote inexistente.");
      Store.update(c, "UPDATE lotes SET cantidad=? WHERE id=?", counted, lot);
      audit(c, s, "AJUSTE_LOTE", lot + " de " + l.get("cantidad") + " a " + counted + " · " + why);
      return null;
    });
  }

  public void shift(Session s, long employee, int day, LocalTime start, LocalTime end)
      throws Exception {
    access(s, true);
    if (day < 1 || day > 7 || start == null || end == null || !end.isAfter(start))
      throw new IllegalArgumentException("Día entre 1 y 7, con hora final posterior a la inicial.");
    Store.tx(c -> {
      Store.one(c, "SELECT id FROM empleados WHERE id=? FOR UPDATE", employee);
      if (Store.id(
              Store.one(
                  c,
                  "SELECT COUNT(*) n FROM citas WHERE empleado_id=? AND estado='PENDIENTE' AND"
                      + " fecha>CURRENT_TIMESTAMP AND WEEKDAY(fecha)+1=? AND (TIME(fecha)<? OR"
                      + " TIME(DATE_ADD(fecha,INTERVAL duracion_acordada MINUTE))>?)",
                  employee,
                  day,
                  start,
                  end),
              "n")
          > 0)
        throw new IllegalArgumentException(
            "Reprograme primero las citas que quedarían fuera del horario.");
      Store.update(
          c, "DELETE FROM horarios_trabajo WHERE empleado_id=? AND dia_semana=?", employee, day);
      Store.insert(
          c,
          "INSERT INTO horarios_trabajo(empleado_id,dia_semana,hora_inicio,hora_fin) VALUES"
              + " (?,?,?,?)",
          employee,
          day,
          start,
          end);
      audit(c, s, "HORARIO_EDITADO", employee + " día " + day);
      return null;
    });
  }

  public List<Map<String, Object>> page(
      Session s,
      String module,
      String search,
      String state,
      LocalDate from,
      LocalDate to,
      int offset)
      throws Exception {
    access(s, Set.of("Compras", "Proveedores", "Auditoría", "Equipo", "Horarios").contains(module));
    BarberiaService.range(from, to);
    if (offset < 0) throw new IllegalArgumentException("Página inválida.");
    String q = "%" + Objects.toString(search, "").trim() + "%";
    try (var c = Database.connection()) {
      return switch (module) {
        case "Equipo" ->
            Store.query(
                c,
                "SELECT e.id,e.nombre,e.especialidad,u.usuario,u.activo FROM empleados e JOIN"
                    + " usuarios u ON u.id=e.usuario_id WHERE e.nombre LIKE ? ORDER BY e.nombre"
                    + " LIMIT 30 OFFSET ?",
                q,
                offset);
        case "Horarios" ->
            Store.query(
                c,
                "SELECT h.id,e.nombre,h.dia_semana,h.hora_inicio,h.hora_fin FROM horarios_trabajo h"
                    + " JOIN empleados e ON e.id=h.empleado_id WHERE e.nombre LIKE ? ORDER BY"
                    + " e.nombre,h.dia_semana LIMIT 30 OFFSET ?",
                q,
                offset);
        case "Inventario" ->
            Store.query(
                c,
                "SELECT p.id,p.codigo,p.nombre,p.categoria,p.precio,p.minimo,COALESCE(SUM(CASE WHEN"
                    + " l.vencimiento>=CURRENT_DATE THEN l.cantidad ELSE 0 END),0)"
                    + " disponible,COALESCE(SUM(CASE WHEN l.vencimiento<CURRENT_DATE THEN"
                    + " l.cantidad ELSE 0 END),0) vencido FROM productos p LEFT JOIN lotes l ON"
                    + " l.producto_id=p.id WHERE p.activo=1 AND (p.nombre LIKE ? OR p.codigo LIKE ?"
                    + " OR p.categoria LIKE ?) GROUP BY"
                    + " p.id,p.codigo,p.nombre,p.categoria,p.precio,p.minimo ORDER BY p.nombre"
                    + " LIMIT 30 OFFSET ?",
                q,
                q,
                q,
                offset);
        case "Lotes" ->
            Store.query(
                c,
                "SELECT l.id,p.nombre producto,l.codigo,l.vencimiento,l.costo,l.cantidad FROM lotes"
                    + " l JOIN productos p ON p.id=l.producto_id WHERE (p.nombre LIKE ? OR l.codigo"
                    + " LIKE ?) AND (?='TODOS' OR (?='VENCIDO' AND l.vencimiento<CURRENT_DATE) OR"
                    + " (?='VIGENTE' AND l.vencimiento>=CURRENT_DATE)) ORDER BY l.vencimiento,l.id"
                    + " LIMIT 30 OFFSET ?",
                q,
                q,
                state,
                state,
                state,
                offset);
        case "Proveedores" ->
            Store.query(
                c,
                "SELECT id,nombre,telefono,activo FROM proveedores WHERE nombre LIKE ? ORDER BY"
                    + " nombre LIMIT 30 OFFSET ?",
                q,
                offset);
        case "Compras" ->
            Store.query(
                c,
                "SELECT co.id,p.nombre proveedor,co.fecha,co.total FROM compras co JOIN proveedores"
                    + " p ON p.id=co.proveedor_id WHERE p.nombre LIKE ? AND co.fecha>=? AND"
                    + " co.fecha<? ORDER BY co.id DESC LIMIT 30 OFFSET ?",
                q,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                offset);
        case "Ventas" ->
            Store.query(
                c,
                "SELECT v.id,v.numero,l.nombre"
                    + " cliente,v.fecha,v.subtotal,v.descuento,v.impuesto,v.total,v.metodo,v.cambio,v.estado"
                    + " FROM ventas v JOIN clientes l ON l.id=v.cliente_id WHERE (v.numero LIKE ?"
                    + " OR l.nombre LIKE ?) AND (?='TODOS' OR v.estado=?) AND v.fecha>=? AND"
                    + " v.fecha<? ORDER BY v.id DESC LIMIT 30 OFFSET ?",
                q,
                q,
                state,
                state,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                offset);
        case "Caja" ->
            Store.query(
                c,
                "SELECT id,usuario_id,apertura,cierre,fondo,esperado,contado,diferencia FROM cajas"
                    + " WHERE (?=1 OR usuario_id=?) AND apertura>=? AND apertura<? ORDER BY id DESC"
                    + " LIMIT 30 OFFSET ?",
                s.admin() ? 1 : 0,
                s.id(),
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                offset);
        case "Fidelización" ->
            Store.query(
                c,
                "SELECT l.id,l.nombre,COALESCE(SUM(p.valor),0) puntos,CASE WHEN"
                    + " COALESCE(SUM(p.valor),0)>=100 THEN 'ORO' WHEN COALESCE(SUM(p.valor),0)>=30"
                    + " THEN 'PLATA' ELSE 'BRONCE' END nivel FROM clientes l LEFT JOIN puntos p ON"
                    + " p.cliente_id=l.id WHERE l.activo=1 AND l.nombre LIKE ? GROUP BY"
                    + " l.id,l.nombre ORDER BY puntos DESC LIMIT 30 OFFSET ?",
                q,
                offset);
        case "Recordatorios" ->
            Store.query(
                c,
                "SELECT r.id,l.nombre"
                    + " cliente,r.titulo,r.categoria,r.prioridad,r.fecha,r.repetir_dias,r.estado"
                    + " FROM recordatorios r JOIN clientes l ON l.id=r.cliente_id WHERE (r.titulo"
                    + " LIKE ? OR l.nombre LIKE ? OR r.categoria LIKE ?) AND (?='TODOS' OR"
                    + " r.estado=?) AND r.fecha>=? AND r.fecha<? ORDER BY r.fecha LIMIT 30 OFFSET"
                    + " ?",
                q,
                q,
                q,
                state,
                state,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                offset);
        case "Auditoría" ->
            Store.query(
                c,
                "SELECT a.id,u.usuario,a.accion,a.referencia,a.fecha FROM auditoria a JOIN usuarios"
                    + " u ON u.id=a.usuario_id WHERE (a.accion LIKE ? OR u.usuario LIKE ?) AND"
                    + " a.fecha>=? AND a.fecha<? ORDER BY a.id DESC LIMIT 30 OFFSET ?",
                q,
                q,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                offset);
        default -> throw new IllegalArgumentException("Módulo desconocido.");
      };
    }
  }

  public List<Map<String, Object>> report(Session s, String kind, LocalDate from, LocalDate to)
      throws Exception {
    access(s, false);
    BarberiaService.range(from, to);
    try (var c = Database.connection()) {
      return switch (kind) {
        case "Ventas" ->
            Store.query(
                c,
                "SELECT DATE(fecha) dia,metodo,COUNT(*) facturas,SUM(total) total FROM ventas WHERE"
                    + " estado='EMITIDA' AND fecha>=? AND fecha<? GROUP BY DATE(fecha),metodo ORDER"
                    + " BY dia",
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());
        case "Inventario" ->
            Store.query(
                c,
                "SELECT p.codigo,p.nombre,l.codigo"
                    + " lote,l.vencimiento,l.cantidad,l.costo,l.cantidad*l.costo valor FROM"
                    + " productos p JOIN lotes l ON l.producto_id=p.id ORDER BY"
                    + " p.nombre,l.vencimiento");
        case "Caja" ->
            Store.query(
                c,
                "SELECT"
                    + " c.id,u.usuario,c.apertura,c.cierre,c.fondo,c.esperado,c.contado,c.diferencia"
                    + " FROM cajas c JOIN usuarios u ON u.id=c.usuario_id WHERE c.apertura>=? AND"
                    + " c.apertura<? AND (?=1 OR c.usuario_id=?) ORDER BY c.id",
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                s.admin() ? 1 : 0,
                s.id());
        case "Clientes" ->
            Store.query(
                c,
                "SELECT l.nombre,COUNT(p.id) movimientos,COALESCE(SUM(p.valor),0) puntos FROM"
                    + " clientes l LEFT JOIN puntos p ON p.cliente_id=l.id GROUP BY l.id,l.nombre"
                    + " ORDER BY puntos DESC");
        default -> throw new IllegalArgumentException("Reporte no disponible.");
      };
    }
  }

  public List<Map<String, Object>> invoice(Session s, long id) throws Exception {
    access(s, false);
    try (var c = Database.connection()) {
      return Store.query(
          c,
          "SELECT v.numero,v.fecha,c.nombre cliente,p.nombre"
              + " producto,d.cantidad,d.precio,v.descuento,v.impuesto,v.total,v.metodo,v.recibido,v.cambio,v.estado"
              + " FROM ventas v JOIN clientes c ON c.id=v.cliente_id JOIN venta_detalles d ON"
              + " d.venta_id=v.id JOIN lotes l ON l.id=d.lote_id JOIN productos p ON"
              + " p.id=l.producto_id WHERE v.id=?",
          id);
    }
  }
}
