package edu.unev.barberia.controlador;

import edu.unev.barberia.App;
import edu.unev.barberia.service.PdfReport;
import java.util.*;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

public final class ReportPreview {
  public static void open(String title, List<Map<String, Object>> rows) {
    Dialog<Void> d = new Dialog<>();
    d.initOwner(App.window);
    d.setTitle(title);
    d.setResizable(true);
    d.getDialogPane()
        .getStylesheets()
        .add(ReportPreview.class.getResource("/vistas/styles.css").toExternalForm());
    TextArea preview = new TextArea(String.join("\n", PdfReport.lines(title, rows)));
    preview.setEditable(false);
    preview.setPrefSize(850, 550);
    Label status = new Label();
    Button pdf = new Button("Exportar PDF"), print = new Button("Imprimir");
    pdf.setOnAction(e -> {
      FileChooser fc = new FileChooser();
      fc.setInitialFileName("Barberia_" + title.replace(' ', '_') + ".pdf");
      fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
      var file = fc.showSaveDialog(App.window);
      if (file != null)
        App.work(
            () -> {
              PdfReport.write(file.toPath(), title, rows);
              return true;
            },
            v -> status.setText("PDF guardado correctamente."),
            status);
    });
    print.setOnAction(e -> {
      PrinterJob job = PrinterJob.createPrinterJob();
      if (job == null) {
        status.setText("No hay una impresora disponible.");
        return;
      }
      if (job.showPrintDialog(App.window)) {
        boolean success = true;
        var lines = PdfReport.lines(title, rows);
        for (int start = 0; start < lines.size(); start += 40) {
          VBox page = new VBox(4);
          page.setStyle("-fx-background-color: white; -fx-padding: 20;");
          for (String line : lines.subList(start, Math.min(start + 40, lines.size()))) {
            Label label = new Label(line);
            label.setStyle("-fx-text-fill: black; -fx-font-size: 9;");
            page.getChildren().add(label);
          }
          if (!job.printPage(page)) {
            success = false;
            break;
          }
        }
        if (success) job.endJob();
        else job.cancelJob();
        status.setText(success ? "Enviado a impresión." : "La impresión no se completó.");
      }
    });
    d.getDialogPane().setContent(new VBox(12, new HBox(12, pdf, print), preview, status));
    d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    d.showAndWait();
  }
}
