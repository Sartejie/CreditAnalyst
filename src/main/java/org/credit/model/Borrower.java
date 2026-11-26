package org.credit.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Субъект кредитной истории и заявитель (физическое лицо, Форма 2501 НБ РБ).
 */
public class Borrower {
    private long id;
    private String clientLogin;

    // Структурированное ФИО
    private String lastName;
    private String firstName;
    private String middleName;
    private String fullName;

    private String citizenship;
    private String idNumber;
    private Date birthDate;

    // Структурированный документ, удостоверяющий личность
    private String documentType;
    private String documentSeriesNumber;
    private Date documentIssueDate;
    private String document;

    // Структурированное место жительства
    private String country;
    private String region;
    private String district;
    private String locality;
    private String address;

    private Double monthlyIncome;

    public Borrower() {}

    public Borrower(String fullName, String citizenship, String idNumber, Date birthDate,
                    String document, String address, Double monthlyIncome) {
        setFullName(fullName);
        this.citizenship = citizenship;
        this.idNumber = idNumber;
        this.birthDate = birthDate;
        setDocument(document);
        setAddress(address);
        this.monthlyIncome = monthlyIncome;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getClientLogin() { return clientLogin; }
    public void setClientLogin(String clientLogin) { this.clientLogin = clientLogin; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = trimOrNull(lastName);
        rebuildFullName();
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = trimOrNull(firstName);
        rebuildFullName();
    }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) {
        this.middleName = trimOrNull(middleName);
        rebuildFullName();
    }

    public String getFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        rebuildFullName();
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = trimOrNull(fullName);
        this.lastName = null;
        this.firstName = null;
        this.middleName = null;
        if (this.fullName != null && !this.fullName.isEmpty()) {
            String[] parts = this.fullName.split("\\s+");
            if (parts.length >= 1) {
                this.lastName = parts[0];
            }
            if (parts.length >= 2) {
                this.firstName = parts[1];
            }
            if (parts.length >= 3) {
                this.middleName = String.join(" ", java.util.Arrays.copyOfRange(parts, 2, parts.length));
            }
        }
    }

    private void rebuildFullName() {
        List<String> parts = new ArrayList<>();
        if (lastName != null && !lastName.isEmpty()) parts.add(lastName);
        if (firstName != null && !firstName.isEmpty()) parts.add(firstName);
        if (middleName != null && !middleName.isEmpty()) parts.add(middleName);
        if (!parts.isEmpty()) {
            this.fullName = String.join(" ", parts);
        }
    }

    public String getCitizenship() { return citizenship; }
    public void setCitizenship(String citizenship) { this.citizenship = trimOrNull(citizenship); }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = trimOrNull(idNumber); }

    public Date getBirthDate() { return birthDate; }
    public void setBirthDate(Date birthDate) { this.birthDate = birthDate; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) {
        this.documentType = trimOrNull(documentType);
        rebuildDocument();
    }

    public String getDocumentSeriesNumber() { return documentSeriesNumber; }
    public void setDocumentSeriesNumber(String documentSeriesNumber) {
        this.documentSeriesNumber = trimOrNull(documentSeriesNumber);
        rebuildDocument();
    }

    public Date getDocumentIssueDate() { return documentIssueDate; }
    public void setDocumentIssueDate(Date documentIssueDate) {
        this.documentIssueDate = documentIssueDate;
        rebuildDocument();
    }

    public String getDocument() {
        if (document != null && !document.trim().isEmpty()) {
            return document;
        }
        rebuildDocument();
        return document;
    }

    public void setDocument(String document) {
        this.document = trimOrNull(document);
        if (this.document != null && (documentType == null || documentType.isEmpty())) {
            this.documentType = this.document;
        }
    }

    private void rebuildDocument() {
        List<String> parts = new ArrayList<>();
        if (documentType != null && !documentType.isEmpty()) parts.add(documentType);
        if (documentSeriesNumber != null && !documentSeriesNumber.isEmpty()) parts.add(documentSeriesNumber);
        if (documentIssueDate != null) {
            parts.add("выд. " + new SimpleDateFormat("dd.MM.yyyy").format(documentIssueDate));
        }
        if (!parts.isEmpty()) {
            this.document = String.join(", ", parts);
        }
    }

    public String getCountry() { return country; }
    public void setCountry(String country) {
        this.country = trimOrNull(country);
        rebuildAddress();
    }

    public String getRegion() { return region; }
    public void setRegion(String region) {
        this.region = trimOrNull(region);
        rebuildAddress();
    }

    public String getDistrict() { return district; }
    public void setDistrict(String district) {
        this.district = trimOrNull(district);
        rebuildAddress();
    }

    public String getLocality() { return locality; }
    public void setLocality(String locality) {
        this.locality = trimOrNull(locality);
        rebuildAddress();
    }

    public String getAddress() {
        if (address != null && !address.trim().isEmpty()) {
            return address;
        }
        rebuildAddress();
        return address;
    }

    public void setAddress(String address) {
        this.address = trimOrNull(address);
    }

    private void rebuildAddress() {
        List<String> parts = new ArrayList<>();
        if (country != null && !country.isEmpty()) parts.add(country);
        if (region != null && !region.isEmpty()) parts.add(region);
        if (district != null && !district.isEmpty()) parts.add(district);
        if (locality != null && !locality.isEmpty()) parts.add(locality);
        if (!parts.isEmpty()) {
            this.address = String.join(", ", parts);
        }
    }

    public Double getIncome() { return monthlyIncome; }
    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public Boolean validate() {
        return validateDetailed().isEmpty();
    }

    /**
     * Детализированная валидация с указанием конкретного поля и сути ошибки.
     */
    public List<ValidationError> validateDetailed() {
        List<ValidationError> errors = new ArrayList<>();
        String section = "Заёмщик";

        String resolvedName = getFullName();
        if (resolvedName == null || resolvedName.trim().isEmpty()) {
            errors.add(new ValidationError(section, "ФИО", "обязательно для заполнения"));
        }
        if (idNumber == null || idNumber.trim().isEmpty()) {
            errors.add(new ValidationError(section, "Идентификационный номер", "обязателен для заполнения"));
        }
        if (birthDate == null) {
            errors.add(new ValidationError(section, "Дата рождения", "обязательна для заполнения"));
        } else {
            Calendar endOfToday = Calendar.getInstance();
            endOfToday.set(Calendar.HOUR_OF_DAY, 23);
            endOfToday.set(Calendar.MINUTE, 59);
            endOfToday.set(Calendar.SECOND, 59);
            endOfToday.set(Calendar.MILLISECOND, 999);
            if (birthDate.after(endOfToday.getTime())) {
                errors.add(new ValidationError(section, "Дата рождения", "не может быть в будущем"));
            }
        }
        if (monthlyIncome == null) {
            errors.add(new ValidationError(section, "Среднемесячный доход", "обязателен для заполнения"));
        } else if (monthlyIncome < 0) {
            errors.add(new ValidationError(section, "Среднемесячный доход", "не может быть отрицательным (>= 0)"));
        }

        return errors;
    }

    private static String trimOrNull(String value) {
        return value == null ? null : value.trim();
    }
}
