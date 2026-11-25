package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.*;
import edu.unev.barberia.service.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BARBERIA_DB_TEST", matches = "true")
class IntegrationTest {
  Session admin, barber, recep;
  BarberiaService service = new BarberiaService();
  List<Long> createdUsers = new ArrayList<>();

  @BeforeEach
  void start() throws Exception {
    try (var c = Database.connection()) {
      for (String name : List.of("admin.demo", "recepcion.demo", "barbero.demo")) {
        var u =
            Store.one(
                c,
                "SELECT u.id,u.nombre,u.usuario,r.nombre rol,COALESCE(e.id,0) empleado FROM"
                    + " usuarios u JOIN roles r ON r.id=u.rol_id LEFT JOIN empleados e ON"
                    + " e.usuario_id=u.id WHERE usuario=?",
                name);
        Session s =
            new Session(
                Store.id(u, "id"),
                Store.text(u, "nombre"),
                name,
                Store.text(u, "rol"),
                Store.id(u, "empleado"),
                Instant.now().plusSeconds(300));
        if (s.admin()) admin = s;
        else if (s.barbero()) barber = s;
        else recep = s;
      }
      for (Session cashier : List.of(admin, recep))
        if (Store.one(c, "SELECT id FROM cajas WHERE usuario_id=? AND cierre IS NULL", cashier.id())
            == null) new EnterpriseService().open(cashier, new BigDecimal("0"));
    }
  }

  @AfterEach
  void cleanup() throws Exception {
    try (var c = Database.connection()) {
      for (long id : createdUsers) {
        Store.update(c, "DELETE FROM acceso_log WHERE usuario_id=?", id);
        Store.update(c, "DELETE FROM usuario_2fa WHERE usuario_id=?", id);
        Store.update(c, "DELETE FROM usuarios WHERE id=?", id);
      }
    }
  }

  long account(String name) throws Exception {
    service.user(admin, 0, "Prueba", name, name + "@example.com", "Prueba2026!", 2, true);
    try (var c = Database.connection()) {
      long id = Store.id(Store.one(c, "SELECT id FROM usuarios WHERE usuario=?", name), "id");
      createdUsers.add(id);
      return id;
    }
  }

  @Test
  void dosFactoresBloqueoYBitacora() throws Exception {
    String name = "test" + UUID.randomUUID().toString().substring(0, 8);
    long id = account(name);
    AuthService auth = new AuthService();
    assertNull(auth.begin(name, "incorrecta"));
    var ch = auth.begin(name, "Prueba2026!");
    assertTrue(ch.enroll());
    String valid = Totp.code(ch.secret(), Instant.now().getEpochSecond() / 30);
    String wrong = "000000";
    while (Totp.verify(ch.secret(), wrong, Instant.now().getEpochSecond(), -1) >= 0)
      wrong = String.format("%06d", Integer.parseInt(wrong) + 1);
    assertNull(auth.verify(ch.token(), wrong));
    assertNull(auth.verify(ch.token(), wrong));
    assertNull(auth.verify(ch.token(), wrong));
    assertThrows(SecurityException.class, () -> auth.verify(ch.token(), valid));
    assertNull(auth.begin(name, "Prueba2026!"));
    try (var c = Database.connection()) {
      assertTrue(Store.query(c, "SELECT id FROM acceso_log WHERE usuario_id=?", id).size() >= 5);
      Store.update(c, "UPDATE usuarios SET bloqueado_hasta=NULL WHERE id=?", id);
      Store.update(c, "UPDATE usuario_2fa SET intentos=0 WHERE usuario_id=?", id);
    }
    var next = auth.begin(name, "Prueba2026!");
    var session =
        auth.verify(next.token(), Totp.code(next.secret(), Instant.now().getEpochSecond() / 30));
    assertNotNull(session);
    var again = auth.begin(name, "Prueba2026!");
    assertFalse(again.enroll());
    assertNull(
        auth.verify(again.token(), Totp.code(ch.secret(), Instant.now().getEpochSecond() / 30)));
    auth.abandon(again.token());
    service.reset2fa(admin, id);
    var reset = auth.begin(name, "Prueba2026!");
    assertTrue(reset.enroll());
    assertNotEquals(ch.secret(), reset.secret());
    auth.abandon(reset.token());
  }

  @Test
  void permisosYExportacion() throws Exception {
    assertThrows(SecurityException.class, () -> service.client(barber, 0, "A", "809-555-0123", ""));
    assertThrows(
        SecurityException.class,
        () -> service.service(recep, 0, "A", new BigDecimal("5.00"), 30, 1));
    assertThrows(
        SecurityException.class,
        () -> service.list(barber, "Usuarios", LocalDate.now().minusDays(2), LocalDate.now()));
    LocalDate from = LocalDate.of(2026, 11, 1), to = LocalDate.of(2026, 11, 30);
    var report = service.report(barber, from, to);
    assertFalse(report.isEmpty());
    assertFalse(report.getFirst().containsKey("cobrado"));
    Path path = Files.createTempFile("barberia-report-", ".xlsx");
    try {
      new ReportService().export(barber, from, to, path);
      try (var book = new org.apache.poi.xssf.usermodel.XSSFWorkbook(path.toFile())) {
        assertTrue(
            book.getSheetAt(0)
                .getRow(1)
                .getCell(0)
                .getStringCellValue()
                .contains(barber.usuario()));
        assertEquals(3, book.getSheetAt(0).getRow(4).getLastCellNum());
      }
    } finally {
      Files.deleteIfExists(path);
    }
    assertEquals(
        4,
        service
            .dashboard(admin, LocalDate.now().minusDays(20), LocalDate.now().plusDays(7))
            .values()
            .size());
  }

  @Test
  void crudPersistenteValidacionYConcurrenciaBasica() throws Exception {
    String name = "Cliente " + UUID.randomUUID();
    long id = 0;
    try {
      service.client(recep, 0, name, "809-555-0123", "");
      try (var c = Database.connection()) {
        id = Store.id(Store.one(c, "SELECT id FROM clientes WHERE nombre=?", name), "id");
      }
      assertTrue(id > 0);
      service.client(admin, id, name + " editado", "809-555-0124", "");
      assertTrue(
          service.list(recep, "Clientes", LocalDate.now(), LocalDate.now()).stream()
              .anyMatch(r -> Store.text(r, "nombre").equals(name + " editado")));
      assertThrows(IllegalArgumentException.class, () -> service.client(admin, 0, "", "x", "mal"));
    } finally {
      if (id > 0) service.delete(admin, "Clientes", id);
    }
  }

  @Test
  void cicloCitasYCobros() throws Exception {
    String label = "Prueba " + UUID.randomUUID();
    long client = 0, serviceId = 0, citaId = 0;
    try {
      service.client(admin, 0, label, "809-555-0133", "");
      service.service(admin, 0, label, new BigDecimal("420.00"), 30, 1);
      try (var c = Database.connection()) {
        client = Store.id(Store.one(c, "SELECT id FROM clientes WHERE nombre=?", label), "id");
        serviceId = Store.id(Store.one(c, "SELECT id FROM servicios WHERE nombre=?", label), "id");
      }
      LocalDateTime date = LocalDate.now().plusDays(45).atTime(10, 0);
      service.appointment(recep, 0, client, barber.empleado(), serviceId, date);
      try (var c = Database.connection()) {
        citaId = Store.id(Store.one(c, "SELECT id FROM citas WHERE cliente_id=?", client), "id");
      }
      final long cc = client, ss = serviceId, ci = citaId;
      assertThrows(
          IllegalArgumentException.class,
          () -> service.appointment(admin, 0, cc, barber.empleado(), ss, date.plusMinutes(15)));
      service.appointment(recep, citaId, client, barber.empleado(), serviceId, date.plusHours(1));
      service.status(barber, citaId, true);
      service.pay(recep, citaId);
      assertThrows(IllegalArgumentException.class, () -> service.pay(admin, ci));
      try (var c = Database.connection()) {
        assertNotNull(Store.one(c, "SELECT id FROM pagos WHERE cita_id=?", citaId));
        assertNotNull(Store.one(c, "SELECT id FROM detalles_factura WHERE cita_id=?", citaId));
      }
    } finally {
      try (var c = Database.connection()) {
        if (citaId > 0) {
          var detail =
              Store.one(c, "SELECT factura_id FROM detalles_factura WHERE cita_id=?", citaId);
          Store.update(c, "DELETE FROM detalles_factura WHERE cita_id=?", citaId);
          if (detail != null)
            Store.update(c, "DELETE FROM facturas WHERE id=?", detail.get("factura_id"));
          Store.update(c, "DELETE FROM puntos WHERE cita_id=?", citaId);
          Store.update(c, "DELETE FROM movimientos_caja WHERE cita_id=?", citaId);
          Store.update(c, "DELETE FROM pagos WHERE cita_id=?", citaId);
          Store.update(c, "DELETE FROM citas WHERE id=?", citaId);
        }
        if (serviceId > 0) Store.update(c, "DELETE FROM servicios WHERE id=?", serviceId);
        if (client > 0) Store.update(c, "DELETE FROM clientes WHERE id=?", client);
      }
    }
  }
}
