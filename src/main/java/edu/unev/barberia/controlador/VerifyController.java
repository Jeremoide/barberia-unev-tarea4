package edu.unev.barberia.controlador;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import edu.unev.barberia.App;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;

public final class VerifyController {
  @FXML private TextField codigo, secreto;
  @FXML private Label instrucciones, mensaje;
  @FXML private ImageView qr;
  @FXML private Button verificar;

  @FXML
  private void initialize() throws Exception {
    var ch = App.challenge;
    instrucciones.setText(
        ch.enroll()
            ? "Vincule su cuenta en Google o Microsoft Authenticator y confirme el código para"
                  + " activar 2FA."
            : "Introduzca el código actual de su aplicación Authenticator. Máximo 3 intentos.");
    secreto.setManaged(ch.enroll());
    secreto.setVisible(ch.enroll());
    qr.setManaged(ch.enroll());
    qr.setVisible(ch.enroll());
    if (ch.enroll()) {
      secreto.setText(ch.secret());
      String uri =
          "otpauth://totp/BarberiaUNEV:"
              + java.net.URLEncoder.encode(ch.usuario(), java.nio.charset.StandardCharsets.UTF_8)
              + "?secret="
              + ch.secret()
              + "&issuer=BarberiaUNEV&algorithm=SHA1&digits=6&period=30";
      var bits = new QRCodeWriter().encode(uri, BarcodeFormat.QR_CODE, 220, 220);
      qr.setImage(
          SwingFXUtils.toFXImage(
              com.google.zxing.client.j2se.MatrixToImageWriter.toBufferedImage(bits), null));
    }
  }

  @FXML
  private void verify() {
    String code = codigo.getText().trim();
    if (!code.matches("[0-9]{6}")) {
      mensaje.setText("Escriba seis dígitos.");
      return;
    }
    verificar.setDisable(true);
    App.work(
        () -> App.auth.verify(App.challenge.token(), code),
        s -> {
          verificar.setDisable(false);
          codigo.clear();
          if (s == null) {
            mensaje.setText(
                "Código incorrecto o cuenta bloqueada. Tras 3 fallos espere 5 minutos y vuelva al"
                    + " Login.");
            return;
          }
          App.session = s;
          App.challenge = null;
          App.show("panel");
        },
        mensaje);
    mensaje
        .textProperty()
        .addListener((o, a, b) -> {
          if (!b.equals("Procesando…")) verificar.setDisable(false);
        });
  }

  @FXML
  private void volver() {
    if (App.challenge != null) App.auth.abandon(App.challenge.token());
    App.challenge = null;
    App.show("login");
  }
}
