package edu.unev.barberia.model;

import java.time.*;
import java.util.Objects;

/** Intervalo semanal de un empleado. Los turnos no cruzan medianoche. */
public class HorarioTrabajo {
  private int id;
  private Empleado empleado;
  private DayOfWeek diaSemana;
  private LocalTime horaInicio;
  private LocalTime horaFin;

  public HorarioTrabajo() {}

  public HorarioTrabajo(int id, Empleado empleado, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
    setId(id);
    setEmpleado(empleado);
    setDiaSemana(dia);
    setIntervalo(inicio, fin);
  }

  public boolean incluye(LocalDateTime inicio, LocalDateTime fin) {
    return inicio != null
        && fin != null
        && fin.isAfter(inicio)
        && inicio.toLocalDate().equals(fin.toLocalDate())
        && inicio.getDayOfWeek() == diaSemana
        && !inicio.toLocalTime().isBefore(horaInicio)
        && !fin.toLocalTime().isAfter(horaFin);
  }

  public void setIntervalo(LocalTime inicio, LocalTime fin) {
    Objects.requireNonNull(inicio);
    Objects.requireNonNull(fin);
    if (!fin.isAfter(inicio))
      throw new IllegalArgumentException("El fin debe ser posterior al inicio.");
    horaInicio = inicio;
    horaFin = fin;
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
    this.empleado = Objects.requireNonNull(empleado);
  }

  public DayOfWeek getDiaSemana() {
    return diaSemana;
  }

  public void setDiaSemana(DayOfWeek dia) {
    diaSemana = Objects.requireNonNull(dia);
  }

  public LocalTime getHoraInicio() {
    return horaInicio;
  }

  public void setHoraInicio(LocalTime inicio) {
    Objects.requireNonNull(inicio);
    if (horaFin != null && !horaFin.isAfter(inicio))
      throw new IllegalArgumentException("Intervalo inválido.");
    horaInicio = inicio;
  }

  public LocalTime getHoraFin() {
    return horaFin;
  }

  public void setHoraFin(LocalTime fin) {
    Objects.requireNonNull(fin);
    if (horaInicio != null && !fin.isAfter(horaInicio))
      throw new IllegalArgumentException("Intervalo inválido.");
    horaFin = fin;
  }

  @Override
  public String toString() {
    return diaSemana + " " + horaInicio + " - " + horaFin;
  }
}
