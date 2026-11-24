package edu.unev.barberia.model;

import java.time.*;
import java.util.*;

/** Agrega citas sin apropiarse de su historial y consulta los horarios del empleado. */
public class Agenda {
  private int id;
  private Empleado empleado;
  private List<Cita> citas = new ArrayList<>();

  public Agenda() {}

  public Agenda(int id, Empleado empleado) {
    setId(id);
    setEmpleado(empleado);
  }

  public boolean estaDisponible(LocalDateTime fecha, int minutos) {
    if (empleado == null || fecha == null || minutos <= 0) return false;
    LocalDateTime fin = fecha.plusMinutes(minutos);
    boolean enJornada = empleado.getHorarios().stream().anyMatch(h -> h.incluye(fecha, fin));
    return enJornada
        && citas.stream()
            .noneMatch(c ->
                c.getEstado() != Cita.Estado.CANCELADA
                    && fecha.isBefore(c.getFin())
                    && fin.isAfter(c.getFecha()));
  }

  public Cita agendar(Cliente cliente, Servicio servicio, LocalDateTime fecha) {
    Objects.requireNonNull(cliente);
    Objects.requireNonNull(servicio);
    if (fecha == null
        || !fecha.isAfter(LocalDateTime.now())
        || !estaDisponible(fecha, servicio.getDuracionMinutos()))
      throw new IllegalArgumentException("Fecha fuera de jornada, pasada u ocupada.");
    // Id 0 es transitorio: la base de datos asignará el identificador definitivo.
    Cita cita = new Cita(0, cliente, empleado, servicio, fecha);
    citas.add(cita);
    return cita;
  }

  public List<Cita> consultarCitas(LocalDate dia) {
    return citas.stream().filter(c -> c.getFecha().toLocalDate().equals(dia)).toList();
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public Empleado getEmpleado() {
    return empleado;
  }

  public void setEmpleado(Empleado empleado) {
    Objects.requireNonNull(empleado);
    if (citas.stream().anyMatch(c -> c.getEmpleado() != empleado))
      throw new IllegalArgumentException("La agenda contiene citas de otro empleado.");
    this.empleado = empleado;
  }

  public List<Cita> getCitas() {
    return List.copyOf(citas);
  }

  public void setCitas(List<Cita> citas) {
    var copia = new ArrayList<>(citas);
    if (copia.stream().anyMatch(c -> c == null || c.getEmpleado() != empleado))
      throw new IllegalArgumentException("Cita de otro empleado.");
    this.citas = copia;
  }

  @Override
  public String toString() {
    return "Agenda " + id + " - " + empleado;
  }
}
