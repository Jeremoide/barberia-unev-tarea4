package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;
import edu.unev.barberia.service.BarberiaService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessRulesTest {
  @Test @DisplayName("UT01 nombre válido conserva su contenido")
  void nombreValido() {
    assertEquals("Ana Demo", BarberiaService.required("Ana Demo", "Nombre"));
  }
  @Test @DisplayName("UT02 nombre válido elimina espacios exteriores")
  void nombreConEspaciosExteriores() {
    assertEquals("Luis Demo", BarberiaService.required("  Luis Demo  ", "Nombre"));
  }
  @Test @DisplayName("UT03 nombre en blanco pertenece a la partición inválida")
  void nombreEnBlanco() {
    assertThrows(IllegalArgumentException.class, () -> BarberiaService.required("   ", "Nombre"));
  }
  @Test @DisplayName("UT04 límite de 150 caracteres permitido")
  void longitudMaxima() {
    assertEquals(150, BarberiaService.required("A".repeat(150), "Nombre").length());
  }
  @Test @DisplayName("UT05 límite de 151 caracteres rechazado")
  void longitudExcedida() {
    assertThrows(IllegalArgumentException.class,
        () -> BarberiaService.required("A".repeat(151), "Nombre"));
  }
  @Test @DisplayName("UT06 rango de un solo día permitido")
  void rangoDeUnDia() {
    LocalDate date = LocalDate.of(2026, 11, 24);
    assertDoesNotThrow(() -> BarberiaService.range(date, date));
  }
  @Test @DisplayName("UT07 rango invertido rechazado")
  void rangoInvertido() {
    assertThrows(IllegalArgumentException.class, () -> BarberiaService.range(
        LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 24)));
  }
}
