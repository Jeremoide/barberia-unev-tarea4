package edu.unev.barberia.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/** Comprobante del cliente; composición con sus detalles e inmutable al emitirse. */
public class Factura {
  private int id;
  private String numero;
  private Cliente cliente;
  private LocalDateTime fechaEmision;
  private List<DetalleFactura> detalles = new ArrayList<>();
  private EstadoFactura estado = EstadoFactura.BORRADOR;

  public Factura() {}

  public Factura(int id, String numero, Cliente cliente) {
    setId(id);
    setNumero(numero);
    setCliente(cliente);
  }

  private void comprobarEdicion() {
    if (estado == EstadoFactura.EMITIDA)
      throw new IllegalStateException("La factura ya fue emitida.");
  }

  private void validarDetalle(DetalleFactura detalle) {
    Cita cita = Objects.requireNonNull(detalle.getCita());
    if (cliente == null
        || cita.getCliente().getId() != cliente.getId()
        || cita.getEstado() != Cita.Estado.COMPLETADA
        || detalle.getImporte() == null
        || detalle.getImporte().compareTo(cita.getPrecioAcordado()) != 0)
      throw new IllegalArgumentException(
          "Detalle de otro cliente, cita pendiente o importe inválido.");
  }

  private static boolean mismaCita(Cita a, Cita b) {
    return a == b || (a.getId() > 0 && a.getId() == b.getId());
  }

  public void agregarCita(Cita cita) {
    comprobarEdicion();
    if (detalles.stream().anyMatch(d -> mismaCita(d.getCita(), cita)))
      throw new IllegalArgumentException("Cita duplicada.");
    DetalleFactura detalle = new DetalleFactura(0, cita);
    validarDetalle(detalle);
    detalles.add(detalle);
  }

  public BigDecimal calcularTotal() {
    return detalles.stream()
        .map(DetalleFactura::getImporte)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public void emitir() {
    comprobarEdicion();
    Usuario.obligatorio(numero, "Número");
    Objects.requireNonNull(cliente);
    if (detalles.isEmpty())
      throw new IllegalStateException("La factura necesita al menos un detalle.");
    detalles.forEach(this::validarDetalle);
    detalles.forEach(DetalleFactura::bloquear);
    fechaEmision = LocalDateTime.now();
    estado = EstadoFactura.EMITIDA;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    comprobarEdicion();
    this.id = id;
  }

  public String getNumero() {
    return numero;
  }

  public void setNumero(String numero) {
    comprobarEdicion();
    this.numero = Usuario.obligatorio(numero, "Número");
  }

  public Cliente getCliente() {
    return cliente;
  }

  public void setCliente(Cliente cliente) {
    comprobarEdicion();
    Objects.requireNonNull(cliente);
    if (detalles.stream().anyMatch(d -> d.getCita().getCliente().getId() != cliente.getId()))
      throw new IllegalArgumentException("Los detalles son de otro cliente.");
    this.cliente = cliente;
  }

  public LocalDateTime getFechaEmision() {
    return fechaEmision;
  }

  public void setFechaEmision(LocalDateTime fecha) {
    comprobarEdicion();
    fechaEmision = fecha;
  }

  public List<DetalleFactura> getDetalles() {
    return List.copyOf(detalles);
  }

  public void setDetalles(List<DetalleFactura> valores) {
    comprobarEdicion();
    var copia = new ArrayList<>(valores);
    copia.forEach(this::validarDetalle);
    for (int i = 0; i < copia.size(); i++)
      for (int j = i + 1; j < copia.size(); j++)
        if (mismaCita(copia.get(i).getCita(), copia.get(j).getCita()))
          throw new IllegalArgumentException("Cita duplicada.");
    detalles = copia;
  }

  public EstadoFactura getEstado() {
    return estado;
  }

  public void setEstado(EstadoFactura estado) {
    Objects.requireNonNull(estado);
    if (estado == EstadoFactura.EMITIDA) emitir();
    else {
      comprobarEdicion();
      this.estado = estado;
    }
  }

  @Override
  public String toString() {
    return "Factura " + numero + " - " + estado + " - RD$ " + calcularTotal();
  }
}
