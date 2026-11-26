package org.credit.model;

import java.util.Date;

public class Borrower {
    private String fullName;
    private String citizenship;
    private String idNumber;
    private Date birthDate;
    private String document;
    private String address;
    private Double monthlyIncome;

    public Borrower() {}

    public Borrower(String fullName, String citizenship, String idNumber, Date birthDate, String document, String address, Double monthlyIncome) {
        this.fullName = fullName;
        this.citizenship = citizenship;
        this.idNumber = idNumber;
        this.birthDate = birthDate;
        this.document = document;
        this.address = address;
        this.monthlyIncome = monthlyIncome;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = citizenship; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

    public String getDocument() { return document; }
    public void setDocument(String document) { this.document = document; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public Boolean validate() {
        if (fullName == null || fullName.trim().isEmpty()) return false;
        if (idNumber == null || idNumber.trim().isEmpty()) return false;
        if (monthlyIncome == null || monthlyIncome <= 0) return false;
        if (birthDate == null) return false;
        return true;
    }
}