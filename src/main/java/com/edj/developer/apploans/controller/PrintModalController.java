package com.edj.developer.apploans.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PrintModalController {

    private final BorderPane root;
    private final VBox a4PaperSheet;

    public PrintModalController(javafx.scene.layout.Region reportGrid, String reportTitle) {
        root = new BorderPane();
        root.setStyle("-fx-background-color: #525659;");

        // 1. Barra de Herramientas Superior
        HBox toolbar = new HBox(15);
        toolbar.setStyle("-fx-background-color: #323639; -fx-padding: 10 20;");
        toolbar.setAlignment(Pos.CENTER_RIGHT);

        Button btnPrint = new Button("Imprimir Planilla");
        btnPrint.setStyle("-fx-background-color: #198754; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 14; -fx-cursor: hand;");

        Button btnClose = new Button("Cerrar");
        btnClose.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #212529; -fx-padding: 6 14; -fx-cursor: hand;");

        toolbar.getChildren().addAll(btnPrint, btnClose);
        root.setTop(toolbar);

        // 2. Lienzo de la Hoja A4
        a4PaperSheet = new VBox(25); // Espaciado vertical armónico
        a4PaperSheet.setStyle("-fx-background-color: #ffffff; -fx-padding: 45; -fx-min-width: 820px; -fx-max-width: 820px; " +
                "-fx-min-height: 1120px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 5);");
        a4PaperSheet.setAlignment(Pos.TOP_CENTER);

        // Cabecera Oficial de AppLoans
        VBox docHeader = new VBox(5);
        docHeader.setStyle("-fx-border-color: #007f5f; -fx-border-width: 0 0 3 0; -fx-padding: 0 0 12 0;");

        Label title = new Label(reportTitle.toUpperCase());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #212529; -fx-letter-spacing: 1px;");

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        Label subtitle = new Label("AppLoans — Sistema de Gestión de Créditos  |  Documento Emitido: " + now);
        subtitle.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        docHeader.getChildren().addAll(title, subtitle);

        // Unimos ÚNICAMENTE la cabecera limpia y la grilla de datos (Eliminado el Banner Verde)
        a4PaperSheet.getChildren().addAll(docHeader, reportGrid);

        // Contenedor intermedio para centrar la hoja
        StackPane centerWrapper = new StackPane(a4PaperSheet);
        centerWrapper.setPadding(new Insets(20));
        centerWrapper.setStyle("-fx-background-color: #525659;");
        centerWrapper.setAlignment(Pos.CENTER);

        // ScrollPane de control
        ScrollPane scrollPane = new ScrollPane(centerWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: #525659; -fx-background: #525659;");

        root.setCenter(scrollPane);

        // ─── EVENTOS ───────────────────────────────────────────────────────
        btnClose.setOnAction(e -> ((Stage) btnClose.getScene().getWindow()).close());

        btnPrint.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(btnPrint.getScene().getWindow())) {
                javafx.print.Printer printer = job.getPrinter();

                javafx.print.PageLayout pageLayout = printer.createPageLayout(
                        javafx.print.Paper.A4,
                        javafx.print.PageOrientation.PORTRAIT,
                        14.17, 14.17, 14.17, 14.17
                );
                job.getJobSettings().setPageLayout(pageLayout);

                // 1. Calculamos la escala
                double printableWidth = pageLayout.getPrintableWidth();
                double reportWidth = a4PaperSheet.getBoundsInLocal().getWidth();

                double scale = 1.0;
                if (reportWidth > printableWidth) {
                    scale = printableWidth / reportWidth;
                }

                javafx.scene.transform.Scale scaleTransform = new javafx.scene.transform.Scale(scale, scale);

                // 2. 💡 EL TRUCO: Guardamos el efecto actual y lo removemos para la impresión
                javafx.scene.effect.Effect originalEffect = a4PaperSheet.getEffect();
                a4PaperSheet.setEffect(null);

                // 3. Aplicamos la escala
                a4PaperSheet.getTransforms().add(scaleTransform);

                // 4. Imprimimos (ahora la hoja va a salir blanca y limpia, sin sombras raras)
                boolean success = job.printPage(a4PaperSheet);

                // 5. Restauramos todo para que en la pantalla del usuario siga viéndose de diez
                a4PaperSheet.getTransforms().remove(scaleTransform);
                a4PaperSheet.setEffect(originalEffect); // <-- Le devolvemos su sombra flotante

                if (success) {
                    job.endJob();
                }
            }
        });
    }

    public BorderPane getRootView() { return root; }
}