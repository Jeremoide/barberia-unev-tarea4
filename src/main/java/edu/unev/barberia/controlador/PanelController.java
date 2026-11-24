package edu.unev.barberia.controlador;

import edu.unev.barberia.App;
import edu.unev.barberia.dao.Store;
import edu.unev.barberia.service.*;
import edu.unev.barberia.service.BarberiaService.Option;
import java.time.*;
import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.*;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class PanelController {
  @FXML private BorderPane root;
  @FXML private VBox sidebar;
  @FXML private MenuButton menu;
  @FXML private Label user, titulo, mensaje;
  @FXML private VBox contenido;
  @FXML private DatePicker desde, hasta;
  @FXML private FlowPane filtros;
  @FXML private Button usuarios, clientesButton, accesosButton;
  private String module = "Dashboard";
  private TableView<Map<String, Object>> table;
  private TilePane cards;
  private int generation;

  @FXML
  private void initialize() {
    user.setText(App.session.nombre() + " · " + App.session.rol());
    desde.setValue(LocalDate.now().withDayOfMonth(1));
    hasta.setValue(LocalDate.now().plusMonths(1));
    usuarios.setVisible(App.session.admin());
    usuarios.setManaged(App.session.admin());
    clientesButton.setVisible(!App.session.barbero());
    clientesButton.setManaged(!App.session.barbero());
    accesosButton.setVisible(App.session.admin());
    accesosButton.setManaged(App.session.admin());
    for (String name :
        List.of("Dashboard", "Clientes", "Servicios", "Citas", "Usuarios", "Accesos")) {
      if ((name.equals("Usuarios") || name.equals("Accesos")) && !App.session.admin()) continue;
      if (name.equals("Clientes") && App.session.barbero()) continue;
      MenuItem m = new MenuItem(name);
      m.setOnAction(e -> navigate(name));
      menu.getItems().add(m);
    }
    root.sceneProperty()
        .addListener((o, a, s) -> {
          if (s != null) {
            s.widthProperty().addListener((x, old, w) -> adapt(w.doubleValue()));
            adapt(s.getWidth());
          }
        });
    refresh();
  }

  private void adapt(double width) {
    boolean phone = width < 650, tablet = width < 1100;
    root.getStyleClass().removeAll("modo-telefono", "modo-tableta", "modo-escritorio");
    root.getStyleClass().add(phone ? "modo-telefono" : tablet ? "modo-tableta" : "modo-escritorio");
    root.setLeft(phone ? null : sidebar);
    menu.setVisible(phone);
    menu.setManaged(phone);
    filtros.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
    filtros.setPrefWrapLength(Math.max(320, width - 36));
    if (cards != null) {
      cards.setPrefColumns(phone ? 1 : tablet ? 2 : 4);
      for (var node : cards.getChildren())
        ((javafx.scene.layout.Region) node).setPrefWidth(phone ? width - 60 : 200);
    }
    if (table != null) {
      int shown = 0;
      for (var column : table.getColumns()) {
        boolean visible = !phone || (!column.getText().equals("id") && shown < 3);
        column.setVisible(visible);
        if (visible) shown++;
      }
      table.setColumnResizePolicy(
          phone
              ? TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS
              : TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
  }

  @FXML
  private void dashboard() {
    navigate("Dashboard");
  }

  @FXML
  private void clientes() {
    navigate("Clientes");
  }

  @FXML
  private void servicios() {
    navigate("Servicios");
  }

  @FXML
  private void citas() {
    navigate("Citas");
  }

  @FXML
  private void usuarios() {
    navigate("Usuarios");
  }

  @FXML
  private void accesos() {
    if (App.session.admin()) navigate("Accesos");
  }

  private void navigate(String name) {
    module = name;
    refresh();
  }

  @FXML
  private void refresh() {
    LocalDate f = desde.getValue(), t = hasta.getValue();
    try {
      BarberiaService.range(f, t);
    } catch (Exception ex) {
      mensaje.setText(ex.getMessage());
      return;
    }
    int current = ++generation;
    titulo.setText(module);
    contenido.getChildren().clear();
    cards = null;
    table = null;
    if (module.equals("Dashboard")) {
      App.work(
          () -> App.service.dashboard(App.session, f, t),
          d -> {
            if (current != generation) return;
            cards = new TilePane();
            cards.setHgap(12);
            cards.setVgap(12);
            for (int i = 0; i < 4; i++) {
              Label label = new Label(d.labels().get(i));
              label.setWrapText(true);
              Label value = new Label(d.values().get(i).toString());
              value.getStyleClass().add("kpi-value");
              VBox card = new VBox(8, label, value);
              card.getStyleClass().add("card");
              card.setPrefWidth(200);
              cards.getChildren().add(card);
            }
            BarChart<String, Number> bar = new BarChart<>(new CategoryAxis(), new NumberAxis());
            bar.setTitle("Servicios reservados");
            bar.setLegendVisible(false);
            var series = new XYChart.Series<String, Number>();
            d.servicios().forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
            bar.getData().add(series);
            bar.setPrefHeight(260);
            bar.setAnimated(false);
            LineChart<String, Number> line = new LineChart<>(new CategoryAxis(), new NumberAxis());
            line.setTitle("Citas por día");
            line.setLegendVisible(false);
            line.setAnimated(false);
            var ss = new XYChart.Series<String, Number>();
            d.dias().forEach((k, v) -> ss.getData().add(new XYChart.Data<>(k, v)));
            line.getData().add(ss);
            line.setPrefHeight(260);
            table = table(d.recientes());
            table.setPrefHeight(210);
            contenido
                .getChildren()
                .addAll(cards, bar, line, new Label("Actividad reciente"), table);
            adapt(root.getWidth());
          },
          mensaje);
    } else {
      String selected = module;
      App.work(
          () -> App.service.list(App.session, selected, f, t),
          rows -> {
            if (current != generation) return;
            table = table(rows);
            table.setPrefHeight(400);
            FlowPane actions = new FlowPane(8, 8);
            boolean editable =
                selected.equals("Clientes") && !App.session.barbero()
                    || selected.equals("Servicios") && App.session.admin()
                    || selected.equals("Citas") && !App.session.barbero()
                    || selected.equals("Usuarios") && App.session.admin();
            if (editable) {
              actions
                  .getChildren()
                  .addAll(
                      button("Crear", () -> edit(null)), button("Editar", () -> edit(selection())));
              if (selected.equals("Clientes") || selected.equals("Servicios"))
                actions.getChildren().add(button("Eliminar", this::delete));
            }
            if (selected.equals("Citas")) {
              actions.getChildren().add(button("Completar", () -> status(true)));
              if (!App.session.barbero())
                actions
                    .getChildren()
                    .addAll(
                        button("Cancelar cita", () -> status(false)),
                        button("Registrar pago", this::pay));
            }
            if (selected.equals("Usuarios"))
              actions.getChildren().add(button("Restablecer 2FA", this::reset));
            contenido.getChildren().addAll(actions, table);
            adapt(root.getWidth());
          },
          mensaje);
    }
  }

  private Button button(String text, Runnable action) {
    Button b = new Button(text);
    b.setOnAction(e -> {
      try {
        action.run();
      } catch (Exception ex) {
        mensaje.setText(App.friendly(ex));
      }
    });
    return b;
  }

  private TableView<Map<String, Object>> table(List<Map<String, Object>> rows) {
    TableView<Map<String, Object>> t = new TableView<>();
    t.setPlaceholder(new Label("Sin registros para este período."));
    List<String> cols =
        rows.isEmpty() ? List.of("id", "nombre") : new ArrayList<>(rows.getFirst().keySet());
    for (String key : cols) {
      if (key.endsWith("_id")) continue;
      TableColumn<Map<String, Object>, String> c = new TableColumn<>(key.replace('_', ' '));
      c.setCellValueFactory(
          v -> new SimpleStringProperty(Objects.toString(v.getValue().get(key), "")));
      c.setPrefWidth(key.equals("fecha") ? 180 : 145);
      t.getColumns().add(c);
    }
    t.getItems().setAll(rows);
    return t;
  }

  private Map<String, Object> selection() {
    var r = table.getSelectionModel().getSelectedItem();
    if (r == null) throw new IllegalArgumentException("Seleccione un registro en la tabla.");
    return r;
  }

  private void edit(Map<String, Object> row) {
    String current = module;
    App.work(
        () -> {
          Map<String, List<Option>> map = new HashMap<>();
          List<String> catalogs =
              switch (current) {
                case "Servicios" -> List.of("categorias");
                case "Citas" -> List.of("clientes", "empleados", "servicios");
                case "Usuarios" -> List.of("roles");
                default -> List.of();
              };
          for (String c : catalogs) map.put(c, App.service.options(App.session, c));
          return map;
        },
        cats -> {
          try {
            Editor.open(current, row, cats, this::refresh);
          } catch (Exception ex) {
            mensaje.setText(App.friendly(ex));
          }
        },
        mensaje);
  }

  private boolean confirm(String text) {
    Alert a = new Alert(Alert.AlertType.CONFIRMATION, text, ButtonType.YES, ButtonType.NO);
    a.initOwner(App.window);
    a.getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/vistas/styles.css").toExternalForm());
    a.setHeaderText("Confirmar operación");
    return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
  }

  private void delete() {
    long id = Store.id(selection(), "id");
    String current = module;
    if (confirm("¿Eliminar el registro seleccionado?"))
      App.work(
          () -> {
            App.service.delete(App.session, current, id);
            return true;
          },
          v -> refresh(),
          mensaje);
  }

  private void status(boolean complete) {
    long id = Store.id(selection(), "id");
    if (!complete && !confirm("¿Cancelar esta cita?")) return;
    App.work(
        () -> {
          App.service.status(App.session, id, complete);
          return true;
        },
        v -> refresh(),
        mensaje);
  }

  private void pay() {
    long id = Store.id(selection(), "id");
    if (confirm("¿Registrar el pago total y emitir el comprobante?"))
      App.work(
          () -> {
            App.service.pay(App.session, id);
            return true;
          },
          v -> refresh(),
          mensaje);
  }

  private void reset() {
    long id = Store.id(selection(), "id");
    if (confirm("El usuario deberá vincular de nuevo su Authenticator. ¿Continuar?"))
      App.work(
          () -> {
            App.service.reset2fa(App.session, id);
            return true;
          },
          v -> refresh(),
          mensaje);
  }

  @FXML
  private void report() {
    LocalDate f = desde.getValue(), t = hasta.getValue();
    FileChooser fc = new FileChooser();
    fc.setInitialFileName("Reporte_Barberia_" + LocalDate.now() + ".xlsx");
    fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
    var file = fc.showSaveDialog(App.window);
    if (file != null)
      App.work(
          () -> new ReportService().export(App.session, f, t, file.toPath()),
          p -> mensaje.setText("Reporte exportado: " + p.getFileName()),
          mensaje);
  }

  @FXML
  private void logout() {
    generation++;
    App.session = null;
    App.show("login");
  }
}
