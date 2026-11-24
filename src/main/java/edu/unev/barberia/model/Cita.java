package edu.unev.barberia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** Une un cliente, un barbero y un servicio en una fecha determinada. */
public class Cita {
  /** Constructor vacío para mapeo de datos. Completar sus propiedades antes de usarlo. */
  public Cita() {}

  public enum Estado {
    PENDIENTE,
    COMPLETADA,
    CANCELADA
  }

  private int id;
  private Cliente cliente;
  private Empleado empleado;
  private Servicio servicio;
  private LocalDateTime fecha;
  private BigDecimal precioAcordado;
  private int duracionAcordada;
  private Estado estado = Estado.PENDIENTE;

  public Cita(int id, Cliente cliente, Empleado empleado, Servicio servicio, LocalDateTime fecha) {
    this.id = id;
    this.cliente = Objects.requireNonNull(cliente);
    this.empleado = Objects.requireNonNull(empleado);
    this.servicio = Objects.requireNonNull(servicio);
    this.fecha = Objects.requireNonNull(fecha);
    precioAcordado = servicio.getPrecio();
    duracionAcordada = servicio.getDuracionMinutos();
  }

  public int getId() {
    return id;
  }

  public Cliente getCliente() {
    return cliente;
  }

  public Empleado getEmpleado() {
    return empleado;
  }

  public Servicio getServicio() {
    return servicio;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public LocalDateTime getFin() {
    return fecha.plusMinutes(duracionAcordada);
  }

  public BigDecimal getPrecioAcordado() {
    return precioAcordado;
  }

  public Estado getEstado() {
    return estado;
  }

  public void completar() {
    if (estado != Estado.PENDIENTE)
      throw new IllegalStateException("La cita ya no está pendiente.");
    estado = Estado.COMPLETADA;
  }

  public void cancelar() {
    if (estado != Estado.PENDIENTE)
      throw new IllegalStateException("Solo puede cancelar citas pendientes.");
    estado = Estado.CANCELADA;
  }

  public void setId(int valor) {
    id = valor;
  }

  public void setCliente(Cliente valor) {
    cliente = Objects.requireNonNull(valor);
  }

  public void setEmpleado(Empleado valor) {
    empleado = Objects.requireNonNull(valor);
  }

  public void setServicio(Servicio valor) {
    servicio = Objects.requireNonNull(valor);
  }

  public void setFecha(LocalDateTime valor) {
    fecha = Objects.requireNonNull(valor);
  }

  public void setPrecioAcordado(BigDecimal valor) {
    if (valor == null || valor.signum() <= 0)
      throw new IllegalArgumentException("Precio positivo requerido.");
    precioAcordado = valor;
  }

  public int getDuracionAcordada() {
    return duracionAcordada;
  }

  public void setDuracionAcordada(int valor) {
    if (valor <= 0) throw new IllegalArgumentException("Duración positiva requerida.");
    duracionAcordada = valor;
  }

  public void setEstado(Estado valor) {
    estado = Objects.requireNonNull(valor);
  }

  @Override
  public String toString() {
    return "Cita " + id + " - " + fecha + " - " + estado;
  }
}
