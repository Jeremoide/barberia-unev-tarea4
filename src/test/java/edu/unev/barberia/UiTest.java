package edu.unev.barberia;

import static org.junit.jupiter.api.Assertions.*;

import edu.unev.barberia.dao.Store;
import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.Totp;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "BARBERIA_UI", matches = "true")
class UiTest {
  static Path images = Path.of(System.getProperty("barberia.evidenceDir", "target/evidencias"));

  static <T> T fx(Callable<T> c) throws Exception {
    FutureTask<T> task = new FutureTask<>(c);
    Platform.runLater(task);
    return task.get(15, TimeUnit.SECONDS);
  }

  static void waitFor(Callable<Boolean> condition) throws Exception {
    long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
    while (System.nanoTime() < end) {
      if (fx(condition)) return;
      Thread.sleep(100);
    }
    fail("La interfaz no completó la operación.");
  }

  static Button button(Parent root, String name) {
    return root.lookupAll(".button").stream()
        .filter(n -> n instanceof Button b && b.getText().equals(name))
        .map(n -> (Button) n)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Botón no encontrado: " + name));
  }

  static boolean rootVisible(String id) throws Exception {
    return fx(() -> root().lookup(id).isVisible());
  }

  static Parent root() {
    return App.window.getScene().getRoot();
  }

  static void click(String name) throws Exception {
    fx(() -> {
      button(root(), name).fire();
      return null;
    });
  }

  static void size(int w, int h) throws Exception {
    fx(() -> {
      Stage s = App.window;
      s.setWidth(w + s.getWidth() - s.getScene().getWidth());
      s.setHeight(h + s.getHeight() - s.getScene().getHeight());
      return null;
    });
    Thread.sleep(180);
    fx(() -> {
      Stage st = App.window;
      st.setWidth(st.getWidth() + w - st.getScene().getWidth());
      st.setHeight(st.getHeight() + h - st.getScene().getHeight());
      return null;
    });
    Thread.sleep(100);
  }

  static void shot(String name) throws Exception {
    fx(() -> {
      root().applyCss();
      root().layout();
      Files.createDirectories(images);
      WritableImage im = App.window.getScene().snapshot(null);
      ImageIO.write(
          SwingFXUtils.fromFXImage(im, null), "png", images.resolve(name + ".png").toFile());
      return null;
    });
  }

  @Test
  void flujoRealYCapturas() throws Exception {
    CountDownLatch start = new CountDownLatch(1);
    Platform.startup(start::countDown);
    assertTrue(start.await(10, TimeUnit.SECONDS));
    // Enrollment is intentionally reset for the academic demo account, so the owner can enroll
    // after tests.
    try (var c = Database.connection()) {
      Store.update(
          c,
          "DELETE FROM usuario_2fa WHERE usuario_id IN (SELECT id FROM usuarios WHERE usuario IN"
              + " ('admin.demo','recepcion.demo','barbero.demo'))");
      Store.update(
          c,
          "UPDATE usuarios SET fallos=0,bloqueado_hasta=NULL WHERE usuario IN"
              + " ('admin.demo','recepcion.demo','barbero.demo')");
    }
    try {
      fx(() -> {
        new App().start(new Stage());
        return null;
      });
      for (int[] dim : new int[][] {{1366, 768}, {1024, 768}, {768, 1024}, {412, 915}}) {
        size(dim[0], dim[1]);
        shot("login_" + dim[0] + "x" + dim[1]);
      }
      fx(() -> {
        ((TextField) root().lookup("#usuario")).setText("admin.demo");
        ((PasswordField) root().lookup("#clave"))
            .setText(Database.option("demo.admin.password", ""));
        return null;
      });
      click("Continuar con verificación");
      waitFor(() -> App.challenge != null && root().lookup("#codigo") != null);
      assertNull(App.session);
      String secret = App.challenge.secret();
      assertNotNull(secret);
      fx(() -> {
        ((TextField) root().lookup("#secreto")).setText("Secreto y QR omitidos de la evidencia");
        root().lookup("#qr").setVisible(false);
        return null;
      });
      shot("verificacion_2fa_sin_secretos");
      fx(() -> {
        ((TextField) root().lookup("#secreto")).setText(secret);
        root().lookup("#qr").setVisible(true);
        return null;
      });
      fx(() -> {
        ((TextField) root().lookup("#codigo"))
            .setText(Totp.code(secret, java.time.Instant.now().getEpochSecond() / 30));
        return null;
      });
      click("Verificar e ingresar");
      waitFor(() -> App.session != null && root().lookupAll(".card").size() == 4);
      for (int[] dim : new int[][] {{1366, 768}, {1024, 768}, {768, 1024}, {412, 915}}) {
        size(dim[0], dim[1]);
        shot("dashboard_" + dim[0] + "x" + dim[1]);
      }
      new edu.unev.barberia.service.ReportService()
          .export(
              App.session,
              java.time.LocalDate.now().minusDays(20),
              java.time.LocalDate.now().plusDays(7),
              images.resolve("Reporte_Demo_Administrador.xlsx"));
      size(1366, 768);
      click("Clientes");
      waitFor(() ->
          root().lookup(".table-view") != null
              && ((TableView<?>) root().lookup(".table-view")).getItems().size() >= 8);
      for (int[] dim : new int[][] {{1366, 768}, {1024, 768}, {768, 1024}, {412, 915}}) {
        size(dim[0], dim[1]);
        shot("clientes_" + dim[0] + "x" + dim[1]);
      }
      size(1366, 768);
      // showAndWait runs a nested event loop: queue the click without waiting for the modal to
      // close.
      Platform.runLater(() -> button(root(), "Crear").fire());
      waitFor(() ->
          Window.getWindows().stream()
              .anyMatch(
                  w -> w instanceof Stage s && s.isShowing() && s.getTitle().contains("Nuevo")));
      Stage modal =
          fx(() ->
              (Stage)
                  Window.getWindows().stream()
                      .filter(w ->
                          w instanceof Stage s && s.isShowing() && s.getTitle().contains("Nuevo"))
                      .findFirst()
                      .orElseThrow());
      fx(() -> {
        button(modal.getScene().getRoot(), "Guardar").fire();
        return null;
      });
      waitFor(() -> ((Label) modal.getScene().lookup("#error")).getText().contains("obligatorio"));
      fx(() -> {
        WritableImage im = modal.getScene().snapshot(null);
        ImageIO.write(
            SwingFXUtils.fromFXImage(im, null),
            "png",
            images.resolve("modal_validacion.png").toFile());
        button(modal.getScene().getRoot(), "Cancelar").fire();
        return null;
      });
      shot("clientes_final");
      for (String role : new String[] {"recepcion", "barbero"}) {
        click("Cerrar sesión");
        fx(() -> {
          ((TextField) root().lookup("#usuario")).setText(role + ".demo");
          ((PasswordField) root().lookup("#clave"))
              .setText(
                  Database.option(
                      "demo." + (role.equals("recepcion") ? "recepcion" : "barbero") + ".password",
                      ""));
          return null;
        });
        click("Continuar con verificación");
        waitFor(() -> App.challenge != null && root().lookup("#codigo") != null);
        String key = App.challenge.secret();
        assertNotNull(key);
        fx(() -> {
          ((TextField) root().lookup("#codigo"))
              .setText(Totp.code(key, java.time.Instant.now().getEpochSecond() / 30));
          return null;
        });
        click("Verificar e ingresar");
        waitFor(() -> App.session != null && root().lookupAll(".card").size() == 4);
        size(1366, 768);
        shot("rol_" + role);
        assertFalse(rootVisible("#usuarios"));
        if (role.equals("barbero")) assertFalse(rootVisible("#clientesButton"));
      }
    } finally {
      fx(() -> {
        App.window.close();
        Platform.exit();
        return null;
      });
      try (var c = Database.connection()) {
        Store.update(
            c,
            "DELETE FROM usuario_2fa WHERE usuario_id IN (SELECT id FROM usuarios WHERE usuario IN"
                + " ('admin.demo','recepcion.demo','barbero.demo'))");
        Store.update(
            c,
            "UPDATE usuarios SET fallos=0,bloqueado_hasta=NULL WHERE usuario IN"
                + " ('admin.demo','recepcion.demo','barbero.demo')");
      }
      Database.close();
    }
  }
}
