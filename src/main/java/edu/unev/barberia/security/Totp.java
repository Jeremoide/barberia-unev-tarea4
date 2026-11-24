package edu.unev.barberia.security;

import java.nio.ByteBuffer;
import java.security.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238: HMAC-SHA1, 30 segundos, 6 dígitos. */
public final class Totp {
  private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  private Totp() {}

  public static String secret() {
    byte[] b = new byte[20];
    new SecureRandom().nextBytes(b);
    return encode(b);
  }

  public static String encode(byte[] bytes) {
    StringBuilder s = new StringBuilder();
    int buffer = 0, bits = 0;
    for (byte b : bytes) {
      buffer = (buffer << 8) | (b & 255);
      bits += 8;
      while (bits >= 5) {
        s.append(ALPHABET.charAt((buffer >> (bits -= 5)) & 31));
      }
    }
    if (bits > 0) s.append(ALPHABET.charAt((buffer << (5 - bits)) & 31));
    return s.toString();
  }

  private static byte[] decode(String s) {
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    int buffer = 0, bits = 0;
    for (char c : s.toUpperCase(Locale.ROOT).replace("=", "").toCharArray()) {
      int n = ALPHABET.indexOf(c);
      if (n < 0) throw new IllegalArgumentException("Secreto inválido");
      buffer = (buffer << 5) | n;
      bits += 5;
      if (bits >= 8) out.write((buffer >> (bits -= 8)) & 255);
    }
    return out.toByteArray();
  }

  public static String code(String secret, long step) {
    try {
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(decode(secret), "HmacSHA1"));
      byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array());
      int o = hash[19] & 15;
      int n =
          ((hash[o] & 127) << 24)
              | ((hash[o + 1] & 255) << 16)
              | ((hash[o + 2] & 255) << 8)
              | (hash[o + 3] & 255);
      return String.format(Locale.ROOT, "%06d", n % 1000000);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException(e);
    }
  }

  public static long verify(String secret, String value, long seconds, long last) {
    if (value == null || !value.matches("[0-9]{6}")) return -1;
    long current = seconds / 30;
    for (long t = current - 1; t <= current + 1; t++)
      if (t > last
          && MessageDigest.isEqual(
              code(secret, t).getBytes(java.nio.charset.StandardCharsets.UTF_8),
              value.getBytes(java.nio.charset.StandardCharsets.UTF_8))) return t;
    return -1;
  }
}
