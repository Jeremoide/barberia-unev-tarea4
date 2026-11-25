package edu.unev.barberia;

import edu.unev.barberia.db.Database;
import edu.unev.barberia.security.*;
import edu.unev.barberia.service.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.*;
import javafx.application.*;
import javafx.concurrent.Task;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.stage.*;

public class App extends Application {
  public static Stage window;
  public static AuthService auth;
  public static AuthService.Challenge challenge;
  public static Session session;
  public static final BarberiaService service = new BarberiaService();
  public static final Logger LOG = Logger.getLogger("barberia");
  private static final ExecutorService IO =
      Executors.newFixedThreadPool(
          3,
          r -> {
            Thread t = new Thread(r, "barberia-io");
            t.setDaemon(true);
            return t;
          });

  @Override
  public void start(Stage stage) throws Exception {
    java.util.Locale.setDefault(java.util.Locale.forLanguageTag("es-DO"));
    window = stage;
    Files.createDirectories(Path.of("logs"));
    FileHandler fh = new FileHandler("logs/aplicacion.log", 1000000, 3, true);
    fh.setFormatter(new SimpleFormatter());
    LOG.addHandler(fh);
    auth = new AuthService();
    stage.setTitle("Barbería UNEV · 3.0");
    show("login");
    stage.show();
  }

  public static void show(String name) {
    try {
      var loader = new FXMLLoader(App.class.getResource("/vistas/" + name + ".fxml"));
      Parent root = loader.load();
      Scene scene =
          new Scene(root, name.equals("panel") ? 1180 : 480, name.equals("panel") ? 760 : 650);
      scene.getStylesheets().add(App.class.getResource("/vistas/styles.css").toExternalForm());
      window.setScene(scene);
      window.setMinWidth(360);
      window.setMinHeight(550);
      window.centerOnScreen();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "No se pudo abrir la vista", ex);
      new Alert(Alert.AlertType.ERROR, "No se pudo abrir la pantalla.").showAndWait();
    }
  }

  public static <T> void work(Callable<T> job, Consumer<T> done, Label message) {
    if (message != null) message.setText("Procesando…");
    Task<T> task =
        new Task<>() {
          protected T call() throws Exception {
            return job.call();
          }
        };
    task.setOnSucceeded(e -> {
      if (message != null) message.setText("");
      done.accept(task.getValue());
    });
    task.setOnFailed(e -> {
      Throwable ex = task.getException();
      LOG.log(Level.WARNING, "Operación fallida", ex);
      if (message != null) message.setText(friendly(ex));
    });
    IO.submit(task);
  }

  public static String friendly(Throwable ex) {
    if (ex instanceof java.sql.SQLException sql && sql.getErrorCode() == 1062)
      return "Ya existe un registro con ese código, usuario o identificador único.";
    if (ex instanceof java.sql.SQLException
        || ex instanceof com.zaxxer.hikari.pool.HikariPool.PoolInitializationException) {
      return "No se pudo completar la operación en la base de datos. Revise conexión o registros"
          + " relacionados. Detalle en logs.";
    }
    return ex.getMessage() == null ? "No se pudo completar la operación." : ex.getMessage();
  }

  @Override
  public void stop() {
    IO.shutdownNow();
    Database.close();
  }

  public static void main(String[] args) {
    launch(args);
  }
}
