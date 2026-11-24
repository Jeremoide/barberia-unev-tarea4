package edu.unev.barberia.security;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public final class Vault {
  private final byte[] key;

  public Vault(String encoded) {
    key = Base64.getDecoder().decode(encoded);
    if (key.length != 32)
      throw new IllegalArgumentException("La clave de cifrado debe tener 32 bytes.");
  }

  public String encrypt(String value) {
    try {
      byte[] iv = new byte[12];
      new SecureRandom().nextBytes(iv);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
      byte[] data = c.doFinal(value.getBytes(StandardCharsets.UTF_8));
      byte[] result = new byte[iv.length + data.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(data, 0, result, iv.length, data.length);
      return Base64.getEncoder().encodeToString(result);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("No se pudo cifrar el secreto 2FA", e);
    }
  }

  public String decrypt(String value) {
    try {
      byte[] b = Base64.getDecoder().decode(value);
      Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
      c.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(128, Arrays.copyOf(b, 12)));
      return new String(c.doFinal(Arrays.copyOfRange(b, 12, b.length)), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(
          "No se pudo descifrar el secreto. Revise la clave de instalación.");
    }
  }
}
