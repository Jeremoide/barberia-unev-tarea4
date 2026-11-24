package edu.unev.barberia.security;

import java.time.*;

public record Session(
    long id, String nombre, String usuario, String rol, long empleado, Instant expires) {
  public boolean admin() {
    return rol.equals("ADMINISTRADOR");
  }

  public boolean barbero() {
    return rol.equals("BARBERO");
  }

  public void require(String... roles) {
    if (Instant.now().isAfter(expires))
      throw new SecurityException("La sesión expiró. Inicie sesión de nuevo.");
    for (String r : roles) if (rol.equals(r)) return;
    throw new SecurityException("Su rol no tiene permiso para esta operación.");
  }
}
