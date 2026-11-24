package edu.unev.barberia.dao;

import edu.unev.barberia.db.Database;
import java.sql.*;
import java.util.*;

/** Único lugar con SQL de la aplicación. Los valores siempre se parametrizan. */
public final class Store {
  public static List<Map<String, Object>> query(Connection c, String sql, Object... params)
      throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      bind(ps, params);
      try (var rs = ps.executeQuery()) {
        var rows = new ArrayList<Map<String, Object>>();
        var md = rs.getMetaData();
        while (rs.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= md.getColumnCount(); i++)
            row.put(md.getColumnLabel(i), rs.getObject(i));
          rows.add(row);
        }
        return rows;
      }
    }
  }

  public static int update(Connection c, String sql, Object... params) throws SQLException {
    try (var ps = c.prepareStatement(sql)) {
      bind(ps, params);
      return ps.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... params) throws SQLException {
    try (var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(ps, params);
      ps.executeUpdate();
      try (var r = ps.getGeneratedKeys()) {
        return r.next() ? r.getLong(1) : 0;
      }
    }
  }

  private static void bind(PreparedStatement s, Object[] p) throws SQLException {
    for (int i = 0; i < p.length; i++) s.setObject(i + 1, p[i]);
  }

  public static Map<String, Object> one(Connection c, String sql, Object... p) throws SQLException {
    var rows = query(c, sql, p);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  public static long id(Map<String, Object> row, String k) {
    Object v = row.get(k);
    return v == null ? 0 : ((Number) v).longValue();
  }

  public static String text(Map<String, Object> row, String k) {
    return Objects.toString(row.get(k), "");
  }

  public static boolean yes(Map<String, Object> row, String k) {
    Object v = row.get(k);
    return v instanceof Boolean b ? b : v instanceof Number n && n.intValue() != 0;
  }

  @FunctionalInterface
  public interface Work<T> {
    T run(Connection c) throws Exception;
  }

  public static <T> T tx(Work<T> work) throws Exception {
    try (var c = Database.connection()) {
      c.setAutoCommit(false);
      try {
        T result = work.run(c);
        c.commit();
        return result;
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }
}
