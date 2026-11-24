package edu.unev.barberia.model;

/** Persona que solicita los servicios de la barbería. */
public class Cliente {
  /** Constructor vacío para mapeo de datos. Completar sus propiedades antes de usarlo. */
  public Cliente() {}

  private int id;
  private String nombre, telefono;

  public Cliente(int id, String nombre, String telefono) {
    this.id = id;
    setNombre(nombre);
    setTelefono(telefono);
  }

  public int getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String v) {
    nombre = Usuario.obligatorio(v, "Nombre del cliente");
  }

  public String getTelefono() {
    return telefono;
  }

  public void setTelefono(String v) {
    telefono = Usuario.obligatorio(v, "Teléfono");
  }

  @Override
  public String toString() {
    return nombre;
  }

  public void setId(int valor) {
    id = valor;
  }
}
