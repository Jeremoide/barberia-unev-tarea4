package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;

import edu.unev.barberia.security.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class SecurityTest {
  @Test
  void rfc6238YSinReutilizacion() {
    String secret =
        Totp.encode("12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    assertEquals("287082", Totp.code(secret, 1));
    assertEquals(1, Totp.verify(secret, "287082", 59, -1));
    assertEquals(-1, Totp.verify(secret, "287082", 59, 1));
    assertEquals(-1, Totp.verify(secret, "123", 59, -1));
  }

  @Test
  void cifradoAutenticado() {
    byte[] key = new byte[32];
    new java.security.SecureRandom().nextBytes(key);
    Vault v = new Vault(Base64.getEncoder().encodeToString(key));
    String secret = Totp.secret(), encrypted = v.encrypt(secret);
    assertFalse(encrypted.contains(secret));
    assertEquals(secret, v.decrypt(encrypted));
    byte[] b = Base64.getDecoder().decode(encrypted);
    b[b.length - 1] ^= 1;
    assertThrows(
        IllegalStateException.class, () -> v.decrypt(Base64.getEncoder().encodeToString(b)));
  }

  @Test
  void rolesYExpiracion() {
    Session s = new Session(1, "A", "a", "BARBERO", 1, Instant.now().plusSeconds(10));
    assertThrows(SecurityException.class, () -> s.require("ADMINISTRADOR"));
    assertDoesNotThrow(() -> s.require("BARBERO"));
    assertThrows(
        SecurityException.class,
        () ->
            new Session(1, "A", "a", "ADMINISTRADOR", 0, Instant.now().minusSeconds(1))
                .require("ADMINISTRADOR"));
  }
}
