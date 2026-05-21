package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.model.User;
import com.edj.developer.apploans.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * LoginController
 *
 * Flujo de autenticación:
 *  1. Validar campos no vacíos (inline)
 *  2. Consultar tabla `users` con username + password
 *  3. Verificar que user.active = 1
 *  4. Guardar usuario en SessionManager (Singleton)
 *  5. Cargar MainView.fxml y reemplazar la escena
 *
 * Seguridad:
 *  - En producción, reemplazar comparación directa por BCrypt.verify()
 *  - PreparedStatement previene SQL Injection
 *  - Intentos fallidos se loguean con SLF4J
 */
public class LoginController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private static final String SQL_LOGIN = """
        SELECT id, username, full_name, role, active
        FROM users
        WHERE username = ? AND password = ? AND active = 1
        LIMIT 1
        """;

    private static final int MAX_ATTEMPTS = 5;
    private int failedAttempts = 0;

    // ─── FXML ─────────────────────────────────────────────────────────────
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button        btnLogin;
    @FXML private FontIcon      loginIcon;

    @FXML private HBox    pnlError;
    @FXML private Label   lblError;
    @FXML private Region  errorSpacer;

    @FXML private Label errUsername;
    @FXML private Label errPassword;

    /* ═══════════════════════════════════════════════════════════════════
       INICIALIZACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Focus automático en el campo usuario al abrir
        Platform.runLater(() -> txtUsername.requestFocus());

        // Limpiar errores al escribir
        txtUsername.textProperty().addListener((obs, o, n) -> clearFieldError(errUsername, txtUsername));
        txtPassword.textProperty().addListener((obs, o, n) -> clearFieldError(errPassword, txtPassword));

        // Animación de entrada
        playFadeIn();
    }

    /* ═══════════════════════════════════════════════════════════════════
       HANDLER PRINCIPAL
    ═══════════════════════════════════════════════════════════════════ */

    @FXML
    private void handleLogin() {
        hideError();

        // ── Paso 1: Validar campos ────────────────────────────────────────
        if (!validateFields()) return;

        // ── Paso 2: Verificar bloqueo por intentos ────────────────────────
        if (failedAttempts >= MAX_ATTEMPTS) {
            showError("Cuenta bloqueada por demasiados intentos fallidos. Contactá al administrador.");
            btnLogin.setDisable(true);
            return;
        }

        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        // ── Paso 3: Autenticar contra la BD ──────────────────────────────
        setBusyState(true);

        // Ejecutar en hilo separado para no bloquear la UI
        Thread authThread = new Thread(() -> {
            User authenticatedUser = authenticate(username, password);
            Platform.runLater(() -> {
                setBusyState(false);
                if (authenticatedUser != null) {
                    onLoginSuccess(authenticatedUser);
                } else {
                    onLoginFailure();
                }
            });
        });
        authThread.setDaemon(true);
        authThread.start();
    }

    /* ═══════════════════════════════════════════════════════════════════
       AUTENTICACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    /**
     * Consulta la BD y retorna el User si las credenciales son válidas.
     * Retorna null si no existe o está inactivo.
     *
     * NOTA: En producción usar BCrypt.checkpw(password, storedHash)
     */
    private User authenticate(String username, String password) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LOGIN)) {

            ps.setString(1, username);
            ps.setString(2, password); // TODO: reemplazar con BCrypt en producción

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setFullName(rs.getString("full_name"));
                    user.setRole(rs.getString("role"));
                    user.setActive(rs.getInt("active") == 1);
                    return user;
                }
            }

        } catch (SQLException e) {
            log.error("Database error during authentication", e);
        }

        return null;
    }

    /* ═══════════════════════════════════════════════════════════════════
       CALLBACKS DE RESULTADO
    ═══════════════════════════════════════════════════════════════════ */

    private void onLoginSuccess(User user) {
        log.info("Login exitoso: user='{}', role='{}'", user.getUsername(), user.getRole());
        failedAttempts = 0;

        // Guardar sesión activa
        SessionManager.getInstance().setCurrentUser(user);

        // Cargar la vista principal
        loadMainView();
    }

    private void onLoginFailure() {
        failedAttempts++;
        log.warn("Login fallido. Intento {} de {}", failedAttempts, MAX_ATTEMPTS);

        int remaining = MAX_ATTEMPTS - failedAttempts;

        if (remaining > 0) {
            showError("Usuario o contraseña incorrectos. Intentos restantes: " + remaining);
        } else {
            showError("Demasiados intentos fallidos. Cuenta bloqueada.");
            btnLogin.setDisable(true);
        }

        // Shake animation en el formulario
        playShakeAnimation();

        // Limpiar solo la contraseña
        txtPassword.clear();
        txtPassword.requestFocus();
    }

    /* ═══════════════════════════════════════════════════════════════════
       NAVEGACIÓN — Cargar MainView
    ═══════════════════════════════════════════════════════════════════ */

    private void loadMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/MainView.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("AppLoans — Sistema de Gestión de Préstamos");
            stage.setResizable(true);
            stage.setMaximized(true);
            stage.setMinWidth(1000);
            stage.setMinHeight(750);
            stage.show();

        } catch (Exception e) {
            log.error("Error al cargar MainView", e);
            showError("Error interno: no se pudo cargar la aplicación.");
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       VALIDACIÓN DE CAMPOS
    ═══════════════════════════════════════════════════════════════════ */

    private boolean validateFields() {
        boolean valid = true;

        if (txtUsername.getText().isBlank()) {
            showFieldError(errUsername, txtUsername, "El usuario es obligatorio");
            valid = false;
        }

        if (txtPassword.getText().isBlank()) {
            showFieldError(errPassword, txtPassword, "La contraseña es obligatoria");
            valid = false;
        }

        return valid;
    }

    /* ═══════════════════════════════════════════════════════════════════
       HELPERS DE UI
    ═══════════════════════════════════════════════════════════════════ */

    private void showError(String message) {
        lblError.setText(message);
        pnlError.setVisible(true);
        pnlError.setManaged(true);
        errorSpacer.setPrefHeight(12);
        errorSpacer.setManaged(true);

        // Fade-in del panel de error
        FadeTransition ft = new FadeTransition(Duration.millis(250), pnlError);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void hideError() {
        pnlError.setVisible(false);
        pnlError.setManaged(false);
        errorSpacer.setPrefHeight(0);
        errorSpacer.setManaged(false);
    }

    private void showFieldError(Label errorLabel, javafx.scene.Node field, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        field.getStyleClass().remove("form-control-error");
        field.getStyleClass().add("form-control-error");
    }

    private void clearFieldError(Label errorLabel, javafx.scene.Node field) {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        field.getStyleClass().remove("form-control-error");
        hideError();
    }

    private void setBusyState(boolean busy) {
        btnLogin.setDisable(busy);
        loginIcon.setIconLiteral(busy ? "fas-spinner" : "fas-sign-in-alt");
        btnLogin.setText(busy ? "Verificando..." : "Ingresar al Sistema");
    }

    /* ═══════════════════════════════════════════════════════════════════
       ANIMACIONES
    ═══════════════════════════════════════════════════════════════════ */

    private void playFadeIn() {
        FadeTransition ft = new FadeTransition(Duration.millis(500), btnLogin.getParent());
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /** Sacude horizontalmente el botón para indicar error */
    private void playShakeAnimation() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), pnlError);
        shake.setFromX(0);
        shake.setByX(10);
        shake.setCycleCount(6);
        shake.setAutoReverse(true);
        shake.play();
    }
}
