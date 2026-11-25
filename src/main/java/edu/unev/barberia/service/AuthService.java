package edu.unev.barberia.service;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.mindrot.jbcrypt.BCrypt;

public final class AuthService {
  public record Challenge(String token, String usuario, String secret, boolean enroll) {}

  private record Pending(long id, String secret, Instant expires) {}

  private final Map<String, Pending> pending = new ConcurrentHashMap<>();
  private final Vault vault = new Vault(Database.option("security.key", ""));
  private static final String USER =
      "SELECT u.*,r.nombre rol,COALESCE(e.id,0) empleado FROM usuarios u JOIN roles r ON"
          + " r.id=u.rol_id LEFT JOIN empleados e ON e.usuario_id=u.id WHERE u.id=?";

  public Challenge begin(String username, String password) throws Exception {
    pending.entrySet().removeIf(e -> Instant.now().isAfter(e.getValue().expires));
    if (username == null || username.isBlank() || password == null || password.isBlank())
      throw new IllegalArgumentException("Complete usuario y contraseña.");
    return Store.tx(c -> {
      var u =
          Store.one(
              c,
              "SELECT * FROM usuarios WHERE usuario=? FOR UPDATE",
              username.trim().toLowerCase(Locale.ROOT));
      if (u == null) {
        BCrypt.checkpw(
            password,
            Database.option(
                "security.dummyHash", BCrypt.hashpw("dummy-unused", BCrypt.gensalt(10))));
        audit(c, null, username, "FALLO_CLAVE");
        return null;
      }
      long id = Store.id(u, "id");
      if (!Store.yes(u, "activo") || locked(u)) {
        audit(c, id, username, "BLOQUEADO");
        return null;
      }
      if (!BCrypt.checkpw(password, Store.text(u, "password_hash"))) {
        int fails = ((Number) u.get("fallos")).intValue() + 1;
        Store.update(
            c,
            "UPDATE usuarios SET fallos=?,bloqueado_hasta=? WHERE id=?",
            fails,
            fails >= 5 ? LocalDateTime.now().plusMinutes(5) : null,
            id);
        audit(c, id, username, "FALLO_CLAVE");
        return null;
      }
      Store.update(c, "UPDATE usuarios SET fallos=0 WHERE id=?", id);
      if (u.get("bloqueado_hasta") != null) {
        Store.update(c, "UPDATE usuario_2fa SET intentos=0 WHERE usuario_id=?", id);
        Store.update(c, "UPDATE usuarios SET bloqueado_hasta=NULL WHERE id=?", id);
      }
      var t = Store.one(c, "SELECT * FROM usuario_2fa WHERE usuario_id=?", id);
      boolean enroll = t == null || !Store.yes(t, "habilitado");
      String secret;
      if (t == null) {
        secret = Totp.secret();
        Store.update(
            c,
            "INSERT INTO usuario_2fa(usuario_id,secreto_cifrado) VALUES (?,?)",
            id,
            vault.encrypt(secret));
      } else secret = vault.decrypt(Store.text(t, "secreto_cifrado"));
      String token = UUID.randomUUID().toString();
      pending.put(token, new Pending(id, secret, Instant.now().plusSeconds(300)));
      return new Challenge(token, username, enroll ? secret : null, enroll);
    });
  }

  public Session verify(String token, String code) throws Exception {
    Pending p = pending.get(token);
    if (p == null || Instant.now().isAfter(p.expires)) {
      pending.remove(token);
      throw new SecurityException("Verificación vencida. Vuelva al Login.");
    }
    Session session =
        Store.tx(c -> {
          var u = Store.one(c, USER + " FOR UPDATE", p.id);
          if (u == null || !Store.yes(u, "activo") || locked(u)) return null;
          var t = Store.one(c, "SELECT * FROM usuario_2fa WHERE usuario_id=? FOR UPDATE", p.id);
          if (t == null || !vault.decrypt(Store.text(t, "secreto_cifrado")).equals(p.secret))
            return null;
          long step =
              Totp.verify(
                  p.secret, code, Instant.now().getEpochSecond(), Store.id(t, "ultimo_paso"));
          if (step < 0) {
            long attempts = Store.id(t, "intentos") + 1;
            Store.update(c, "UPDATE usuario_2fa SET intentos=? WHERE usuario_id=?", attempts, p.id);
            if (attempts >= 3) {
              Store.update(
                  c,
                  "UPDATE usuarios SET bloqueado_hasta=? WHERE id=?",
                  LocalDateTime.now().plusMinutes(5),
                  p.id);
              pending.remove(token);
            }
            audit(c, p.id, Store.text(u, "usuario"), "FALLO_2FA");
            return null;
          }
          Store.update(
              c,
              "UPDATE usuario_2fa SET habilitado=1,intentos=0,ultimo_paso=? WHERE usuario_id=?",
              step,
              p.id);
          Store.update(c, "UPDATE usuarios SET fallos=0,bloqueado_hasta=NULL WHERE id=?", p.id);
          audit(c, p.id, Store.text(u, "usuario"), "EXITO");
          return new Session(
              p.id,
              Store.text(u, "nombre"),
              Store.text(u, "usuario"),
              Store.text(u, "rol"),
              Store.id(u, "empleado"),
              Instant.now().plusSeconds(1800));
        });
    if (session != null) pending.remove(token);
    return session;
  }

  private boolean locked(Map<String, Object> u) {
    Object v = u.get("bloqueado_hasta");
    if (v == null) return false;
    LocalDateTime until = v instanceof LocalDateTime time ? time : ((Timestamp) v).toLocalDateTime();
    return until.isAfter(LocalDateTime.now());
  }

  private void audit(Connection c, Long id, String username, String result) throws SQLException {
    Store.update(
        c,
        "INSERT INTO acceso_log(usuario_id,usuario,resultado) VALUES (?,?,?)",
        id,
        username,
        result);
  }

  public void abandon(String token) {
    pending.remove(token);
  }
}
