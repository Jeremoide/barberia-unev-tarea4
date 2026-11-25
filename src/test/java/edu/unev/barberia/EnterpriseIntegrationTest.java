package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.Session;
import edu.unev.barberia.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BARBERIA_DB_TEST", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EnterpriseIntegrationTest {
  static EnterpriseService e = new EnterpriseService();
  static Session admin, barber;
  static long supplier, product, client, sale;
  static String code = "T" + UUID.randomUUID().toString().substring(0, 8);

  static BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  static long number(String sql, Object... args) throws Exception {
    try (var c = Database.connection()) {
      return Store.id(Store.one(c, sql, args), "n");
    }
  }

  @BeforeAll
  static void start() throws Exception {
    try (var c = Database.connection()) {
      var u = Store.one(c, "SELECT * FROM usuarios WHERE usuario='admin.demo'");
      admin =
          new Session(
              Store.id(u, "id"),
              "Prueba",
              "admin.demo",
              "ADMINISTRADOR",
              0,
              Instant.now().plusSeconds(3600));
      u = Store.one(c, "SELECT * FROM usuarios WHERE usuario='barbero.demo'");
      barber =
          new Session(
              Store.id(u, "id"),
              "Prueba",
              "barbero.demo",
              "BARBERO",
              1,
              Instant.now().plusSeconds(3600));
      client =
          Store.insert(
              c,
              "INSERT INTO clientes(nombre,telefono) VALUES (?,?)",
              "Cliente " + code,
              "8095550100");
    }
  }

  @Test
  @Order(1)
  void compraActualizaExistencias() throws Exception {
    e.supplier(admin, "Proveedor " + code, "8095550111");
    supplier = number("SELECT id n FROM proveedores WHERE nombre=?", "Proveedor " + code);
    e.product(admin, code, "Cera " + code, "Estilismo", n("200"), 3);
    product = number("SELECT id n FROM productos WHERE codigo=?", code);
    e.purchase(admin, supplier, product, "L1", LocalDate.now().plusDays(60), 20, n("80"));
    assertEquals(20, number("SELECT SUM(cantidad) n FROM lotes WHERE producto_id=?", product));
  }

  @Test
  @Order(2)
  void barberoSinPermiso() {
    assertThrows(SecurityException.class, () -> e.open(barber, n("0")));
    assertThrows(
        SecurityException.class, () -> e.product(barber, "NO", "Prueba", "Prueba", n("10"), 1));
  }

  @Test
  @Order(3)
  void cajaYVentaTransaccional() throws Exception {
    if (number("SELECT COUNT(*) n FROM cajas WHERE usuario_id=? AND cierre IS NULL", admin.id())
        > 0) e.close(admin, n("0"));
    e.open(admin, n("500"));
    sale =
        e.sell(
            admin,
            client,
            List.of(new EnterpriseService.Line(product, 2)),
            n("0"),
            n("0"),
            "EFECTIVO",
            n("500"),
            false);
    assertEquals(18, number("SELECT SUM(cantidad) n FROM lotes WHERE producto_id=?", product));
    assertEquals(4, number("SELECT SUM(valor) n FROM puntos WHERE cliente_id=?", client));
    assertEquals(1, number("SELECT COUNT(*) n FROM movimientos_caja WHERE venta_id=?", sale));
  }

  @Test
  @Order(4)
  void rollbackSinStock() throws Exception {
    long before = number("SELECT COUNT(*) n FROM ventas");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.sell(
                admin,
                client,
                List.of(new EnterpriseService.Line(product, 50)),
                n("0"),
                n("0"),
                "EFECTIVO",
                n("10000"),
                false));
    assertEquals(before, number("SELECT COUNT(*) n FROM ventas"));
    assertEquals(18, number("SELECT SUM(cantidad) n FROM lotes WHERE producto_id=?", product));
  }

  @Test
  @Order(5)
  void pagoInsuficienteNoDescuentaStock() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.sell(
                admin,
                client,
                List.of(new EnterpriseService.Line(product, 1)),
                n("0"),
                n("0"),
                "EFECTIVO",
                n("1"),
                false));
  }

  @Test
  @Order(6)
  void canjeSinPuntosRechazado() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.sell(
                admin,
                client,
                List.of(new EnterpriseService.Line(product, 1)),
                n("0"),
                n("0"),
                "EFECTIVO",
                n("500"),
                true));
  }

  @Test
  @Order(7)
  void anulacionRestauraSinBorrarHistoria() throws Exception {
    e.cancelSale(admin, sale, "Ensayo de devolución completa");
    assertEquals(20, number("SELECT SUM(cantidad) n FROM lotes WHERE producto_id=?", product));
    assertEquals(0, number("SELECT SUM(valor) n FROM puntos WHERE cliente_id=?", client));
    assertThrows(IllegalArgumentException.class, () -> e.cancelSale(admin, sale, "Repetida"));
    assertEquals(1, number("SELECT COUNT(*) n FROM ventas WHERE id=? AND estado='ANULADA'", sale));
  }

  @Test
  @Order(8)
  void lotesVencidosNoSeVenden() throws Exception {
    try (var c = Database.connection()) {
      Store.update(
          c,
          "UPDATE lotes SET vencimiento=? WHERE producto_id=?",
          LocalDate.now().minusDays(1),
          product);
    }
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.sell(
                admin,
                client,
                List.of(new EnterpriseService.Line(product, 1)),
                n("0"),
                n("0"),
                "EFECTIVO",
                n("500"),
                false));
    e.purchase(admin, supplier, product, "L2", LocalDate.now().plusDays(5), 2, n("90"));
    e.purchase(admin, supplier, product, "L3", LocalDate.now().plusDays(10), 3, n("90"));
    e.sell(
        admin,
        client,
        List.of(new EnterpriseService.Line(product, 1)),
        n("0"),
        n("0"),
        "TARJETA",
        n("0"),
        false);
    assertEquals(
        1, number("SELECT cantidad n FROM lotes WHERE producto_id=? AND codigo='L2'", product));
    assertEquals(
        3, number("SELECT cantidad n FROM lotes WHERE producto_id=? AND codigo='L3'", product));
  }

  @Test
  @Order(9)
  void cajaTarjetaNoAumentaEfectivo() throws Exception {
    assertEquals(n("0.00"), e.close(admin, n("500")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            e.sell(
                admin,
                client,
                List.of(new EnterpriseService.Line(product, 1)),
                n("0"),
                n("0"),
                "EFECTIVO",
                n("500"),
                false));
  }

  @Test
  @Order(10)
  void recordatorioRecurrenteSinDuplicados() throws Exception {
    e.reminder(
        admin, client, "Seguimiento " + code, "Visita", "ALTA", LocalDateTime.now().plusDays(1), 7);
    long id = number("SELECT MAX(id) n FROM recordatorios WHERE cliente_id=?", client);
    e.editReminder(
        admin, id, "Seguimiento editado " + code, LocalDateTime.now().plusDays(2), "MEDIA", 7);
    e.resolveReminder(admin, id, false);
    assertEquals(2, number("SELECT COUNT(*) n FROM recordatorios WHERE cliente_id=?", client));
    assertThrows(IllegalArgumentException.class, () -> e.resolveReminder(admin, id, false));
    assertEquals(2, number("SELECT COUNT(*) n FROM recordatorios WHERE cliente_id=?", client));
  }

  @Test
  @Order(11)
  void reportesYAuditoriaPersistentes() throws Exception {
    for (String k : List.of("Ventas", "Inventario", "Caja", "Clientes")) {
      var rows = e.report(admin, k, LocalDate.now().minusDays(1), LocalDate.now());
      assertNotNull(rows);
      java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/evidencias"));
      PdfReport.write(java.nio.file.Path.of("target/evidencias/reporte_" + k + ".pdf"), k, rows);
    }
    assertTrue(number("SELECT COUNT(*) n FROM auditoria WHERE referencia=?", sale) > 0);
  }

  @Test
  @Order(12)
  void desactivacionConservaHistorial() throws Exception {
    new BarberiaService().delete(admin, "Clientes", client);
    assertEquals(1, number("SELECT COUNT(*) n FROM clientes WHERE id=? AND activo=0", client));
    assertEquals(2, number("SELECT COUNT(*) n FROM ventas WHERE cliente_id=?", client));
  }
}
