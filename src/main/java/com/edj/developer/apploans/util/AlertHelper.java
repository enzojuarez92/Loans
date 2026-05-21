package com.edj.developer.apploans.util;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.Optional;

/**
 * AlertHelper — Fachada para diálogos y notificaciones toast.
 *
 * Centraliza la creación de Alert con CSS personalizado,
 * evitando duplicación en cada controlador.
 */
public final class AlertHelper {

    public enum ToastType { SUCCESS, ERROR, WARNING, INFO }

    private static final String CSS = "/css/style.css";
    private AlertHelper() {}

    /* ─── Diálogos ────────────────────────────────────────────────────── */

    public static void showError(String title, String header, String detail) {
        build(Alert.AlertType.ERROR, title, header, detail).showAndWait();
    }

    public static void showInfo(String title, String header, String detail) {
        build(Alert.AlertType.INFORMATION, title, header, detail).showAndWait();
    }

    public static void showWarning(String title, String header, String detail) {
        build(Alert.AlertType.WARNING, title, header, detail).showAndWait();
    }

    public static Optional<ButtonType> showConfirm(String title, String header, String detail) {
        Alert alert = build(Alert.AlertType.CONFIRMATION, title, header, detail);
        ButtonType yes = new ButtonType("Si, proceder", ButtonBar.ButtonData.OK_DONE);
        ButtonType no  = new ButtonType("Cancelar",       ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(yes, no);
        return alert.showAndWait();
    }

    /* ─── Toast notification ─────────────────────────────────────────── */

    public static void showToast(Scene scene, String message, ToastType type) {
        if (scene == null) return;

        javafx.application.Platform.runLater(() -> {
            String badgeStyle = switch (type) {
                case SUCCESS -> "badge-success";
                case ERROR   -> "badge-danger";
                case WARNING -> "badge-warning";
                case INFO    -> "badge-info";
            };

            Label label = new Label(message);
            label.getStyleClass().addAll("badge", badgeStyle);
            label.setStyle("-fx-padding: 10px 20px; -fx-font-size: 13px;");

            StackPane container = new StackPane(label);
            container.setAlignment(Pos.CENTER);
            container.setMouseTransparent(true);

            Popup popup = new Popup();
            popup.getContent().add(container);

            Window win = scene.getWindow();
            popup.show(win,
                    win.getX() + win.getWidth() / 2 - 110,
                    win.getY() + win.getHeight() - 85
            );

            FadeTransition fadeIn  = new FadeTransition(Duration.millis(180), container);
            fadeIn.setFromValue(0); fadeIn.setToValue(1);

            PauseTransition stay   = new PauseTransition(Duration.seconds(2.5));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(380), container);
            fadeOut.setFromValue(1); fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> popup.hide());

            new SequentialTransition(fadeIn, stay, fadeOut).play();
        });
    }

    /* ─── Privados ───────────────────────────────────────────────────── */

    private static Alert build(Alert.AlertType type, String title, String header, String detail) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(detail);
        alert.setResizable(false);
        try {
            var css = AlertHelper.class.getResource(CSS);
            if (css != null) {
                alert.getDialogPane().getStylesheets().add(css.toExternalForm());
                alert.getDialogPane().getStyleClass().add("dialog-pane");
            }
        } catch (Exception ignored) {}
        return alert;
    }
}