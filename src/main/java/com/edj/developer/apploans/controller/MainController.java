package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.config.DatabaseConfig;
import com.edj.developer.apploans.util.AlertHelper;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * MainController — Controlador del layout principal.
 *
 * Responsabilidades:
 * 1. Mostrar datos del usuario y marca del negocio en la sidebar (lectura dinámica de 'config' y 'users')
 * 2. Intercambiar sub-vistas en el StackPane central con animación
 * 3. Gestionar el estado activo (CSS "active") del ítem de menú seleccionado
 * 4. Implementar lazy-loading: cada vista se carga UNA SOLA VEZ y se cachea
 * 5. Manejar la salida y finalización del programa con confirmación
 */
public class MainController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    // ─── Identificadores de vistas (claves del caché) ─────────────────────
    private static final String VIEW_DASHBOARD      = "dashboard";
    private static final String VIEW_CLIENTES       = "clientes";
    private static final String VIEW_PRESTAMOS      = "prestamos";
    private static final String VIEW_REPORTES       = "reportes";
    private static final String VIEW_CONFIGURACION  = "configuracion";
    private static final String VIEW_PRODUCTOS      = "productos";
    private static final String VIEW_VENTAS_LIST    = "ventas";
    private static final String VIEW_VENTAS_FORM    = "sale_form";

    // ─── Rutas FXML ───────────────────────────────────────────────────────
    private static final Map<String, String> FXML_ROUTES = Map.of(
            VIEW_DASHBOARD,     "/fxml/DashboardView.fxml",
            VIEW_CLIENTES,      "/fxml/CustomerList.fxml",
            VIEW_PRESTAMOS,     "/fxml/LoanListView.fxml",
            VIEW_REPORTES,      "/fxml/ReportsView.fxml",
            VIEW_CONFIGURACION, "/fxml/SettingsView.fxml",
            VIEW_PRODUCTOS,     "/fxml/ProductListView.fxml",
            VIEW_VENTAS_LIST,   "/fxml/SaleListView.fxml",
            VIEW_VENTAS_FORM,   "/fxml/SaleFormView.fxml"
    );

    // ─── FXML – Sidebar ────────────────────────────────────────────────────
    @FXML private StackPane contentArea;

    @FXML private Label lblLogoText;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblUserInitials;

    @FXML private Button btnDashboard;
    @FXML private Button btnClientes;
    @FXML private Button btnPrestamos;
    @FXML private Button btnReportes;
    @FXML private Button btnConfiguracion;
    @FXML private Button btnProductos;
    @FXML private Button btnVentas; // Vinculado a fx:id="btnVentas" del FXML

    // ─── Estado interno ───────────────────────────────────────────────────
    private final Map<String, Parent> viewCache = new HashMap<>();
    private String currentView = null;
    private Button activeNavButton = null;
    private List<Button> navButtons;

    /* ═══════════════════════════════════════════════════════════════════
       INICIALIZACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        navButtons = List.of(
                btnDashboard, btnClientes, btnPrestamos, btnReportes, btnConfiguracion, btnProductos, btnVentas
        );

        Platform.runLater(() -> {
            if (contentArea.getScene() != null && contentArea.getScene().getRoot() != null) {
                contentArea.getScene().getRoot().setUserData(this);
            }
        });

        refreshBusinessAndUserInfo();
        navigateTo(VIEW_DASHBOARD, btnDashboard);
    }

    /**
     * Consulta las tablas 'config' y 'users' para actualizar los textos de la interfaz en caliente
     */
    public void refreshBusinessAndUserInfo() {
        String configSql = "SELECT business_name FROM config LIMIT 1";
        String userSql = "SELECT full_name, role, username FROM users WHERE id = 1";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(configSql)) {
                if (rs.next()) {
                    String name = rs.getString("business_name");
                    lblLogoText.setText((name != null && !name.trim().isEmpty()) ? name : "AppLoans");
                } else {
                    lblLogoText.setText("AppLoans");
                }
            }

            try (ResultSet rs = stmt.executeQuery(userSql)) {
                if (rs.next()) {
                    String fullName = rs.getString("full_name");
                    String username = rs.getString("username");
                    String role = rs.getString("role");

                    String displayName = (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;
                    lblUserName.setText(displayName);
                    lblUserRole.setText(translateRole(role));
                    lblUserInitials.setText(buildInitials(displayName));
                }
            }
            log.info("Sidebar UI sincronizada con las configuraciones actuales.");
        } catch (Exception e) {
            log.error("Error al refrescar información dinámica del Sidebar", e);
            lblLogoText.setText("AppLoans");
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       HANDLERS DE NAVEGACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    @FXML private void handleNavDashboard()     { navigateTo(VIEW_DASHBOARD,     btnDashboard);     }
    @FXML private void handleNavClientes()      { navigateTo(VIEW_CLIENTES,      btnClientes);      }
    @FXML private void handleNavPrestamos()     { navigateTo(VIEW_PRESTAMOS,     btnPrestamos);     }
    @FXML private void handleNavReportes()      { navigateTo(VIEW_REPORTES,      btnReportes);      }
    @FXML private void handleNavConfiguracion() { navigateTo(VIEW_CONFIGURACION, btnConfiguracion); }
    @FXML private void handleNavProductos()     { navigateTo(VIEW_PRODUCTOS,     btnProductos);     }
    @FXML private void handleNavVentas()        { navigateTo(VIEW_VENTAS_LIST,   btnVentas);        }

    /**
     * Maneja el clic en el botón de salida, solicitando confirmación antes de cerrar.
     */
    @FXML
    private void handleLogout() {
        Optional<ButtonType> result = AlertHelper.showConfirm(
                "Salir del Sistema",
                "¿Confirmás que querés cerrar la aplicación?",
                "Se guardarán todos tus cambios actuales."
        );

        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            log.info("Finalizando aplicación por solicitud del usuario.");
            Platform.exit();
            System.exit(0);
        }
    }

    /* ═══════════════════════════════════════════════════════════════════
       MOTOR DE NAVEGACIÓN
    ═══════════════════════════════════════════════════════════════════ */

    public void navigateTo(String viewKey, Button navBtn) {
        if (viewKey.equals(currentView)) return;

        try {
            Parent viewNode;

            if (viewCache.containsKey(viewKey)) {
                viewNode = viewCache.get(viewKey);
                log.debug("Vista '{}' cargada desde caché", viewKey);

                // ─── 💡 LA SOLUCIÓN DEFINITIVA: REFRESCO AUTOMÁTICO EN CACHÉ ───
                // Recuperamos el controlador real que JavaFX le pegó al nodo raíz cuando se creó
                Object controller = viewNode.getProperties().get("fx:controller");

                if (controller != null) {
                    // Caso A: Si es el Dashboard, mandamos a recargar las consultas SQL nativas
                    if (controller instanceof DashboardController dashboardCtrl) {
                        // Si renombraste tus métodos a públicos los llamás directo,
                        // o usamos este truco seguro por reflexión para ejecutar la carga en caliente:
                        try {
                            java.lang.reflect.Method method = dashboardCtrl.getClass().getDeclaredMethod("initialize");
                            method.setAccessible(true);
                            method.invoke(dashboardCtrl);
                            log.info("🔄 DashboardController refrescado automáticamente desde el caché del menú.");
                        } catch (Exception ex) {
                            log.error("No se pudo refrescar el Dashboard de manera automática", ex);
                        }
                    }

                    // Caso B: Si es la lista de préstamos (cuando volvés del detalle tras anular/finalizar)
                    else if (viewKey.equals(VIEW_PRESTAMOS)) {
                        try {
                            // Buscamos tu método handleRefresh de tu controlador de listado de préstamos
                            java.lang.reflect.Method method = controller.getClass().getMethod("handleRefresh");
                            method.invoke(controller);
                            log.info("🔄 LoanListController refrescado automáticamente al regresar.");
                        } catch (NoSuchMethodException e) {
                            // Si el método no es público o se llama distinto, probamos con initialize
                            try {
                                java.lang.reflect.Method initMethod = controller.getClass().getDeclaredMethod("initialize");
                                initMethod.setAccessible(true);
                                initMethod.invoke(controller);
                            } catch (Exception ex) {
                                log.error("No se pudo refrescar la lista de préstamos", ex);
                            }
                        }
                    }
                }

            } else {
                // Código original: Primera carga física del FXML
                String fxmlPath = FXML_ROUTES.get(viewKey);
                if (fxmlPath == null) {
                    viewNode = buildPlaceholder(viewKey);
                } else {
                    URL fxmlUrl = getClass().getResource(fxmlPath);
                    if (fxmlUrl == null) {
                        log.warn("FXML no encontrado: {}", fxmlPath);
                        viewNode = buildPlaceholder(viewKey);
                    } else {
                        FXMLLoader loader = new FXMLLoader(fxmlUrl);
                        viewNode = loader.load();

                        // 💡 IMPORTANTE: Guardamos la referencia del controlador en las propiedades del nodo
                        // para poder recuperarlo con seguridad cuando vuelva a usarse desde el caché
                        Object ctrl = loader.getController();
                        if (ctrl != null) {
                            viewNode.getProperties().put("fx:controller", ctrl);
                        }

                        log.info("Vista '{}' cargada desde FXML: {}", viewKey, fxmlPath);
                    }
                }

                viewCache.put(viewKey, viewNode);
                contentArea.getChildren().add(viewNode);
            }

            // Código original: Manejo de visibilidad y animaciones
            for (Node child : contentArea.getChildren()) {
                child.setVisible(false);
                child.setManaged(false);
            }

            viewNode.setVisible(true);
            viewNode.setManaged(true);

            FadeTransition ft = new FadeTransition(Duration.millis(180), viewNode);
            ft.setFromValue(0.3);
            ft.setToValue(1.0);
            ft.play();

            currentView = viewKey;

            if (navBtn != null) {
                setActiveNavButton(navBtn);
            } else if (VIEW_VENTAS_FORM.equals(viewKey)) {
                setActiveNavButton(btnVentas);
            }

        } catch (Exception e) {
            log.error("Error al navegar a la vista '{}'", viewKey, e);
            AlertHelper.showError(
                    "Error de Navegación",
                    "No se pudo cargar la sección solicitada",
                    "Detalle: " + e.getMessage()
            );
        }
    }

    private void setActiveNavButton(Button btn) {
        navButtons.forEach(b -> {
            if (b != null) b.getStyleClass().remove("active");
        });
        if (btn != null && !btn.getStyleClass().contains("active")) {
            btn.getStyleClass().add("active");
        }
        activeNavButton = btn;
    }

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
       HELPERS
    ═══════════════════════════════════════════════════════════════════ */

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

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