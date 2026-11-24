package edu.unev.barberia.tools;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import java.time.*;
import org.mindrot.jbcrypt.BCrypt;

/** Datos académicos explícitos. No sobrescribe usuarios existentes ni publica secretos 2FA. */
public final class Seed {
  public static void main(String[] args) throws Exception {
    if (!"yes".equals(System.getenv("BARBERIA_SEED_DEMO")))
      throw new IllegalStateException(
          "Active BARBERIA_SEED_DEMO=yes únicamente en una base de práctica.");
    Store.tx(c -> {
      if (Store.one(c, "SELECT id FROM usuarios LIMIT 1") != null)
        throw new IllegalStateException("La base ya contiene usuarios. No se modificó.");
      String[] names = {"Administrador Demo", "Recepción Demo", "Carlos Barbero"},
          users = {"admin.demo", "recepcion.demo", "barbero.demo"},
          passwords =
              {
                Database.option("demo.admin.password", ""),
                Database.option("demo.recepcion.password", ""),
                Database.option("demo.barbero.password", "")
              };
      long barber = 0;
      for (String password : passwords)
        if (password.length() < 10 || password.length() > 60)
          throw new IllegalArgumentException(
              "Configure las tres claves de demostración en el archivo privado.");
      for (int i = 0; i < 3; i++) {
        long id =
            Store.insert(
                c,
                "INSERT INTO usuarios(nombre,usuario,correo,password_hash,rol_id) VALUES"
                    + " (?,?,?,?,?)",
                names[i],
                users[i],
                users[i] + "@example.com",
                BCrypt.hashpw(passwords[i], BCrypt.gensalt(12)),
                i + 1);
        if (i == 2) {
          barber =
              Store.insert(
                  c,
                  "INSERT INTO empleados(usuario_id,nombre,especialidad) VALUES (?,?,'Corte y"
                      + " barba')",
                  id,
                  names[i]);
          Store.insert(c, "INSERT INTO agendas(empleado_id) VALUES (?)", barber);
          for (int day = 1; day <= 7; day++)
            Store.update(
                c,
                "INSERT INTO horarios_trabajo(empleado_id,dia_semana,hora_inicio,hora_fin) VALUES"
                    + " (?,?,'08:00:00','20:00:00')",
                barber,
                day);
        }
      }
      long category =
          Store.insert(
              c,
              "INSERT INTO categorias_servicio(nombre,descripcion) VALUES ('Cortes','Servicios de"
                  + " cabello')");
      long category2 =
          Store.insert(
              c,
              "INSERT INTO categorias_servicio(nombre,descripcion) VALUES ('Barba','Cuidado de"
                  + " barba')");
      long[] services = {
        Store.insert(
            c,
            "INSERT INTO servicios(nombre,precio,duracion_minutos,categoria_id) VALUES ('Corte"
                + " clásico',350,30,?)",
            category),
        Store.insert(
            c,
            "INSERT INTO servicios(nombre,precio,duracion_minutos,categoria_id) VALUES ('Corte y"
                + " barba',550,45,?)",
            category2)
      };
      long[] clients = new long[8];
      for (int i = 0; i < 8; i++)
        clients[i] =
            Store.insert(
                c,
                "INSERT INTO clientes(nombre,telefono,correo) VALUES (?,?,?)",
                new String[] {
                      "Ana Demo",
                      "Luis Demo",
                      "Pedro Demo",
                      "María Demo",
                      "José Demo",
                      "Clara Demo",
                      "Rafael Demo",
                      "Lucía Demo"
                    }
                    [i],
                "809-555-01" + String.format("%02d", i),
                "cliente" + i + "@example.com");
      long agenda =
          Store.id(Store.one(c, "SELECT id FROM agendas WHERE empleado_id=?", barber), "id");
      for (int i = 0; i < 24; i++) {
        boolean done = i < 18;
        long serv = services[i % 2];
        int price = i % 2 == 0 ? 350 : 550, mins = i % 2 == 0 ? 30 : 45;
        LocalDateTime date = LocalDate.now().minusDays(9 - i / 3).atTime(9 + (i % 3) * 2, 0);
        if (!done) date = LocalDate.now().plusDays(1 + (i - 18) / 3).atTime(9 + (i % 3) * 2, 0);
        long cita =
            Store.insert(
                c,
                "INSERT INTO"
                    + " citas(cliente_id,empleado_id,servicio_id,agenda_id,fecha,precio_acordado,duracion_acordada,estado)"
                    + " VALUES (?,?,?,?,?,?,?,?)",
                clients[i % 8],
                barber,
                serv,
                agenda,
                date,
                price,
                mins,
                done ? "COMPLETADA" : "PENDIENTE");
        if (done)
          Store.insert(
              c,
              "INSERT INTO pagos(cita_id,monto,fecha) VALUES (?,?,?)",
              cita,
              price,
              date.plusMinutes(mins));
      }
      return null;
    });
    System.out.println(
        "Datos de práctica creados. Cada cuenta deberá vincular su propio Authenticator al primer"
            + " ingreso.");
    Database.close();
  }
}
