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
  private int page = 0;
  private String query = "", state = "TODOS";
  private int generation;
  private javafx.scene.Node navigation;

  @FXML
  private void initialize() {
    mensaje.managedProperty().bind(mensaje.textProperty().isNotEmpty());
    mensaje.visibleProperty().bind(mensaje.managedProperty());
    navigation = root.getLeft();
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
    if (!App.session.barbero()) {
      Label section = new Label("OPERACIONES 3.0");
      section.getStyleClass().add("nav-section");
      sidebar.getChildren().add(section);
      for (String name :
          List.of(
              "Inventario",
              "Lotes",
              "Proveedores",
              "Compras",
              "Ventas",
              "Caja",
              "Fidelización",
              "Recordatorios",
              "Reportes",
              "Equipo",
              "Horarios",
              "Auditoría")) {
        if (!App.session.admin()
            && Set.of("Proveedores", "Compras", "Auditoría", "Equipo", "Horarios").contains(name))
          continue;
        Button nav = button(name, () -> navigate(name));
        nav.setMaxWidth(Double.MAX_VALUE);
        sidebar.getChildren().add(nav);
        MenuItem item = new MenuItem(name);
        item.setOnAction(e -> navigate(name));
        menu.getItems().add(item);
      }
    }
    for (var node : sidebar.getChildren())
      if (node instanceof Button b) {
        javafx.scene.shape.SVGPath icon = new javafx.scene.shape.SVGPath();
        icon.setContent(
            switch (b.getText()) {
              case "Dashboard" ->
                  "M1 1 H7 V7 H1 Z M11 1 H17 V7 H11 Z M1 11 H7 V17 H1 Z M11 11 H17 V17 H11 Z";
              case "Clientes", "Usuarios", "Equipo" ->
                  "M5 5 A4 4 0 1 0 13 5 A4 4 0 1 0 5 5 M1 18 C1 10 17 10 17 18";
              case "Citas", "Recordatorios", "Horarios" ->
                  "M2 3 H16 V17 H2 Z M2 7 H16 M6 1 V5 M12 1 V5";
              case "Ventas", "Caja", "Compras" -> "M2 4 H16 V16 H2 Z M2 8 H16 M11 12 H15";
              default -> "M2 2 H16 V17 H2 Z M5 6 H13 M5 10 H13 M5 14 H10";
            });
        icon.getStyleClass().add("nav-icon");
        b.setGraphic(icon);
        b.setGraphicTextGap(12);
      }
    refresh();
  }

  private void adapt(double width) {
    boolean phone = width < 650, tablet = width < 1100;
    root.getStyleClass().removeAll("modo-telefono", "modo-tableta", "modo-escritorio");
    root.getStyleClass().add(phone ? "modo-telefono" : tablet ? "modo-tableta" : "modo-escritorio");
    root.setLeft(phone ? null : navigation);
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
    page = 0;
    query = "";
    state = "TODOS";
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
    boolean standard =
        Set.of("Dashboard", "Clientes", "Servicios", "Citas", "Usuarios", "Accesos", "Reportes")
            .contains(module);
    filtros.setVisible(standard);
    filtros.setManaged(standard);
    contenido.getChildren().clear();
    for (var node : sidebar.getChildren())
      if (node instanceof Button b) {
        b.getStyleClass().remove("nav-active");
        if (b.getText().equals(module)) b.getStyleClass().add("nav-active");
      }
    if (Set.of(
            "Inventario",
            "Lotes",
            "Proveedores",
            "Compras",
            "Ventas",
            "Caja",
            "Fidelización",
            "Recordatorios",
            "Equipo",
            "Horarios",
            "Auditoría")
        .contains(module)) {
      contenido.getChildren().add(new EnterprisePane(module, f, t));
      return;
    }
    if (module.equals("Reportes")) {
      Label hint = new Label("Informes desde datos persistentes · vista previa, impresión y PDF");
      hint.setWrapText(true);
      contenido.getChildren().add(hint);
      for (String kind : List.of("Ventas", "Inventario", "Caja", "Clientes"))
        contenido
            .getChildren()
            .add(
                button(
                    "Ver reporte de " + kind,
                    () ->
                        App.work(
                            () -> new EnterpriseService().report(App.session, kind, f, t),
                            rows -> ReportPreview.open("Reporte de " + kind, rows),
                            mensaje)));
      contenido
          .getChildren()
          .add(
              button(
                  "Reporte de citas",
                  () ->
                      App.work(
                          () -> App.service.report(App.session, f, t),
                          rows -> ReportPreview.open("Citas", rows),
                          mensaje)));
      return;
    }
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
            if (!App.session.barbero()) {
              FlowPane quick = new FlowPane(10, 10);
              quick
                  .getChildren()
                  .addAll(
                      button("＋ Agendar cita", () -> navigate("Citas")),
                      button("Punto de venta", () -> navigate("Ventas")),
                      button("Caja", () -> navigate("Caja")));
              DatePicker calendar = new DatePicker(LocalDate.now());
              calendar.setPromptText("Consultar agenda diaria");
              calendar
                  .valueProperty()
                  .addListener((o, a, b) -> {
                    if (b != null) {
                      desde.setValue(b);
                      hasta.setValue(b);
                      navigate("Citas");
                    }
                  });
              quick.getChildren().add(calendar);
              contenido.getChildren().add(1, quick);
              Label alerts = new Label("Cargando alertas operativas…");
              alerts.setWrapText(true);
              alerts.getStyleClass().add("message");
              contenido.getChildren().add(2, alerts);
              App.work(
                  () -> new EnterpriseService().summary(App.session),
                  summary -> {
                    if (current == generation)
                      alerts.setText(
                          String.join(
                              "    •    ",
                              summary.entrySet().stream()
                                  .map(e -> e.getKey() + ": " + e.getValue())
                                  .toList()));
                  },
                  alerts);
            }
            adapt(root.getWidth());
          },
          mensaje);
    } else {
      String selected = module;
      App.work(
          () -> App.service.page(App.session, selected, query, state, f, t, page * 30),
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
                actions.getChildren().add(button("Desactivar", this::delete));
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
            TextField search = new TextField(query);
            search.setPromptText("Buscar mientras escribe…");
            ComboBox<String> statusFilter = new ComboBox<>();
            statusFilter.getItems().setAll("TODOS", "PENDIENTE", "COMPLETADA", "CANCELADA");
            statusFilter.setValue(state);
            statusFilter.setVisible(selected.equals("Citas"));
            statusFilter.setManaged(selected.equals("Citas"));
            javafx.animation.PauseTransition pause =
                new javafx.animation.PauseTransition(javafx.util.Duration.millis(350));
            pause.setOnFinished(e -> {
              query = search.getText();
              page = 0;
              refresh();
            });
            search.textProperty().addListener((o, a, b) -> pause.playFromStart());
            statusFilter
                .valueProperty()
                .addListener((o, a, b) -> {
                  state = b;
                  page = 0;
                  refresh();
                });
            Button
                previous =
                    button(
                        "← Anterior",
                        () -> {
                          page--;
                          refresh();
                        }),
                next =
                    button(
                        "Siguiente →",
                        () -> {
                          page++;
                          refresh();
                        });
            previous.setDisable(page == 0);
            next.setDisable(rows.size() < 30);
            contenido
                .getChildren()
                .addAll(
                    new HBox(10, search, statusFilter),
                    actions,
                    table,
                    new HBox(12, previous, new Label("Página " + (page + 1)), next));
            adapt(root.getWidth());
          },
          mensaje);
    }
  }

  private Button button(String text, Runnable action) {
    Button b = new Button(text);
    if (table != null
        && Set.of(
                "Editar",
                "Desactivar",
                "Completar",
                "Cancelar cita",
                "Registrar pago",
                "Restablecer 2FA")
            .contains(text))
      b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
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
    TableColumn<Map<String, Object>, Void> detail = new TableColumn<>("Acción");
    detail.setPrefWidth(100);
    detail.setSortable(false);
    detail.setCellFactory(col ->
        new TableCell<>() {
          final Button view = new Button("Ver");

          {
            view.setOnAction(e -> {
              Map<String, Object> row = getTableView().getItems().get(getIndex());
              ReportPreview.open("Detalle de " + module, List.of(row));
            });
          }

          protected void updateItem(Void v, boolean empty) {
            super.updateItem(v, empty);
            setGraphic(empty ? null : view);
          }
        });
    t.getColumns().add(detail);
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
    if (confirm("¿Desactivar este registro conservando su historial?"))
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
