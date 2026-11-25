package edu.unev.barberia.service;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.Session;
import java.math.BigDecimal;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;

/** Autorización en servidor lógico: la vista nunca sustituye estas comprobaciones. */
public final class BarberiaService {
  public record Option(long id, String nombre) {
    public String toString() {
      return nombre;
    }
  }

  public record Dashboard(
      List<String> labels,
      List<Number> values,
      Map<String, Number> servicios,
      Map<String, Number> dias,
      List<Map<String, Object>> recientes) {}

  private void check(Session s, String... roles) throws Exception {
    s.require(roles);
    try (var c = Database.connection()) {
      var u =
          Store.one(
              c,
              "SELECT u.activo,r.nombre rol FROM usuarios u JOIN roles r ON r.id=u.rol_id WHERE"
                  + " u.id=?",
              s.id());
      if (u == null || !Store.yes(u, "activo") || !Store.text(u, "rol").equals(s.rol()))
        throw new SecurityException("La cuenta cambió. Inicie sesión de nuevo.");
    }
  }

  private boolean all(Session s) {
    return !s.barbero();
  }

  private String scope(Session s) {
    return all(s) ? "" : " AND c.empleado_id=?";
  }

  private Object[] scoped(Session s, Object... p) {
    if (all(s)) return p;
    Object[] a = Arrays.copyOf(p, p.length + 1);
    a[p.length] = s.empleado();
    return a;
  }

  public List<Map<String, Object>> list(Session s, String module, LocalDate from, LocalDate to)
      throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    range(from, to);
    try (var c = Database.connection()) {
      return switch (module) {
        case "Clientes" -> {
          if (s.barbero())
            throw new SecurityException("No puede consultar el directorio de clientes.");
          yield Store.query(
              c, "SELECT id,nombre,telefono,correo FROM clientes WHERE activo=1 ORDER BY nombre");
        }
        case "Servicios" ->
            Store.query(
                c,
                "SELECT s.id,s.nombre,s.precio,s.duracion_minutos duracion,s.categoria_id,g.nombre"
                    + " categoria FROM servicios s JOIN categorias_servicio g ON"
                    + " g.id=s.categoria_id WHERE s.activo=1 ORDER BY s.nombre");
        case "Citas" ->
            Store.query(
                c,
                "SELECT c.id,c.cliente_id,c.empleado_id,c.servicio_id,l.nombre cliente,e.nombre"
                    + " barbero,v.nombre servicio,c.fecha,c.estado,c.precio_acordado precio FROM"
                    + " citas c JOIN clientes l ON l.id=c.cliente_id JOIN empleados e ON"
                    + " e.id=c.empleado_id JOIN servicios v ON v.id=c.servicio_id WHERE c.fecha>=?"
                    + " AND c.fecha<?"
                    + scope(s)
                    + " ORDER BY c.fecha DESC",
                scoped(s, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
        case "Usuarios" -> {
          s.require("ADMINISTRADOR");
          yield Store.query(
              c,
              "SELECT u.id,u.nombre,u.usuario,u.correo,u.rol_id,r.nombre"
                  + " rol,u.activo,COALESCE(t.habilitado,0) doble_factor FROM usuarios u JOIN roles"
                  + " r ON r.id=u.rol_id LEFT JOIN usuario_2fa t ON t.usuario_id=u.id ORDER BY"
                  + " u.nombre");
        }
        case "Accesos" -> {
          s.require("ADMINISTRADOR");
          yield Store.query(
              c,
              "SELECT id,usuario,fecha,resultado FROM acceso_log WHERE fecha>=? AND fecha<? ORDER"
                  + " BY fecha DESC LIMIT 200",
              from.atStartOfDay(),
              to.plusDays(1).atStartOfDay());
        }
        default -> throw new IllegalArgumentException("Módulo desconocido.");
      };
    }
  }

  public List<Map<String, Object>> page(
      Session s,
      String module,
      String query,
      String state,
      LocalDate from,
      LocalDate to,
      int offset)
      throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    range(from, to);
    if (offset < 0) throw new IllegalArgumentException("Página inválida.");
    String q = "%" + Objects.toString(query, "").trim() + "%";
    try (var c = Database.connection()) {
      return switch (module) {
        case "Clientes" -> {
          s.require("ADMINISTRADOR", "RECEPCIONISTA");
          yield Store.query(
              c,
              "SELECT id,nombre,telefono,correo FROM clientes WHERE activo=1 AND (nombre LIKE ? OR"
                  + " telefono LIKE ? OR correo LIKE ?) ORDER BY nombre LIMIT 30 OFFSET ?",
              q,
              q,
              q,
              offset);
        }
        case "Servicios" ->
            Store.query(
                c,
                "SELECT s.id,s.nombre,s.precio,s.duracion_minutos duracion,s.categoria_id,g.nombre"
                    + " categoria FROM servicios s JOIN categorias_servicio g ON"
                    + " g.id=s.categoria_id WHERE s.activo=1 AND (s.nombre LIKE ? OR g.nombre LIKE"
                    + " ?) ORDER BY s.nombre LIMIT 30 OFFSET ?",
                q,
                q,
                offset);
        case "Citas" ->
            Store.query(
                c,
                "SELECT c.id,c.cliente_id,c.empleado_id,c.servicio_id,l.nombre cliente,e.nombre"
                    + " barbero,v.nombre servicio,c.fecha,c.estado,c.precio_acordado precio FROM"
                    + " citas c JOIN clientes l ON l.id=c.cliente_id JOIN empleados e ON"
                    + " e.id=c.empleado_id JOIN servicios v ON v.id=c.servicio_id WHERE c.fecha>=?"
                    + " AND c.fecha<? AND (l.nombre LIKE ? OR e.nombre LIKE ? OR v.nombre LIKE ?)"
                    + " AND (?='TODOS' OR c.estado=?) AND (?=1 OR c.empleado_id=?) ORDER BY c.fecha"
                    + " DESC LIMIT 30 OFFSET ?",
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay(),
                q,
                q,
                q,
                state,
                state,
                s.barbero() ? 0 : 1,
                s.empleado(),
                offset);
        case "Usuarios" -> {
          s.require("ADMINISTRADOR");
          yield Store.query(
              c,
              "SELECT u.id,u.nombre,u.usuario,u.correo,u.rol_id,r.nombre"
                  + " rol,u.activo,COALESCE(t.habilitado,0) doble_factor FROM usuarios u JOIN roles"
                  + " r ON r.id=u.rol_id LEFT JOIN usuario_2fa t ON t.usuario_id=u.id WHERE"
                  + " u.nombre LIKE ? OR u.usuario LIKE ? ORDER BY u.nombre LIMIT 30 OFFSET ?",
              q,
              q,
              offset);
        }
        case "Accesos" -> {
          s.require("ADMINISTRADOR");
          yield Store.query(
              c,
              "SELECT id,usuario,fecha,resultado FROM acceso_log WHERE fecha>=? AND fecha<? AND"
                  + " (usuario LIKE ? OR resultado LIKE ?) ORDER BY id DESC LIMIT 30 OFFSET ?",
              from.atStartOfDay(),
              to.plusDays(1).atStartOfDay(),
              q,
              q,
              offset);
        }
        default -> throw new IllegalArgumentException("Módulo desconocido.");
      };
    }
  }

  public List<Option> options(Session s, String module) throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    try (var c = Database.connection()) {
      List<Map<String, Object>> rows =
          switch (module) {
            case "categorias" ->
                Store.query(c, "SELECT id,nombre FROM categorias_servicio ORDER BY nombre");
            case "clientes" -> {
              if (s.barbero()) throw new SecurityException("Operación no permitida.");
              yield Store.query(c, "SELECT id,nombre FROM clientes WHERE activo=1 ORDER BY nombre");
            }
            case "empleados" ->
                s.barbero()
                    ? Store.query(c, "SELECT id,nombre FROM empleados WHERE id=?", s.empleado())
                    : Store.query(c, "SELECT id,nombre FROM empleados ORDER BY nombre");
            case "servicios" ->
                Store.query(c, "SELECT id,nombre FROM servicios WHERE activo=1 ORDER BY nombre");
            case "roles" -> {
              s.require("ADMINISTRADOR");
              yield Store.query(c, "SELECT id,nombre FROM roles ORDER BY id");
            }
            default -> throw new IllegalArgumentException("Catálogo desconocido.");
          };
      return rows.stream()
          .map(r -> new Option(Store.id(r, "id"), Store.text(r, "nombre")))
          .toList();
    }
  }

  public void client(Session s, long id, String name, String phone, String email) throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA");
    name = required(name, "Nombre");
    phone = required(phone, "Teléfono");
    if (!phone.matches("[+0-9 ()-]{7,30}"))
      throw new IllegalArgumentException("Teléfono inválido.");
    email = email.trim();
    if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Correo inválido.");
    final String validatedName = name, validatedEmail = email, validatedPhone = phone;
    Store.tx(c -> {
      if (id == 0)
        Store.insert(
            c,
            "INSERT INTO clientes(nombre,telefono,correo) VALUES (?,?,?)",
            validatedName,
            validatedPhone,
            validatedEmail);
      else
        Store.update(
            c,
            "UPDATE clientes SET nombre=?,telefono=?,correo=? WHERE id=?",
            validatedName,
            validatedPhone,
            validatedEmail,
            id);
      EnterpriseService.audit(c, s, "CLIENTE_GUARDADO", id == 0 ? validatedName : id);
      return null;
    });
  }

  public void service(Session s, long id, String name, BigDecimal price, int minutes, long category)
      throws Exception {
    check(s, "ADMINISTRADOR");
    name = required(name, "Nombre");
    if (price == null || price.signum() <= 0 || price.scale() > 2 || minutes < 5 || minutes > 480)
      throw new IllegalArgumentException(
          "Precio positivo con dos decimales y duración entre 5 y 480 minutos.");
    final String validatedName = name;
    Store.tx(c -> {
      if (id == 0)
        Store.insert(
            c,
            "INSERT INTO servicios(nombre,precio,duracion_minutos,categoria_id) VALUES (?,?,?,?)",
            validatedName,
            price,
            minutes,
            category);
      else
        Store.update(
            c,
            "UPDATE servicios SET nombre=?,precio=?,duracion_minutos=?,categoria_id=? WHERE id=?",
            validatedName,
            price,
            minutes,
            category,
            id);
      EnterpriseService.audit(c, s, "SERVICIO_GUARDADO", id == 0 ? validatedName : id);
      return null;
    });
  }

  public void delete(Session s, String module, long id) throws Exception {
    if (module.equals("Clientes")) check(s, "ADMINISTRADOR", "RECEPCIONISTA");
    else check(s, "ADMINISTRADOR");
    String sql =
        switch (module) {
          case "Clientes" -> "UPDATE clientes SET activo=0 WHERE id=?";
          case "Servicios" -> "UPDATE servicios SET activo=0 WHERE id=?";
          default ->
              throw new IllegalArgumentException(
                  "No se permite eliminar este registro; cancele la cita o desactive la cuenta.");
        };
    Store.tx(c -> {
      Store.update(c, sql, id);
      EnterpriseService.audit(c, s, "DESACTIVAR_" + module, id);
      return null;
    });
  }

  public void appointment(
      Session s, long id, long client, long employee, long service, LocalDateTime date)
      throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA");
    if (date == null || !date.isAfter(LocalDateTime.now()))
      throw new IllegalArgumentException("Use una fecha y hora futuras.");
    Store.tx(c -> {
      var emp = Store.one(c, "SELECT id FROM empleados WHERE id=? FOR UPDATE", employee);
      if (emp == null) throw new IllegalArgumentException("Barbero inexistente.");
      if (Store.one(c, "SELECT id FROM clientes WHERE id=? AND activo=1 FOR UPDATE", client)
          == null) throw new IllegalArgumentException("Cliente inactivo o inexistente.");
      var serv = Store.one(c, "SELECT * FROM servicios WHERE id=? AND activo=1", service);
      if (serv == null) throw new IllegalArgumentException("Servicio inexistente.");
      long mins = Store.id(serv, "duracion_minutos");
      if (id != 0) {
        var prev = Store.one(c, "SELECT estado FROM citas WHERE id=? FOR UPDATE", id);
        if (prev == null || !Store.text(prev, "estado").equals("PENDIENTE"))
          throw new IllegalArgumentException("Solo se editan citas pendientes.");
      }
      LocalDateTime end = date.plusMinutes(mins);
      if (!date.toLocalDate().equals(end.toLocalDate()))
        throw new IllegalArgumentException("La cita debe terminar el mismo día.");
      var hours =
          Store.one(
              c,
              "SELECT id FROM horarios_trabajo WHERE empleado_id=? AND dia_semana=? AND"
                  + " hora_inicio<=? AND hora_fin>=?",
              employee,
              date.getDayOfWeek().getValue(),
              date.toLocalTime(),
              end.toLocalTime());
      if (hours == null)
        throw new IllegalArgumentException("Fuera del horario del barbero (08:00 a 20:00).");
      if (Store.one(
              c,
              "SELECT id FROM citas WHERE id<>? AND estado<>'CANCELADA' AND (empleado_id=? OR"
                  + " cliente_id=?) AND fecha<? AND TIMESTAMPADD(MINUTE,duracion_acordada,fecha)>?"
                  + " LIMIT 1",
              id,
              employee,
              client,
              end,
              date)
          != null)
        throw new IllegalArgumentException("Cliente o barbero ocupado en ese intervalo.");
      long agenda =
          Store.id(Store.one(c, "SELECT id FROM agendas WHERE empleado_id=?", employee), "id");
      if (id == 0)
        Store.insert(
            c,
            "INSERT INTO"
                + " citas(cliente_id,empleado_id,servicio_id,agenda_id,fecha,precio_acordado,duracion_acordada)"
                + " VALUES (?,?,?,?,?,?,?)",
            client,
            employee,
            service,
            agenda,
            date,
            serv.get("precio"),
            mins);
      else
        Store.update(
            c,
            "UPDATE citas SET"
                + " cliente_id=?,empleado_id=?,servicio_id=?,agenda_id=?,fecha=?,precio_acordado=?,duracion_acordada=?"
                + " WHERE id=?",
            client,
            employee,
            service,
            agenda,
            date,
            serv.get("precio"),
            mins,
            id);
      return null;
    });
  }

  public void status(Session s, long id, boolean complete) throws Exception {
    if (complete) check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    else check(s, "ADMINISTRADOR", "RECEPCIONISTA");
    Store.tx(c -> {
      var row = Store.one(c, "SELECT * FROM citas WHERE id=? FOR UPDATE", id);
      if (row == null || !Store.text(row, "estado").equals("PENDIENTE"))
        throw new IllegalArgumentException("La cita ya no está pendiente.");
      if (s.barbero() && Store.id(row, "empleado_id") != s.empleado())
        throw new SecurityException("Solo puede completar sus propias citas.");
      Store.update(
          c, "UPDATE citas SET estado=? WHERE id=?", complete ? "COMPLETADA" : "CANCELADA", id);
      EnterpriseService.audit(c, s, complete ? "CITA_COMPLETADA" : "CITA_CANCELADA", id);
      return null;
    });
  }

  public void pay(Session s, long id) throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA");
    Store.tx(c -> {
      var box =
          Store.one(
              c, "SELECT id FROM cajas WHERE usuario_id=? AND cierre IS NULL FOR UPDATE", s.id());
      if (box == null) throw new IllegalArgumentException("Abra su caja antes de cobrar la cita.");
      var row = Store.one(c, "SELECT * FROM citas WHERE id=? FOR UPDATE", id);
      if (row == null || !Store.text(row, "estado").equals("COMPLETADA"))
        throw new IllegalArgumentException("Complete la cita antes de cobrar.");
      if (Store.one(c, "SELECT id FROM pagos WHERE cita_id=?", id) != null)
        throw new IllegalArgumentException("La cita ya fue pagada.");
      Store.insert(
          c,
          "INSERT INTO pagos(cita_id,monto,fecha) VALUES (?,?,CURRENT_TIMESTAMP)",
          id,
          row.get("precio_acordado"));
      long fact =
          Store.insert(
              c,
              "INSERT INTO facturas(numero,cliente_id,fecha_emision,estado) VALUES"
                  + " (?,?,CURRENT_TIMESTAMP,'EMITIDA')",
              "F-" + UUID.randomUUID().toString().substring(0, 12),
              row.get("cliente_id"));
      String name =
          Store.text(
              Store.one(c, "SELECT nombre FROM servicios WHERE id=?", row.get("servicio_id")),
              "nombre");
      Store.insert(
          c,
          "INSERT INTO detalles_factura(factura_id,cita_id,descripcion_servicio,importe) VALUES"
              + " (?,?,?,?)",
          fact,
          id,
          name,
          row.get("precio_acordado"));
      Store.insert(
          c,
          "INSERT INTO movimientos_caja(caja_id,usuario_id,cita_id,importe,concepto,metodo) VALUES"
              + " (?,?,?,?,?,?)",
          box.get("id"),
          s.id(),
          id,
          row.get("precio_acordado"),
          "COBRO_CITA",
          "EFECTIVO");
      Store.insert(
          c,
          "INSERT INTO puntos(cliente_id,cita_id,valor,concepto) VALUES (?,?,?,?)",
          row.get("cliente_id"),
          id,
          new BigDecimal(row.get("precio_acordado").toString())
              .divide(new BigDecimal("100"), 0, java.math.RoundingMode.DOWN)
              .intValue(),
          "SERVICIO");
      EnterpriseService.audit(c, s, "COBRO_CITA", id);
      return null;
    });
  }

  public void user(
      Session s,
      long id,
      String name,
      String username,
      String email,
      String password,
      long role,
      boolean active)
      throws Exception {
    check(s, "ADMINISTRADOR");
    final String n = required(name, "Nombre"),
        u = required(username, "Usuario").toLowerCase(Locale.ROOT),
        mail = required(email, "Correo");
    if (!u.matches("[a-z0-9_.-]{3,30}") || !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      throw new IllegalArgumentException("Usuario o correo inválido.");
    if ((id == 0 || !password.isEmpty()) && (password.length() < 10 || password.length() > 60))
      throw new IllegalArgumentException("Contraseña de 10 a 60 caracteres.");
    Store.tx(c -> {
      var r = Store.one(c, "SELECT nombre FROM roles WHERE id=?", role);
      if (r == null) throw new IllegalArgumentException("Rol inválido.");
      if (id == s.id() && (!active || !Store.text(r, "nombre").equals("ADMINISTRADOR")))
        throw new IllegalArgumentException("No puede quitarse su propio acceso de administrador.");
      long uid = id;
      if (id == 0)
        uid =
            Store.insert(
                c,
                "INSERT INTO usuarios(nombre,usuario,correo,password_hash,rol_id,activo) VALUES"
                    + " (?,?,?,?,?,?)",
                n,
                u,
                mail,
                BCrypt.hashpw(password, BCrypt.gensalt(12)),
                role,
                active);
      else {
        Store.update(
            c,
            "UPDATE usuarios SET nombre=?,usuario=?,correo=?,rol_id=?,activo=? WHERE id=?",
            n,
            u,
            mail,
            role,
            active,
            id);
        if (!password.isEmpty())
          Store.update(
              c,
              "UPDATE usuarios SET password_hash=?,fallos=0,bloqueado_hasta=NULL WHERE id=?",
              BCrypt.hashpw(password, BCrypt.gensalt(12)),
              id);
      }
      EnterpriseService.audit(c, s, "USUARIO_GUARDADO", uid);
      if (Store.text(r, "nombre").equals("BARBERO")
          && Store.one(c, "SELECT id FROM empleados WHERE usuario_id=?", uid) == null) {
        long e =
            Store.insert(
                c,
                "INSERT INTO empleados(usuario_id,nombre,especialidad) VALUES (?,?,'Corte y"
                    + " barba')",
                uid,
                n);
        Store.insert(c, "INSERT INTO agendas(empleado_id) VALUES (?)", e);
        for (int day = 1; day <= 7; day++)
          Store.update(
              c,
              "INSERT INTO horarios_trabajo(empleado_id,dia_semana,hora_inicio,hora_fin) VALUES"
                  + " (?,?,'08:00:00','20:00:00')",
              e,
              day);
      }
      return null;
    });
  }

  public void reset2fa(Session s, long id) throws Exception {
    check(s, "ADMINISTRADOR");
    Store.tx(c -> {
      Store.update(c, "DELETE FROM usuario_2fa WHERE usuario_id=?", id);
      Store.update(c, "UPDATE usuarios SET fallos=0,bloqueado_hasta=NULL WHERE id=?", id);
      Store.update(
          c,
          "INSERT INTO acceso_log(usuario_id,usuario,resultado) SELECT id,usuario,'REINICIO_2FA'"
              + " FROM usuarios WHERE id=?",
          id);
      return null;
    });
  }

  public Dashboard dashboard(Session s, LocalDate from, LocalDate to) throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    range(from, to);
    try (var c = Database.connection()) {
      Object[] args = scoped(s, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
      String where = " WHERE c.fecha>=? AND c.fecha<?" + scope(s);
      var row =
          Store.one(
              c,
              "SELECT COUNT(*) total,COALESCE(SUM(c.estado='PENDIENTE'),0)"
                  + " pendientes,COALESCE(SUM(c.estado='COMPLETADA'),0) completadas,COUNT(DISTINCT"
                  + " c.cliente_id) clientes FROM citas c"
                  + where,
              args);
      var money =
          Store.one(
              c,
              "SELECT COALESCE(SUM(p.monto),0) valor FROM pagos p JOIN citas c ON c.id=p.cita_id"
                  + " WHERE p.fecha>=? AND p.fecha<?"
                  + scope(s),
              args);
      var a = new LinkedHashMap<String, Number>();
      for (var v :
          Store.query(
              c,
              "SELECT v.nombre etiqueta,COUNT(*) valor FROM citas c JOIN servicios v ON"
                  + " v.id=c.servicio_id"
                  + where
                  + " GROUP BY v.nombre ORDER BY valor DESC LIMIT 8",
              args)) a.put(Store.text(v, "etiqueta"), (Number) v.get("valor"));
      var b = new LinkedHashMap<String, Number>();
      for (var v :
          Store.query(
              c,
              "SELECT DATE(c.fecha) etiqueta,COUNT(*) valor FROM citas c"
                  + where
                  + " GROUP BY DATE(c.fecha) ORDER BY etiqueta",
              args)) b.put(Store.text(v, "etiqueta"), (Number) v.get("valor"));
      List<String> labels =
          s.barbero()
              ? List.of("Mis citas", "Pendientes", "Completadas", "Mis clientes")
              : List.of("Citas del período", "Pendientes", "Clientes atendidos", "Cobrado RD$");
      List<Number> values =
          s.barbero()
              ? List.of(
                  (Number) row.get("total"),
                  (Number) row.get("pendientes"),
                  (Number) row.get("completadas"),
                  (Number) row.get("clientes"))
              : List.of(
                  (Number) row.get("total"),
                  (Number) row.get("pendientes"),
                  (Number) row.get("clientes"),
                  (Number) money.get("valor"));
      return new Dashboard(
          labels,
          values,
          a,
          b,
          Store.query(
              c,
              "SELECT c.id,l.nombre cliente,v.nombre servicio,c.fecha,c.estado FROM citas c JOIN"
                  + " clientes l ON l.id=c.cliente_id JOIN servicios v ON v.id=c.servicio_id"
                  + where
                  + " ORDER BY c.fecha DESC LIMIT 8",
              args));
    }
  }

  public List<Map<String, Object>> report(Session s, LocalDate from, LocalDate to)
      throws Exception {
    check(s, "ADMINISTRADOR", "RECEPCIONISTA", "BARBERO");
    range(from, to);
    try (var c = Database.connection()) {
      String columns =
          s.barbero()
              ? "c.fecha,v.nombre servicio,c.estado"
              : "c.fecha,l.nombre cliente,e.nombre barbero,v.nombre servicio,c.estado,p.monto"
                  + " cobrado";
      return Store.query(
          c,
          "SELECT "
              + columns
              + " FROM citas c JOIN clientes l ON l.id=c.cliente_id JOIN empleados e ON"
              + " e.id=c.empleado_id JOIN servicios v ON v.id=c.servicio_id LEFT JOIN pagos p ON"
              + " p.cita_id=c.id WHERE c.fecha>=? AND c.fecha<?"
              + scope(s)
              + " ORDER BY c.fecha",
          scoped(s, from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }
  }

  public static void range(LocalDate from, LocalDate to) {
    if (from == null
        || to == null
        || to.isBefore(from)
        || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 366)
      throw new IllegalArgumentException("Rango de fechas inválido (máximo un año).");
  }

  public static String required(String s, String name) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException(name + " es obligatorio.");
    if (s.trim().length() > 150) throw new IllegalArgumentException(name + " demasiado largo.");
    return s.trim();
  }
}
