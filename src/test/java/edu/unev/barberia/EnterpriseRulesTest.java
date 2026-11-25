package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;

import edu.unev.barberia.service.EnterpriseService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EnterpriseRulesTest {
  BigDecimal n(String s) {
    return new BigDecimal(s);
  }

  @Test
  void impuestoSobreBaseDescontada() {
    var t = EnterpriseService.totals(n("1000"), n("100"), n("18"));
    assertEquals(n("1062.00"), t.total());
    assertEquals(n("162.00"), t.tax());
  }

  @Test
  void ventaSinImpuesto() {
    assertEquals(n("500.00"), EnterpriseService.totals(n("500"), n("0"), n("0")).total());
  }

  @Test
  void descuentoExcesivo() {
    assertThrows(
        IllegalArgumentException.class, () -> EnterpriseService.totals(n("10"), n("11"), n("0")));
  }

  @Test
  void porcentajeInvalido() {
    assertThrows(
        IllegalArgumentException.class, () -> EnterpriseService.totals(n("10"), n("0"), n("101")));
  }

  @Test
  void importeNegativo() {
    assertThrows(IllegalArgumentException.class, () -> EnterpriseService.money(n("-1")));
  }

  @Test
  void precisionMonetaria() {
    assertThrows(IllegalArgumentException.class, () -> EnterpriseService.money(n("1.001")));
  }

  @Test
  void redondeoContable() {
    assertEquals(n("0.01"), EnterpriseService.totals(n("0.03"), n("0"), n("18")).tax());
  }

  @Test
  void cantidadCero() {
    assertThrows(IllegalArgumentException.class, () -> EnterpriseService.quantity(0));
  }

  @Test
  void cantidadMaxima() {
    assertDoesNotThrow(() -> EnterpriseService.quantity(10000));
    assertThrows(IllegalArgumentException.class, () -> EnterpriseService.quantity(10001));
  }

  @Test
  void importeNulo() {
    assertThrows(IllegalArgumentException.class, () -> EnterpriseService.money(null));
  }
}
