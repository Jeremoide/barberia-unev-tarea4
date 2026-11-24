package edu.unev.barberia.model;

/** Clasifica el catálogo: cortes, barba o tratamientos. */
public class CategoriaServicio {
  private int id;
  private String nombre;
  private String descripcion;

  public CategoriaServicio() {}

  public CategoriaServicio(int id, String nombre, String descripcion) {
    setId(id);
    setNombre(nombre);
    setDescripcion(descripcion);
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getNombre() {
    return nombre;
  }

  public void setNombre(String nombre) {
    this.nombre = Usuario.obligatorio(nombre, "Categoría");
  }

  public String getDescripcion() {
    return descripcion;
  }

  public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
  }

  @Override
  public String toString() {
    return nombre;
  }
}
