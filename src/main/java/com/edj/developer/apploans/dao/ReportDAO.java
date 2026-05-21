package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.DailyReportItem;
import com.edj.developer.apploans.model.GeneralReportItem;
import java.util.List;

public interface ReportDAO {
    List<DailyReportItem> getDailyDueInstallments();
    List<GeneralReportItem> getActiveLoansInstallments();
}