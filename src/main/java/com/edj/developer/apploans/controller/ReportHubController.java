package com.edj.developer.apploans.controller;

import com.edj.developer.apploans.dao.ReportDAO;
import com.edj.developer.apploans.dao.impl.ReportDAOImpl;
import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;
import com.edj.developer.apploans.util.ReportManager;
import javafx.fxml.FXML;
import java.util.List;

public class ReportHubController {

    private final ReportDAO reportDAO = new ReportDAOImpl();

    // ================= LOAN HUB METHODS =================
    @FXML
    private void generateDailyReport() {
        List<DailyReportItem> data = reportDAO.getDailyDueInstallments();
        ReportManager.launchReport("Reporte Diario (Vencimientos de Hoy)", data, "PRÉST.");
    }

    @FXML
    private void generateGeneralReport() {
        List<GeneralReportItem> data = reportDAO.getActiveLoansInstallments();
        ReportManager.launchReport("Planilla General de Cobranza", data, "PRÉST.");
    }

    // ================= COMMERCIAL SALES METHODS =================
    @FXML
    private void generateSaleDailyReport() {
        List<DailyReportItem> data = reportDAO.getDailyDueSalesInstallments();
        ReportManager.launchReport("Reporte Diario Ventas (Vencimientos de Hoy)", data, "VENTA");
    }

    @FXML
    private void generateSaleGeneralReport() {
        List<GeneralReportItem> data = reportDAO.getActiveSalesInstallments();
        ReportManager.launchReport("Planilla General de Cobranza - Ventas", data, "VENTA");
    }
}