
// ═════════════════════════════════════════════════════════════════════════════
// ARCHIVO 3: src/main/java/com/edj/developer/apploans/Main.java  (ACTUALIZADO)
// Ahora arranca en LoginView en lugar de ir directo a la app.
// ═════════════════════════════════════════════════════════════════════════════
package com.edj.developer.apploans;

import com.edj.developer.apploans.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Inicializar base de datos (tablas + seed admin)
        DatabaseConfig.initDatabase();

        // 2. Cargar pantalla de Login como punto de entrada
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/LoginView.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 860, 540);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm()
        );

        primaryStage.setTitle("AppLoans — Iniciar Sesión");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.centerOnScreen();
        primaryStage.show();

        log.info("AppLoans iniciado. Esperando autenticación...");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
