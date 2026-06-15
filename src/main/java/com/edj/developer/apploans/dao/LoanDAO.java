package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.Loan;
import com.edj.developer.apploans.model.LoanSummary;
import java.util.List;
import java.util.Map;

public interface LoanDAO {
    boolean saveFullLoan(Loan loan);

    List<Double> findAllSuggestedAmounts();

    List<String> findAllFrequencies();

    List<Loan> findAll();

    List<Loan> findAllPaged(String search, String statusFilter, int limit, int offset);

    int countLoans(String search, String statusFilter);

    boolean updatePaymentStatus(int paymentId, String newStatus);

    Loan findFullLoanById(int loanId);

    boolean updatePaymentStatusWithAmount(int id, String nuevoEstado, double nuevoPagado);

    List<Loan> findLoansByCustomerId(int customerId);

    LoanSummary getSummaryByCustomerId(int customerId);

    Map<String, Integer> findAllFrequenciesWithIntervals();

    void checkAndUpdateOverdueInstallments();

    // Agregá esta línea dentro de tu interfaz LoanDAO
    boolean cancelLoan(int loanId);

    // Agregá esta firma al final de tu interfaz LoanDAO
    boolean processCascadePayment(int loanId, double totalAmount, String notes);
}