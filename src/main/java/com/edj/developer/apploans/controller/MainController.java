package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.model.User;
import com.edj.developer.apploans.util.AlertHelper;
import com.edj.developer.apploans.util.SessionManager;
import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * MainController — Controlador del layout principal.
 *
 * Responsabilidades:
 *  1. Mostrar datos del usuario en la sidebar (nombre, rol, iniciales)
 *  2. Intercambiar sub-vistas en el StackPane central con animación
 *  3. Gestionar el estado activo (CSS "active") del ítem de menú seleccionado
 *  4. Implementar lazy-loading: cada vista se carga UNA SOLA VEZ y se cachea
 *  5. Manejar el cierre de sesión con confirmación
 *
 * Patrón de navegación:
 *  - Las vistas se pre-cargan en un Map<String, Parent> (caché)
 *  - Al navegar, se hace visible la vista del caché y se ocultan las demás
 *  - FadeTransition de 200ms en cada cambio para transición suave
 */
public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // ─── Identificadores de vistas (claves del caché) ─────────────────────
    private static final String VIEW_DASHBOARD      = "dashboard";
    private static final String VIEW_CLIENTES       = "clientes";
    private static final String VIEW_PRESTAMOS      = "prestamos";
    private static final String VIEW_REPORTES         = "reportes";
    private static final String VIEW_CONFIGURACION  = "configuracion";

    // ─── Rutas FXML ───────────────────────────────────────────────────────
    private static final Map<String, String> FXML_ROUTES = Map.of(
            VIEW_DASHBOARD,     "/fxml/DashboardView.fxml",
            VIEW_CLIENTES,      "/fxml/CustomerList.fxml",
            VIEW_PRESTAMOS,     "/fxml/LoanListView.fxml",
            VIEW_REPORTES,        "/fxml/ReportsView.fxml",
            VIEW_CONFIGURACION, "/fxml/SettingsView.fxml"
    );

    // ─── FXML – Sidebar ────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblUserInitials;

    @FXML private Button btnDashboard;
    @FXML private Button btnClientes;
    @FXML private Button btnPrestamos;
    @FXML private Button btnReportes;
    @FXML private Button btnConfiguracion;

    // ─── Estado interno ───────────────────────────────────────────────────
    /** Caché de vistas ya cargadas: evita re-parsear FXML en cada navegación */
    private final Map<String, Parent> viewCache = new HashMap<>();

    /** Vista actualmente visible */
    private String currentView = null;

    /** Botón actualmente marcado como activo */
    private Button activeNavButton = null;

    /** Lista de todos los botones de navegación (para limpiar estado activo) */
    private List<Button> navButtons;

    /* ═══════════════════════════════════════════════════════════════════
       INICIALIZACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navButtons = List.of(
                btnDashboard, btnClientes, btnPrestamos, btnReportes, btnConfiguracion
        );

        loadUserInfo();
        navigateTo(VIEW_DASHBOARD, btnDashboard); // Vista inicial
    }

    /** Carga y muestra los datos del usuario de la sesión activa */
    private void loadUserInfo() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        lblUserName.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        lblUserRole.setText(translateRole(user.getRole()));
        lblUserInitials.setText(buildInitials(user.getFullName()));
    }

    /* ═══════════════════════════════════════════════════════════════════
       HANDLERS DE NAVEGACIÓN (llamados desde FXML)
    ═══════════════════════════════════════════════════════════════════ */

    @FXML private void handleNavDashboard()     { navigateTo(VIEW_DASHBOARD,     btnDashboard);     }
    @FXML private void handleNavClientes()      { navigateTo(VIEW_CLIENTES,      btnClientes);      }
    @FXML private void handleNavPrestamos()     { navigateTo(VIEW_PRESTAMOS,     btnPrestamos);     }
    @FXML private void handleNavReportes()        { navigateTo(VIEW_REPORTES,        btnReportes);        }
    @FXML private void handleNavConfiguracion() { navigateTo(VIEW_CONFIGURACION, btnConfiguracion); }

    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Cerrar Sesión",
                "¿Confirmás que querés cerrar sesión?",
                "Serás redirigido a la pantalla de inicio de sesión."
        );

        result.filter(bt -> bt == ButtonType.OK).ifPresent(bt -> performLogout());
    }

    /* ═══════════════════════════════════════════════════════════════════
       MOTOR DE NAVEGACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    /**
     * Cambia la vista activa en el StackPane central.
     *
     * Flujo:
     *  1. Si la vista ya está en caché → simplemente hacerla visible
     *  2. Si no está → cargar el FXML, añadirlo al StackPane y al caché
     *  3. Ocultar todas las demás vistas del StackPane
     *  4. Aplicar FadeTransition a la vista entrante
     *  5. Actualizar el estado CSS del ítem de menú
     *
     * @param viewKey  clave de la vista (VIEW_* constants)
     * @param navBtn   botón del menú que se debe marcar como activo
     */
    private void navigateTo(String viewKey, Button navBtn) {
        if (viewKey.equals(currentView)) return; // Ya estamos aquí

        try {
            Parent viewNode;

            if (viewCache.containsKey(viewKey)) {
                // ── Hit de caché: reusar vista ya cargada ──────────────────
                viewNode = viewCache.get(viewKey);
                log.debug("Vista '{}' cargada desde caché", viewKey);
            } else {
                // ── Miss de caché: cargar FXML por primera vez ─────────────
                String fxmlPath = FXML_ROUTES.get(viewKey);
                if (fxmlPath == null) {
                    // Vista aún no implementada → mostrar placeholder
                    viewNode = buildPlaceholder(viewKey);
                } else {
                    URL fxmlUrl = getClass().getResource(fxmlPath);
                    if (fxmlUrl == null) {
                        // Archivo FXML no encontrado → placeholder
                        log.warn("FXML no encontrado: {}", fxmlPath);
                        viewNode = buildPlaceholder(viewKey);
                    } else {
                        FXMLLoader loader = new FXMLLoader(fxmlUrl);
                        viewNode = loader.load();
                        log.info("Vista '{}' cargada desde FXML: {}", viewKey, fxmlPath);
                    }
                }

                // Registrar en caché y añadir al StackPane
                viewCache.put(viewKey, viewNode);
                contentArea.getChildren().add(viewNode);
            }

            // ── Ocultar todas las vistas del StackPane ─────────────────────
            for (Node child : contentArea.getChildren()) {
                child.setVisible(false);
                child.setManaged(false);
            }

            // ── Mostrar la vista seleccionada con fade-in ──────────────────
            viewNode.setVisible(true);
            viewNode.setManaged(true);

            FadeTransition ft = new FadeTransition(Duration.millis(180), viewNode);
            ft.setFromValue(0.3);
            ft.setToValue(1.0);
            ft.play();

            currentView = viewKey;

            // ── Actualizar estado activo del botón ─────────────────────────
            setActiveNavButton(navBtn);

        } catch (Exception e) {
            log.error("Error al navegar a la vista '{}'", viewKey, e);
            AlertHelper.showError(
                    "Error de Navegación",
                    "No se pudo cargar la sección solicitada",
                    e.getMessage()
            );
        }
    }

    /** Aplica/quita la clase CSS "active" en los botones del menú */
    private void setActiveNavButton(Button btn) {
        // Quitar "active" de todos
        navButtons.forEach(b -> b.getStyleClass().remove("active"));

        // Aplicar "active" al seleccionado
        if (btn != null && !btn.getStyleClass().contains("active")) {
            btn.getStyleClass().add("active");
        }

        activeNavButton = btn;
    }

    /**
     * Construye un nodo placeholder para vistas aún no implementadas.
     * Muestra un mensaje amigable en lugar de un error.
     */
    private Parent buildPlaceholder(String viewKey) {
        javafx.scene.layout.VBox placeholder = new javafx.scene.layout.VBox(16);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setStyle("-fx-background-color: #F8F9FA;");

        org.kordamp.ikonli.javafx.FontIcon icon =
                new org.kordamp.ikonli.javafx.FontIcon("fas-tools");
        icon.setIconSize(52);
        icon.setIconColor(javafx.scene.paint.Color.web("#CED4DA"));

        Label title = new Label("Módulo en Construcción");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ADB5BD;");

        Label subtitle = new Label("La sección «" + viewKey + "» estará disponible próximamente.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #ADB5BD;");

        placeholder.getChildren().addAll(icon, title, subtitle);
        return placeholder;
    }

    /* ═══════════════════════════════════════════════════════════════════
       CIERRE DE SESIÓN
    ═══════════════════════════════════════════════════════════════════ */

    private void performLogout() {
        log.info("Cerrando sesión: user='{}'",
                SessionManager.getInstance().getCurrentUser() != null
                        ? SessionManager.getInstance().getCurrentUser().getUsername() : "unknown");

        // Limpiar sesión
        SessionManager.getInstance().clearSession();

        // Limpiar caché de vistas
        viewCache.clear();

        // Volver al Login
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/LoginView.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/style.css").toExternalForm()
            );

            stage.setScene(scene);
            stage.setTitle("AppLoans — Iniciar Sesión");
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setWidth(860);
            stage.setHeight(540);
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            log.error("Error al volver al Login", e);
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       HELPERS
    ═══════════════════════════════════════════════════════════════════ */

    /** Construye las iniciales del nombre (máx 2 caracteres) */
    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /** Traduce el rol técnico al español para mostrar en la UI */
    private String translateRole(String role) {
        if (role == null) return "OPERADOR";
        return switch (role.toUpperCase()) {
            case "ADMIN"    -> "ADMINISTRADOR";
            case "OPERATOR" -> "OPERADOR";
            case "VIEWER"   -> "SOLO LECTURA";
            default         -> role.toUpperCase();
        };
    }
}