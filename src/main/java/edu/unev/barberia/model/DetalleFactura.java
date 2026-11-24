package edu.unev.barberia.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Copia la descripción y precio acordado; no consulta el precio actual del catálogo. */
public class DetalleFactura {
  private int id;
  private Cita cita;
  private String descripcionServicio;
  private BigDecimal importe;
  private boolean bloqueado;

  public DetalleFactura() {}

  public DetalleFactura(int id, Cita cita) {
    setId(id);
    setCita(cita);
    setDescripcionServicio(cita.getServicio().getNombre());
    setImporte(cita.getPrecioAcordado());
  }

  void bloquear() {
    bloqueado = true;
  }

  private void comprobarEdicion() {
    if (bloqueado) throw new IllegalStateException("Detalle de factura emitida.");
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    comprobarEdicion();
    this.id = id;
  }

  public Cita getCita() {
    return cita;
  }

  public void setCita(Cita cita) {
    comprobarEdicion();
    Objects.requireNonNull(cita);
    if (cita.getEstado() != Cita.Estado.COMPLETADA)
      throw new IllegalArgumentException("Cita no completada.");
    this.cita = cita;
  }

  public String getDescripcionServicio() {
    return descripcionServicio;
  }

  public void setDescripcionServicio(String texto) {
    comprobarEdicion();
    descripcionServicio = Usuario.obligatorio(texto, "Descripción");
  }

  public BigDecimal getImporte() {
    return importe;
  }

  public void setImporte(BigDecimal importe) {
    comprobarEdicion();
    if (cita == null || importe == null || importe.compareTo(cita.getPrecioAcordado()) != 0)
      throw new IllegalArgumentException("El importe debe coincidir con la cita.");
    this.importe = importe;
  }

  @Override
  public String toString() {
    return descripcionServicio + " - RD$ " + importe;
  }
}
