package com.edj.developer.apploans.util;

import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.view.JasperViewer;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportManager {

    public static void launchReport(String reportTitle, List<?> rawData, String codeType) {
        try {
            // 1. Map properties dynamically into generic fields for the jrxml
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
                    map.put("value", general.getInstallmentAmount());
                }
                mappedData.add(map);
            }

            // Fallback row if data list is empty
            if (mappedData.isEmpty()) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("id", 0);
                emptyMap.put("name", "No se encontraron registros activos.");
                emptyMap.put("value", 0.0);
                mappedData.add(emptyMap);
            }

            // 2. Wrap data inside Jasper structure
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(mappedData);

            // 3. Setup header parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("REPORT_TITLE", reportTitle.toUpperCase());
            parameters.put("CODE_TYPE", codeType);

            // 4. Locate and compile JRXML resource file
            InputStream reportStream = ReportManager.class.getResourceAsStream("/reports/CollectionTwoColumns.jrxml");
            if (reportStream == null) {
                System.err.println("ERROR: Could not find CollectionTwoColumns.jrxml in resources/reports/");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            // 5. Open Jasper native viewer on FX Application Thread to prevent interface lockups
            javafx.application.Platform.runLater(() -> {
                JasperViewer viewer = new JasperViewer(jasperPrint, false);
                viewer.setTitle(reportTitle);
                viewer.setVisible(true);
                viewer.toFront();
            });

        } catch (JRException e) {
            e.printStackTrace();
        }
    }

    public static void launchSaleStatement(com.edj.developer.apploans.model.Sale sale, String formattedPaid, String formattedPending) {
        try {
            // 1. Convert SaleReceipt objects into generic maps for the table datasource
            List<Map<String, ?>> mappedData = new ArrayList<>();
            for (com.edj.developer.apploans.model.SaleReceipt receipt : sale.getReceipts()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", receipt.getId());
                map.put("paymentDate", receipt.getPaymentDate());
                map.put("amount", receipt.getAmount());
                map.put("notes", receipt.getNotes());
                mappedData.add(map);
            }

            // Fallback row if history list is empty
            if (mappedData.isEmpty()) {
                Map<String, Object> emptyMap = new HashMap<>();
                emptyMap.put("id", 0);
                emptyMap.put("paymentDate", "-");
                emptyMap.put("amount", 0.0);
                emptyMap.put("notes", "No se registran entregas guardadas.");
                mappedData.add(emptyMap);
            }

            net.sf.jasperreports.engine.data.JRMapCollectionDataSource dataSource = new net.sf.jasperreports.engine.data.JRMapCollectionDataSource(mappedData);

            // 2. Map header and summary properties
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("CUSTOMER_NAME", sale.getCustomerName());
            parameters.put("CUSTOMER_PHONE", ""); // Leave empty or map if your model has it
            parameters.put("CUSTOMER_ADDRESS", "");
            parameters.put("CUSTOMER_EMAIL", "");
            parameters.put("SALE_ID_TEXT", "Venta #" + sale.getId() + " - " + sale.getProductName());
            parameters.put("TOTAL_PAID", formattedPaid);
            parameters.put("TOTAL_PENDING", formattedPending);

            // 3. Compile and build viewer window
            InputStream reportStream = ReportManager.class.getResourceAsStream("/reports/sale_statement.jrxml");
            if (reportStream == null) {
                System.err.println("ERROR: Could not find sale_statement.jrxml");
                return;
            }

            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

            javafx.application.Platform.runLater(() -> {
                JasperViewer viewer = new JasperViewer(jasperPrint, false);
                viewer.setTitle("Resumen de Cuenta - " + sale.getCustomerName());
                viewer.setVisible(true);
                viewer.toFront();
            });

        } catch (JRException e) {
            e.printStackTrace();
        }
    }
}