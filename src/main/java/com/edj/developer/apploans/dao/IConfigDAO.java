package com.edj.developer.apploans.dao;

import com.edj.developer.apploans.model.LoanAmount;
import com.edj.developer.apploans.model.LoanFrequency;
import java.util.List;

public interface IConfigDAO {
    List<LoanAmount> findAllAmounts();
    boolean saveAmount(LoanAmount amount);
    boolean deleteAmount(int id);

    List<LoanFrequency> findAllFrequencies();
    boolean saveFrequency(LoanFrequency frequency);
    boolean deleteFrequency(int id);
}