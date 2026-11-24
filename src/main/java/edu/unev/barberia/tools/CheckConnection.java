package edu.unev.barberia.tools;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;

public final class CheckConnection {
  public static void main(String[] args) throws Exception {
    try (var c = Database.connection()) {
      var r = Store.one(c, "SELECT DATABASE() base,VERSION() version");
      System.out.println("Conexión real verificada: " + r);
      System.out.println("Tablas: " + Store.query(c, "SHOW TABLES").size());
    } finally {
      Database.close();
    }
  }
}
