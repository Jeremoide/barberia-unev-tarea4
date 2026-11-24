package edu.unev.barberia.model;

import java.util.Objects;

/** Empleado relacionado con su cuenta de acceso. */
public class Empleado {
  /** Constructor vacío para mapeo de datos. Completar sus propiedades antes de usarlo. */
  public Empleado() {}

  private int id;
  private Usuario usuario;
  private String especialidad;

  public Empleado(int id, Usuario usuario, String especialidad) {
    this.id = id;
    this.usuario = Objects.requireNonNull(usuario);
    setEspecialidad(especialidad);
  }

  public int getId() {
    return id;
  }

  public Usuario getUsuario() {
    return usuario;
  }

  public String getEspecialidad() {
    return especialidad;
  }

  public void setEspecialidad(String v) {
    especialidad = Usuario.obligatorio(v, "Especialidad");
  }

  @Override
  public String toString() {
    return usuario.getNombreCompleto();
  }

  private java.util.List<HorarioTrabajo> horarios = new java.util.ArrayList<>();

  public void setId(int valor) {
    id = valor;
  }

  public void setUsuario(Usuario valor) {
    usuario = java.util.Objects.requireNonNull(valor);
  }

  public java.util.List<HorarioTrabajo> getHorarios() {
    return java.util.List.copyOf(horarios);
  }

  public void setHorarios(java.util.List<HorarioTrabajo> valores) {
    var copia = new java.util.ArrayList<>(valores);
    if (copia.stream().anyMatch(h -> h.getEmpleado() != this))
      throw new IllegalArgumentException("Horario de otro empleado.");
    horarios = copia;
  }
}
