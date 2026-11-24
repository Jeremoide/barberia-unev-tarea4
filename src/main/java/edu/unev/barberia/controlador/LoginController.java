package edu.unev.barberia.controlador;

import edu.unev.barberia.App;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public final class LoginController {
  @FXML private TextField usuario;
  @FXML private PasswordField clave;
  @FXML private Label mensaje;
  @FXML private Button iniciar;

  @FXML
  private void login() {
    String u = usuario.getText(), p = clave.getText();
    if (u.isBlank() || p.isBlank()) {
      mensaje.setText("Complete usuario y contraseña.");
      return;
    }
    iniciar.setDisable(true);
    App.work(
        () -> App.auth.begin(u, p),
        c -> {
          iniciar.setDisable(false);
          clave.clear();
          if (c == null) {
            mensaje.setText("Credenciales incorrectas, cuenta inactiva o bloqueo temporal.");
            return;
          }
          App.challenge = c;
          App.show("verificar");
        },
        mensaje);
    mensaje
        .textProperty()
        .addListener((o, a, b) -> {
          if (!b.equals("Procesando…")) iniciar.setDisable(false);
        });
  }

  @FXML
  private void salir() {
    Platform.exit();
  }
}
