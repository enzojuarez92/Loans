package com.edj.developer.apploans.util;

import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportManager {

    public static void launchReport(String reportTitle, List<?> rawData, String codeType) {
        try {
            // 1. Mapeo dinámico de propiedades
            List<Map<String, ?>> mappedData = new ArrayList<>();

            for (Object item : rawData) {
                Map<String, Object> map = new HashMap<>();
                if (item instanceof DailyReportItem) {
                    DailyReportItem daily = (DailyReportItem) item;
                    map.put("id", daily.getLoanId());
                    map.put("name", daily.getCustomerName());
                    map.put("value", daily.getAmount());
                } else if (item instanceof GeneralReportItem) {
                    GeneralReportItem general = (GeneralReportItem) item;
                    map.put("id", general.getLoanId());
                    map.put("name", general.getCustomerName());
                    map.put("value", general.getOutstandingBalance());
                }
                mappedData.add(map);
            }

            if (mappedData.isEmpty()) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("id", 0);
                emptyMap.put("name", "No se encontraron registros activos.");
                emptyMap.put("value", 0.0);
                mappedData.add(emptyMap);
            }

            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(mappedData);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", reportTitle.toUpperCase());
            parameters.put("CODE_TYPE", codeType);

            InputStream reportStream = ReportManager.class.getResourceAsStream("/reports/CollectionTwoColumns.jrxml");
            if (reportStream == null) {
                System.err.println("ERROR: Could not find CollectionTwoColumns.jrxml in resources/reports/");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 🌟 INTENTO PREMIUM: Exporta a PDF y delega la impresión al S.O.
            try {
                File tempFile = File.createTempFile("Planilla_" + codeType + "_", ".pdf");
                tempFile.deleteOnExit();
                JasperExportManager.exportReportToPdfFile(jasperPrint, tempFile.getAbsolutePath());

                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(tempFile);
                } else {
                    mostrarVisorInternoJasper(jasperPrint, reportTitle);
                }
            } catch (Exception ex) {
                // 🚀 SALVADA: Entra acá si el entorno no soporta la acción o no posee lector de PDF
                System.err.println("Aviso: No se pudo abrir el PDF nativo, usando visor Jasper. " + ex.getMessage());
                mostrarVisorInternoJasper(jasperPrint, reportTitle);
            }

        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    public static void launchSaleStatement(com.edj.developer.apploans.model.Sale sale, String formattedPaid, String formattedPending) {
        try {
            List<Map<String, ?>> mappedData = new ArrayList<>();
            for (com.edj.developer.apploans.model.SaleReceipt receipt : sale.getReceipts()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", receipt.getId());
                map.put("paymentDate", receipt.getPaymentDate());
                map.put("amount", receipt.getAmount());
                map.put("notes", receipt.getNotes());
                mappedData.add(map);
            }

            if (mappedData.isEmpty()) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("id", 0);
                emptyMap.put("paymentDate", "-");
                emptyMap.put("amount", 0.0);
                emptyMap.put("notes", "No se registran entregas guardadas.");
                mappedData.add(emptyMap);
            }

            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(mappedData);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("CUSTOMER_NAME", sale.getCustomerName());
            parameters.put("CUSTOMER_PHONE", "");
            parameters.put("CUSTOMER_ADDRESS", "");
            parameters.put("CUSTOMER_EMAIL", "");
            parameters.put("SALE_ID_TEXT", "Venta #" + sale.getId() + " - " + sale.getProductName());
            parameters.put("TOTAL_PAID", formattedPaid);
            parameters.put("TOTAL_PENDING", formattedPending);

            InputStream reportStream = ReportManager.class.getResourceAsStream("/reports/sale_statement.jrxml");
            if (reportStream == null) {
                System.err.println("ERROR: Could not find sale_statement.jrxml");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 🌟 INTENTO PREMIUM: Para el reporte individual de ventas
            try {
                File tempFile = File.createTempFile("Resumen_Venta_", ".pdf");
                tempFile.deleteOnExit();
                JasperExportManager.exportReportToPdfFile(jasperPrint, tempFile.getAbsolutePath());

                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                    java.awt.Desktop.getDesktop().open(tempFile);
                } else {
                    mostrarVisorInternoJasper(jasperPrint, "Resumen de Cuenta - " + sale.getCustomerName());
                }
            } catch (Exception ex) {
                System.err.println("Aviso: No se pudo abrir el PDF nativo del resumen, usando visor Jasper. " + ex.getMessage());
                mostrarVisorInternoJasper(jasperPrint, "Resumen de Cuenta - " + sale.getCustomerName());
            }

        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    /**
     * 💡 Método Auxiliar Unificado para mitigar redundancias y fallas en hilos de JavaFX.
     */
    private static void mostrarVisorInternoJasper(JasperPrint jasperPrint, String title) {
        javafx.application.Platform.runLater(() -> {
            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle(title);
            viewer.setVisible(true);
            viewer.toFront();
        });
    }
}
