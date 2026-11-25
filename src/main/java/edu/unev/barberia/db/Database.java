package edu.unev.barberia.db;

import com.zaxxer.hikari.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Pool utilitario, configuración externa y credenciales fuera del código. */
public final class Database {
  private static HikariDataSource pool;
  private static Properties config;

  static {
    System.setProperty("com.mysql.cj.disableAbandonedConnectionCleanup", "true");
  }

  private Database() {}

  public static synchronized Properties config() {
    if (config == null) {
      config = new Properties();
      try (var in =
          Files.newInputStream(
              Path.of(System.getProperty("barberia.config", "config.properties")))) {
        config.load(in);
      } catch (IOException e) {
        throw new IllegalStateException(
            "Falta config.properties. Copie la plantilla y configure la conexión.");
      }
    }
    return config;
  }

  public static String option(String key, String def) {
    return System.getenv()
        .getOrDefault(key.toUpperCase().replace('.', '_'), config().getProperty(key, def));
  }

  public static synchronized Connection connection() throws SQLException {
    if (pool == null) {
      String url = option("db.url", "");
      if (!url.startsWith("jdbc:mariadb:") && !url.startsWith("jdbc:mysql:"))
        throw new SQLException("URL JDBC no admitida.");
      boolean local = url.matches("jdbc:(mariadb|mysql)://(127\\.0\\.0\\.1|localhost)(:|/).*");
      if (!local
          && !(url.contains("sslMode=verify-full") || url.contains("sslMode=VERIFY_IDENTITY")))
        throw new SQLException(
            "La base remota requiere TLS con verificación del certificado y del servidor.");
      HikariConfig c = new HikariConfig();
      c.setJdbcUrl(url);
      c.setUsername(option("db.user", ""));
      c.setPassword(option("db.password", ""));
      c.setMaximumPoolSize(4);
      c.setConnectionTimeout(10000);
      c.setValidationTimeout(3000);
      c.setPoolName("BarberiaV3");
      pool = new HikariDataSource(c);
    }
    return pool.getConnection();
  }

  public static synchronized void close() {
    if (pool != null) {
      pool.close();
      pool = null;
    }
  }

  public static synchronized void reload() {
    close();
    config = null;
  }
}
