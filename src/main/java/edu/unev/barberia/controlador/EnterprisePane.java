package edu.unev.barberia.controlador;

import edu.unev.barberia.App;
import edu.unev.barberia.dao.Store;
import edu.unev.barberia.service.*;
import edu.unev.barberia.service.BarberiaService.Option;
import java.math.*;
import java.time.*;
import java.util.*;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

/** Módulos empresariales con paginación SQL y formularios modales accesibles. */
public final class EnterprisePane extends VBox {
  final EnterpriseService service = new EnterpriseService();
  final String module;
  final Label message = new Label();
  final TextField search = new TextField();
  final ComboBox<String> state = new ComboBox<>();
  final DatePicker from, to;
  final TableView<Map<String, Object>> table = new TableView<>();
  final Button next = new Button("Siguiente →"), prev = new Button("← Anterior");
  final Label pageLabel = new Label();
  int page = 0, request = 0;

  public EnterprisePane(String module, LocalDate start, LocalDate end) {
    super(16);
    this.module = module;
    from = new DatePicker(start);
    to = new DatePicker(end);
    Label intro =
        new Label(
            switch (module) {
              case "Inventario" -> "Productos, existencias disponibles y alertas de vencimiento.";
              case "Caja" -> "Apertura, arqueo y cierre. Solo efectivo afecta el dinero esperado.";
              case "Fidelización" ->
                  "Puntos y niveles. Cada RD$100 cobrados suma un punto; 100 puntos permiten un"
                      + " bono de RD$100.";
              case "Recordatorios" ->
                  "Seguimiento local de clientes, recurrencias y prioridades. No envía mensajes"
                      + " externos.";
              default ->
                  "Gestión de "
                      + module.toLowerCase()
                      + " · datos persistentes y operaciones auditadas.";
            });
    intro.setWrapText(true);
    intro.getStyleClass().add("muted");
    search.setPromptText("Buscar por nombre, código o referencia…");
    search.setPrefWidth(320);
    state
        .getItems()
        .setAll(
            switch (module) {
              case "Ventas" -> List.of("TODOS", "EMITIDA", "ANULADA");
              case "Recordatorios" -> List.of("TODOS", "PENDIENTE", "COMPLETADA", "POSPUESTA");
              case "Lotes" -> List.of("TODOS", "VIGENTE", "VENCIDO");
              default -> List.of("TODOS");
            });
    state.getSelectionModel().selectFirst();
    boolean dated =
        Set.of("Compras", "Ventas", "Caja", "Recordatorios", "Auditoría").contains(module);
    for (Node n : List.of(from, to)) {
      n.setVisible(dated);
      n.setManaged(dated);
    }
    state.setVisible(state.getItems().size() > 1);
    state.setManaged(state.isVisible());
    FlowPane filters = new FlowPane(10, 10, search, state, from, to);
    filters.getStyleClass().add("card");
    FlowPane actions = new FlowPane(10, 10);
    if (App.session.admin()) {
      if (module.equals("Inventario"))
        actions
            .getChildren()
            .addAll(
                action("＋ Nuevo producto", this::product),
                action(
                    "Editar producto",
                    () -> {
                      var r = table.getSelectionModel().getSelectedItem();
                      long id = selected();
                      form(
                          "Editar producto",
                          List.of(
                              field("Nombre", new TextField(Store.text(r, "nombre"))),
                              field("Categoría", new TextField(Store.text(r, "categoria"))),
                              field("Precio", new TextField(Store.text(r, "precio"))),
                              field("Mínimo", new TextField(Store.text(r, "minimo")))),
                          v -> {
                            service.editProduct(
                                App.session, id, v.text(0), v.text(1), v.decimal(2), v.number(3));
                            return null;
                          });
                    }),
                action(
                    "Desactivar",
                    () -> {
                      long id = selected();
                      Alert confirm =
                          new Alert(
                              Alert.AlertType.CONFIRMATION,
                              "¿Desactivar el producto conservando su historial?",
                              ButtonType.YES,
                              ButtonType.NO);
                      if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES)
                        App.work(
                            () -> {
                              service.deactivateProduct(App.session, id);
                              return true;
                            },
                            v -> load(),
                            message);
                    }));
      if (module.equals("Lotes"))
        actions
            .getChildren()
            .add(
                action(
                    "Ajustar existencia",
                    () -> {
                      long id = selected();
                      form(
                          "Ajuste auditado de inventario",
                          List.of(
                              field("Cantidad contada", new TextField()),
                              field("Motivo", new TextField())),
                          v -> {
                            service.adjust(App.session, id, v.number(0), v.text(1));
                            return null;
                          });
                    }));
      if (module.equals("Horarios"))
        actions
            .getChildren()
            .add(
                action(
                    "Configurar jornada",
                    () ->
                        catalogs(
                            c ->
                                form(
                                    "Horario de atención del barbero",
                                    List.of(
                                        field("Barbero", combo(c.get("empleados"))),
                                        field("Día (1=lunes, 7=domingo)", new TextField("1")),
                                        field("Inicio HH:mm", new TextField("08:00")),
                                        field("Fin HH:mm", new TextField("18:00"))),
                                    v -> {
                                      service.shift(
                                          App.session,
                                          v.option(0),
                                          v.number(1),
                                          LocalTime.parse(v.text(2)),
                                          LocalTime.parse(v.text(3)));
                                      return null;
                                    }),
                            "empleados")));
      if (module.equals("Proveedores"))
        actions.getChildren().add(action("＋ Nuevo proveedor", this::supplier));
      if (module.equals("Compras"))
        actions.getChildren().add(action("＋ Registrar compra", this::purchase));
      if (module.equals("Ventas"))
        actions
            .getChildren()
            .add(
                action(
                    "Anular venta",
                    () ->
                        form(
                            "Anulación autorizada",
                            List.of(field("Motivo", new TextField())),
                            v -> {
                              service.cancelSale(App.session, selected(), v.text(0));
                              return null;
                            })));
    }
    if (module.equals("Ventas")) {
      actions.getChildren().add(0, action("＋ Nueva venta", this::cart));
      actions
          .getChildren()
          .add(
              action(
                  "Ver factura / PDF",
                  () -> {
                    long id = selected();
                    App.work(
                        () -> service.invoice(App.session, id),
                        r -> ReportPreview.open("Factura de venta", r),
                        message);
                  }));
    }
    if (module.equals("Caja"))
      actions
          .getChildren()
          .addAll(
              action(
                  "Abrir caja",
                  () ->
                      form(
                          "Apertura de caja",
                          List.of(field("Fondo inicial", new TextField("0"))),
                          v -> service.open(App.session, v.decimal(0)))),
              action(
                  "Cerrar y arquear",
                  () ->
                      form(
                          "Cierre de caja",
                          List.of(field("Efectivo contado", new TextField())),
                          v -> service.close(App.session, v.decimal(0)))));
    if (module.equals("Recordatorios"))
      actions
          .getChildren()
          .addAll(
              action("＋ Recordatorio", this::reminder),
              action("Completar", () -> resolve(false)),
              action("Posponer un día", () -> resolve(true)));
    for (var col : List.of("id", "nombre")) {} // Las columnas se crean desde el resultado paginado.
    table.setPlaceholder(new Label("No hay registros para estos filtros."));
    table.setPrefHeight(450);
    table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    prev.setOnAction(e -> {
      if (page > 0) {
        page--;
        load();
      }
    });
    next.setOnAction(e -> {
      page++;
      load();
    });
    HBox pages = new HBox(14, prev, pageLabel, next);
    pages.setAlignment(Pos.CENTER_RIGHT);
    message.managedProperty().bind(message.textProperty().isNotEmpty());
    message.visibleProperty().bind(message.managedProperty());
    for (Node n : actions.getChildren())
      if (n instanceof Button b
          && Set.of(
                  "Editar producto",
                  "Desactivar",
                  "Ajustar existencia",
                  "Anular venta",
                  "Ver factura / PDF",
                  "Completar",
                  "Posponer un día")
              .contains(b.getText()))
        b.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
    getChildren().addAll(intro, filters, actions, message, table, pages);
    VBox.setVgrow(table, Priority.ALWAYS);
    PauseTransition debounce = new PauseTransition(Duration.millis(280));
    debounce.setOnFinished(e -> {
      page = 0;
      load();
    });
    search.textProperty().addListener((o, a, b) -> debounce.playFromStart());
    state
        .valueProperty()
        .addListener((o, a, b) -> {
          page = 0;
          load();
        });
    from.valueProperty()
        .addListener((o, a, b) -> {
          page = 0;
          load();
        });
    to.valueProperty()
        .addListener((o, a, b) -> {
          page = 0;
          load();
        });
    load();
  }

  void load() {
    int token = ++request;
    String q = search.getText(), st = state.getValue();
    LocalDate f = from.getValue(), t = to.getValue();
    int offset = page * 30;
    App.work(
        () -> service.page(App.session, module, q, st, f, t, offset),
        rows -> {
          if (token != request) return;
          table.getColumns().clear();
          for (String k : rows.isEmpty() ? List.of("id", "nombre") : rows.getFirst().keySet()) {
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(k.replace('_', ' '));
            col.setCellValueFactory(
                v -> new SimpleStringProperty(Objects.toString(v.getValue().get(k), "—")));
            col.setPrefWidth(k.equals("fecha") || k.equals("apertura") ? 180 : 140);
            table.getColumns().add(col);
          }
          table.getItems().setAll(rows);
          prev.setDisable(page == 0);
          next.setDisable(rows.size() < 30);
          pageLabel.setText("Página " + (page + 1) + " · " + rows.size() + " registros");
          if (module.equals("Recordatorios")) {
            long overdue =
                rows.stream()
                    .filter(r ->
                        Store.text(r, "estado").equals("PENDIENTE")
                            && date(r.get("fecha")).isBefore(LocalDateTime.now()))
                    .count();
            if (overdue > 0)
              message.setText(
                  "● " + overdue + " recordatorios pendientes vencidos en esta página.");
          }
        },
        message);
  }

  static LocalDateTime date(Object value) {
    return value instanceof LocalDateTime d ? d : ((java.sql.Timestamp) value).toLocalDateTime();
  }

  long selected() {
    var r = table.getSelectionModel().getSelectedItem();
    if (r == null) throw new IllegalArgumentException("Seleccione una fila.");
    return Store.id(r, "id");
  }

  Button action(String text, Runnable fn) {
    Button b = new Button(text);
    b.setOnAction(e -> {
      try {
        fn.run();
      } catch (Exception ex) {
        message.setText(App.friendly(ex));
      }
    });
    return b;
  }

  record Field(String title, Node input) {}

  Field field(String title, Node input) {
    return new Field(title, input);
  }

  record Values(List<Field> fields) {
    String text(int i) {
      Node n = fields.get(i).input();
      return n instanceof TextField t
          ? t.getText().trim()
          : Objects.toString(((ComboBox<?>) n).getValue(), "");
    }

    BigDecimal decimal(int i) {
      try {
        return new BigDecimal(text(i));
      } catch (Exception e) {
        throw new IllegalArgumentException("Use un importe numérico válido.");
      }
    }

    int number(int i) {
      try {
        return Integer.parseInt(text(i));
      } catch (Exception e) {
        throw new IllegalArgumentException("Use una cantidad entera.");
      }
    }

    long option(int i) {
      Object v = ((ComboBox<?>) fields.get(i).input()).getValue();
      if (!(v instanceof Option o))
        throw new IllegalArgumentException("Seleccione " + fields.get(i).title());
      return o.id();
    }

    LocalDate day(int i) {
      LocalDate d = ((DatePicker) fields.get(i).input()).getValue();
      if (d == null) throw new IllegalArgumentException("Seleccione una fecha.");
      return d;
    }
  }

  @FunctionalInterface
  interface Save {
    Object run(Values values) throws Exception;
  }

  void form(String title, List<Field> fields, Save save) {
    Dialog<Void> d = new Dialog<>();
    d.setTitle(title);
    d.initOwner(App.window);
    d.setResizable(true);
    d.getDialogPane()
        .getStylesheets()
        .add(getClass().getResource("/vistas/styles.css").toExternalForm());
    VBox box = new VBox(12);
    box.setPadding(new Insets(20));
    box.setPrefWidth(460);
    Label error = new Label();
    error.setWrapText(true);
    error.getStyleClass().add("message");
    for (Field f : fields) {
      Label label = new Label(f.title());
      label.setLabelFor(f.input());
      box.getChildren().addAll(label, f.input());
      if (f.input() instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
      if (f.input() instanceof TextField tf)
        tf.textProperty()
            .addListener((o, a, b) -> {
              tf.pseudoClassStateChanged(
                  javafx.css.PseudoClass.getPseudoClass("invalid"), b.isBlank());
            });
    }
    box.getChildren().add(error);
    d.getDialogPane().setContent(new ScrollPane(box));
    d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
    ok.setText("Guardar");
    ok.addEventFilter(
        javafx.event.ActionEvent.ACTION,
        e -> {
          e.consume();
          for (Field f : fields)
            if (f.input() instanceof TextField tf && tf.getText().isBlank()) {
              error.setText("Complete " + f.title() + ".");
              tf.requestFocus();
              return;
            }
          ok.setDisable(true);
          javafx.concurrent.Task<Object> task =
              new javafx.concurrent.Task<>() {
                protected Object call() throws Exception {
                  return save.run(new Values(fields));
                }
              };
          task.setOnSucceeded(done -> {
            d.close();
            load();
            message.setText("Operación guardada correctamente.");
          });
          task.setOnFailed(fail -> {
            ok.setDisable(false);
            error.setText(App.friendly(task.getException()));
          });
          Thread thread = new Thread(task, "v3-form");
          thread.setDaemon(true);
          thread.start();
        });
    d.showAndWait();
  }

  // Catálogos se cargan asincrónicamente; los controles se crean en el hilo JavaFX.
  void catalogs(java.util.function.Consumer<Map<String, List<Option>>> done, String... kinds) {
    App.work(
        () -> {
          Map<String, List<Option>> out = new HashMap<>();
          for (String k : kinds) out.put(k, service.catalog(App.session, k));
          return out;
        },
        done,
        message);
  }

  ComboBox<Option> combo(List<Option> values) {
    ComboBox<Option> c = new ComboBox<>();
    c.getItems().setAll(values);
    c.setPromptText("Seleccione…");
    return c;
  }

  ComboBox<String> choices(String... values) {
    ComboBox<String> c = new ComboBox<>();
    c.getItems().setAll(values);
    c.getSelectionModel().selectFirst();
    return c;
  }

  void product() {
    form(
        "Catálogo · Nuevo producto",
        List.of(
            field("Código único", new TextField()),
            field("Nombre", new TextField()),
            field("Categoría", new TextField()),
            field("Precio de venta", new TextField()),
            field("Stock mínimo", new TextField("3"))),
        v -> {
          service.product(App.session, v.text(0), v.text(1), v.text(2), v.decimal(3), v.number(4));
          return null;
        });
  }

  void supplier() {
    form(
        "Nuevo proveedor",
        List.of(field("Nombre", new TextField()), field("Teléfono", new TextField())),
        v -> {
          service.supplier(App.session, v.text(0), v.text(1));
          return null;
        });
  }

  void purchase() {
    catalogs(
        c ->
            form(
                "Compra y entrada de inventario",
                List.of(
                    field("Proveedor", combo(c.get("proveedores"))),
                    field("Producto", combo(c.get("productos"))),
                    field("Lote", new TextField()),
                    field("Vencimiento", new DatePicker(LocalDate.now().plusYears(1))),
                    field("Cantidad", new TextField("1")),
                    field("Costo unitario", new TextField())),
                v ->
                    service.purchase(
                        App.session,
                        v.option(0),
                        v.option(1),
                        v.text(2),
                        v.day(3),
                        v.number(4),
                        v.decimal(5))),
        "productos",
        "proveedores");
  }

  void reminder() {
    catalogs(
        c ->
            form(
                "Nuevo seguimiento de cliente",
                List.of(
                    field("Cliente", combo(c.get("clientes"))),
                    field("Actividad", new TextField()),
                    field("Categoría", new TextField("Seguimiento")),
                    field("Prioridad", choices("MEDIA", "ALTA", "BAJA")),
                    field("Fecha", new DatePicker(LocalDate.now().plusDays(1))),
                    field("Hora (HH:mm)", new TextField("09:00")),
                    field("Repetir cada días (0=no)", new TextField("0"))),
                v -> {
                  service.reminder(
                      App.session,
                      v.option(0),
                      v.text(1),
                      v.text(2),
                      v.text(3),
                      v.day(4).atTime(LocalTime.parse(v.text(5))),
                      v.number(6));
                  return null;
                }),
        "clientes");
  }

  void resolve(boolean postpone) {
    long id = selected();
    App.work(
        () -> {
          service.resolveReminder(App.session, id, postpone);
          return true;
        },
        v -> load(),
        message);
  }

  void cart() {
    catalogs(
        c -> {
          List<EnterpriseService.Line> lines = new ArrayList<>();
          Dialog<Void> d = new Dialog<>();
          d.initOwner(App.window);
          d.setTitle("Punto de venta · Barbería 3.0");
          d.setResizable(true);
          d.getDialogPane()
              .getStylesheets()
              .add(getClass().getResource("/vistas/styles.css").toExternalForm());
          ComboBox<Option> client = combo(c.get("clientes")), product = combo(c.get("productos"));
          Spinner<Integer> qty = new Spinner<>(1, 10000, 1);
          ListView<String> basket = new ListView<>();
          basket.setPrefHeight(160);
          TextField discount = new TextField("0"),
              tax = new TextField("0"),
              received = new TextField("0");
          ComboBox<String> method = choices("EFECTIVO", "TARJETA", "TRANSFERENCIA");
          CheckBox redeem = new CheckBox("Canjear 100 puntos por RD$100");
          Label error = new Label(), total = new Label("Subtotal: RD$0.00");
          error.setWrapText(true);
          Map<Long, BigDecimal> prices = new HashMap<>();
          App.work(() -> service.prices(App.session), prices::putAll, error);
          Runnable recalc =
              () -> {
                try {
                  BigDecimal sub = BigDecimal.ZERO;
                  for (var l : lines)
                    sub =
                        sub.add(prices.get(l.product()).multiply(BigDecimal.valueOf(l.quantity())));
                  BigDecimal disc =
                      new BigDecimal(discount.getText())
                          .add(redeem.isSelected() ? new BigDecimal("100") : BigDecimal.ZERO);
                  var t = EnterpriseService.totals(sub, disc, new BigDecimal(tax.getText()));
                  total.setText(
                      "Subtotal RD$"
                          + t.subtotal()
                          + "  ·  Descuento RD$"
                          + t.discount()
                          + "  ·  Impuesto RD$"
                          + t.tax()
                          + "  ·  TOTAL RD$"
                          + t.total());
                } catch (Exception ex) {
                  total.setText("Revise descuento e impuesto.");
                }
              };
          Button add =
              action(
                  "Añadir al carrito",
                  () -> {
                    Option p = product.getValue();
                    if (p == null || !prices.containsKey(p.id()))
                      throw new IllegalArgumentException("Seleccione un producto disponible.");
                    lines.add(new EnterpriseService.Line(p.id(), qty.getValue()));
                    basket.getItems().add(p.nombre() + " × " + qty.getValue());
                    recalc.run();
                  });
          Button remove =
              action(
                  "Quitar línea",
                  () -> {
                    int i = basket.getSelectionModel().getSelectedIndex();
                    if (i >= 0) {
                      lines.remove(i);
                      basket.getItems().remove(i);
                      recalc.run();
                    }
                  });
          discount.textProperty().addListener((o, a, b) -> recalc.run());
          tax.textProperty().addListener((o, a, b) -> recalc.run());
          redeem.selectedProperty().addListener((o, a, b) -> recalc.run());
          GridPane grid = new GridPane();
          grid.setHgap(16);
          grid.setVgap(12);
          int row = 0;
          for (var f :
              List.of(
                  field("Cliente", client),
                  field("Producto", product),
                  field("Cantidad", qty),
                  field("Descuento RD$", discount),
                  field("Impuesto % (según corresponda)", tax),
                  field("Método de pago", method),
                  field("Efectivo recibido RD$", received))) {
            grid.add(new Label(f.title()), 0, row);
            grid.add(f.input(), 1, row++);
          }
          VBox box =
              new VBox(
                  14,
                  new Label("VENTA · Productos y lotes por vencer primero"),
                  grid,
                  new HBox(10, add, remove),
                  basket,
                  redeem,
                  total,
                  error);
          box.setPadding(new Insets(20));
          box.setPrefWidth(740);
          total.setWrapText(true);
          d.getDialogPane().setContent(new ScrollPane(box));
          d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
          Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
          ok.setText("Cobrar y emitir factura");
          ok.addEventFilter(
              javafx.event.ActionEvent.ACTION,
              e -> {
                e.consume();
                try {
                  if (client.getValue() == null)
                    throw new IllegalArgumentException("Seleccione cliente.");
                  long id = client.getValue().id();
                  BigDecimal dis = new BigDecimal(discount.getText()),
                      rate = new BigDecimal(tax.getText()),
                      rec = new BigDecimal(received.getText());
                  String met = method.getValue();
                  boolean red = redeem.isSelected();
                  List<EnterpriseService.Line> copy = List.copyOf(lines);
                  ok.setDisable(true);
                  javafx.concurrent.Task<Long> task =
                      new javafx.concurrent.Task<>() {
                        protected Long call() throws Exception {
                          return service.sell(App.session, id, copy, dis, rate, met, rec, red);
                        }
                      };
                  task.setOnSucceeded(done -> {
                    d.close();
                    load();
                    message.setText("Venta emitida: " + task.getValue());
                  });
                  task.setOnFailed(fail -> {
                    ok.setDisable(false);
                    error.setText(App.friendly(task.getException()));
                  });
                  new Thread(task, "v3-sale").start();
                } catch (Exception ex) {
                  error.setText(App.friendly(ex));
                }
              });
          d.showAndWait();
        },
        "clientes",
        "productos");
  }
}
