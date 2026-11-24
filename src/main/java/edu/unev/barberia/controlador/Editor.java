package edu.unev.barberia.controlador;

import edu.unev.barberia.App;
import edu.unev.barberia.dao.Store;
import edu.unev.barberia.service.BarberiaService.Option;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

/** Un mismo FXML modal contiene campos según el módulo, sin SQL en la vista. */
public final class Editor {
  @FXML private Label titulo, error;
  @FXML private VBox campos;
  @FXML private Button guardar;
  private Stage stage;
  private String module;
  private Map<String, Object> row;
  private Runnable refreshed;
  private final Map<String, Control> fields = new LinkedHashMap<>();

  public static void open(
      String module,
      Map<String, Object> row,
      Map<String, List<Option>> catalogs,
      Runnable refreshed)
      throws Exception {
    var loader = new FXMLLoader(Editor.class.getResource("/vistas/editor.fxml"));
    Parent root = loader.load();
    Editor e = loader.getController();
    e.module = module;
    e.row = row;
    e.refreshed = refreshed;
    e.stage = new Stage();
    e.stage.initOwner(App.window);
    e.stage.initModality(Modality.APPLICATION_MODAL);
    e.stage.setTitle((row == null ? "Nuevo " : "Editar ") + module);
    e.build(catalogs);
    Scene scene =
        new Scene(
            root,
            Math.max(350, Math.min(520, App.window.getWidth() - 24)),
            Math.min(650, App.window.getHeight() - 30));
    scene.getStylesheets().add(Editor.class.getResource("/vistas/styles.css").toExternalForm());
    e.stage.setScene(scene);
    e.stage.showAndWait();
  }

  private String val(String k) {
    return row == null ? "" : Store.text(row, k);
  }

  private void field(String key, String title, Control control) {
    fields.put(key, control);
    control.setMaxWidth(Double.MAX_VALUE);
    campos.getChildren().addAll(new Label(title), control);
  }

  private void text(String k, String title) {
    field(k, title, new TextField(val(k)));
  }

  private void choice(String key, String title, List<Option> options, long selected) {
    ComboBox<Option> box = new ComboBox<>();
    box.getItems().setAll(options);
    options.stream().filter(o -> o.id() == selected).findFirst().ifPresent(box::setValue);
    box.setPromptText("Seleccione");
    field(key, title, box);
  }

  private void build(Map<String, List<Option>> cats) {
    titulo.setText((row == null ? "Crear " : "Editar ") + module);
    switch (module) {
      case "Clientes" -> {
        text("nombre", "Nombre completo");
        text("telefono", "Teléfono");
        text("correo", "Correo (opcional)");
      }
      case "Servicios" -> {
        text("nombre", "Nombre del servicio");
        text("precio", "Precio RD$");
        text("duracion", "Duración en minutos");
        choice(
            "categoria_id",
            "Categoría",
            cats.get("categorias"),
            row == null ? 0 : Store.id(row, "categoria_id"));
      }
      case "Citas" -> {
        choice(
            "cliente_id",
            "Cliente",
            cats.get("clientes"),
            row == null ? 0 : Store.id(row, "cliente_id"));
        choice(
            "empleado_id",
            "Barbero",
            cats.get("empleados"),
            row == null ? 0 : Store.id(row, "empleado_id"));
        choice(
            "servicio_id",
            "Servicio",
            cats.get("servicios"),
            row == null ? 0 : Store.id(row, "servicio_id"));
        LocalDateTime dt =
            row == null
                ? LocalDate.now().plusDays(1).atTime(10, 0)
                : ((java.sql.Timestamp) row.get("fecha")).toLocalDateTime();
        field("dia", "Fecha", new DatePicker(dt.toLocalDate()));
        field("hora", "Hora (HH:mm)", new TextField(dt.toLocalTime().toString()));
      }
      case "Usuarios" -> {
        text("nombre", "Nombre completo");
        text("usuario", "Usuario");
        text("correo", "Correo electrónico");
        field("clave", "Contraseña (vacía conserva la actual)", new PasswordField());
        choice("rol_id", "Rol", cats.get("roles"), row == null ? 0 : Store.id(row, "rol_id"));
        CheckBox active = new CheckBox("Cuenta activa");
        active.setSelected(row == null || Store.yes(row, "activo"));
        field("activo", "Acceso", active);
      }
    }
  }

  private String text(String k) {
    return ((TextField) fields.get(k)).getText().trim();
  }

  private long option(String k) {
    var o = ((ComboBox<Option>) fields.get(k)).getValue();
    if (o == null) throw new IllegalArgumentException("Complete todas las selecciones.");
    return o.id();
  }

  @FXML
  private void save() {
    try {
      long id = row == null ? 0 : Store.id(row, "id");
      java.util.concurrent.Callable<Void> job =
          switch (module) {
            case "Clientes" -> {
              String n = text("nombre"), p = text("telefono"), e = text("correo");
              yield () -> {
                App.service.client(App.session, id, n, p, e);
                return null;
              };
            }
            case "Servicios" -> {
              String n = text("nombre");
              BigDecimal price = new BigDecimal(text("precio"));
              int mins = Integer.parseInt(text("duracion"));
              long cat = option("categoria_id");
              yield () -> {
                App.service.service(App.session, id, n, price, mins, cat);
                return null;
              };
            }
            case "Citas" -> {
              long cl = option("cliente_id"),
                  em = option("empleado_id"),
                  se = option("servicio_id");
              LocalDate day = ((DatePicker) fields.get("dia")).getValue();
              if (day == null) throw new IllegalArgumentException("Seleccione fecha.");
              LocalDateTime date = LocalDateTime.of(day, LocalTime.parse(text("hora")));
              yield () -> {
                App.service.appointment(App.session, id, cl, em, se, date);
                return null;
              };
            }
            case "Usuarios" -> {
              String n = text("nombre"),
                  u = text("usuario"),
                  e = text("correo"),
                  p = ((PasswordField) fields.get("clave")).getText();
              long r = option("rol_id");
              boolean active = ((CheckBox) fields.get("activo")).isSelected();
              yield () -> {
                App.service.user(App.session, id, n, u, e, p, r, active);
                return null;
              };
            }
            default -> throw new IllegalArgumentException("Módulo inválido.");
          };
      guardar.setDisable(true);
      error
          .textProperty()
          .addListener((o, a, b) -> {
            if (!b.equals("Procesando…")) guardar.setDisable(false);
          });
      App.work(
          job,
          v -> {
            stage.close();
            refreshed.run();
          },
          error);
    } catch (Exception ex) {
      error.setText(
          ex instanceof NumberFormatException
              ? "Revise precio y duración: deben ser números."
              : ex instanceof java.time.format.DateTimeParseException
                  ? "Hora inválida. Use HH:mm (por ejemplo 10:30)."
                  : App.friendly(ex));
    }
  }

  @FXML
  private void cancel() {
    stage.close();
  }
}
