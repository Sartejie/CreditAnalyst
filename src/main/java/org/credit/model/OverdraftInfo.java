package org.credit.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Сведения об овердрафте в составе Формы 2501 (п. 6.1 ТЗ).
 */
public class OverdraftInfo {
    private Date contractDate;
    private Double limitAmount;
    private Date limitSetDate;

    public OverdraftInfo() {}

    public OverdraftInfo(Date contractDate, Double limitAmount, Date limitSetDate) {
        this.contractDate = contractDate;
        this.limitAmount = limitAmount;
        this.limitSetDate = limitSetDate;
    }

    public Date getContractDate() {
        return contractDate;
    }

    public void setContractDate(Date contractDate) {
        this.contractDate = contractDate;
    }

    public Double getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(Double limitAmount) {
        this.limitAmount = limitAmount;
    }

    public Date getLimitSetDate() {
        return limitSetDate;
    }

    public void setLimitSetDate(Date limitSetDate) {
        this.limitSetDate = limitSetDate;
    }

    public List<ValidationError> validateDetailed(int index) {
        List<ValidationError> errors = new ArrayList<>();
        String section = "Овердрафт №" + index;
        if (limitAmount == null || limitAmount <= 0) {
            errors.add(new ValidationError(section, "Лимит овердрафта", "должен быть больше 0"));
        }
        if (contractDate == null) {
            errors.add(new ValidationError(section, "Дата заключения договора", "обязательна для заполнения"));
        }
        if (limitSetDate == null) {
            errors.add(new ValidationError(section, "Дата установления лимита", "обязательна для заполнения"));
        }
        if (contractDate != null && limitSetDate != null && limitSetDate.before(contractDate)) {
            errors.add(new ValidationError(section, "Дата установления лимита",
                    "не может быть раньше даты заключения договора"));
        }
        return errors;
    }
}
