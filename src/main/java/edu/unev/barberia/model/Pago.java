package edu.unev.barberia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Pago {
  /** Constructor vacío para mapeo de datos. Completar sus propiedades antes de usarlo. */
  public Pago() {}

  private int id;
  private Cita cita;
  private BigDecimal monto;
  private LocalDateTime fecha;

  public Pago(int id, Cita cita, BigDecimal monto) {
    this.id = id;
    this.cita = Objects.requireNonNull(cita);
    if (cita.getEstado() != Cita.Estado.COMPLETADA)
      throw new IllegalArgumentException("Complete la cita antes de cobrar.");
    if (monto == null || monto.compareTo(cita.getPrecioAcordado()) != 0)
      throw new IllegalArgumentException("El monto debe coincidir con el precio acordado.");
    this.monto = monto;
    fecha = LocalDateTime.now();
  }

  public int getId() {
    return id;
  }

  public Cita getCita() {
    return cita;
  }

  public BigDecimal getMonto() {
    return monto;
  }

  public LocalDateTime getFecha() {
    return fecha;
  }

  public void setId(int valor) {
    id = valor;
  }

  public void setCita(Cita valor) {
    if (valor == null || valor.getEstado() != Cita.Estado.COMPLETADA)
      throw new IllegalArgumentException("Cita completada requerida.");
    cita = valor;
  }

  public void setMonto(BigDecimal valor) {
    if (cita == null || valor == null || valor.compareTo(cita.getPrecioAcordado()) != 0)
      throw new IllegalArgumentException("Monto diferente al precio acordado.");
    monto = valor;
  }

  public void setFecha(LocalDateTime valor) {
    fecha = Objects.requireNonNull(valor);
  }

  @Override
  public String toString() {
    return "Pago " + id + " - RD$ " + monto;
  }
}
