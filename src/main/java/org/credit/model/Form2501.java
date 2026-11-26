package org.credit.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Форма 2501 «Сведения, входящие в состав кредитной истории» (Постановление НБ РБ № 291).
 */
public class Form2501 {
    private List<CreditDeal> creditDeals;
    private List<OverdraftInfo> overdrafts;
    private Borrower importedBorrower;

    public Form2501() {
        this.creditDeals = new ArrayList<>();
        this.overdrafts = new ArrayList<>();
    }

    public List<CreditDeal> getCreditDeals() { return creditDeals; }
    public void setCreditDeals(List<CreditDeal> creditDeals) {
        this.creditDeals = creditDeals != null ? creditDeals : new ArrayList<>();
    }

    public List<OverdraftInfo> getOverdrafts() { return overdrafts; }
    public void setOverdrafts(List<OverdraftInfo> overdrafts) {
        this.overdrafts = overdrafts != null ? overdrafts : new ArrayList<>();
    }

    public Borrower getImportedBorrower() { return importedBorrower; }
    public void setImportedBorrower(Borrower importedBorrower) { this.importedBorrower = importedBorrower; }

    /**
     * Проверяет наличие непустой и валидной кредитной истории.
     */
    public Boolean validate() {
        if (creditDeals == null || creditDeals.isEmpty()) {
            return false;
        }
        for (int i = 0; i < creditDeals.size(); i++) {
            if (!creditDeals.get(i).validateDetailed(i + 1).isEmpty()) {
                return false;
            }
        }
        if (overdrafts != null) {
            for (int i = 0; i < overdrafts.size(); i++) {
                if (!overdrafts.get(i).validateDetailed(i + 1).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Детализированная валидация имеющихся сделок и овердрафтов.
     */
    public List<ValidationError> validateDetailed() {
        List<ValidationError> errors = new ArrayList<>();
        if (creditDeals != null) {
            for (int i = 0; i < creditDeals.size(); i++) {
                errors.addAll(creditDeals.get(i).validateDetailed(i + 1));
            }
        }
        if (overdrafts != null) {
            for (int i = 0; i < overdrafts.size(); i++) {
                errors.addAll(overdrafts.get(i).validateDetailed(i + 1));
            }
        }
        return errors;
    }

    /**
     * Импорт Формы 2501 (и сведений о заёмщике при наличии) из XML файла.
     */
    public void importXML(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Файл не существует");
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            // Субъект кредитной истории (если присутствует в XML)
            NodeList borrowerNodes = doc.getElementsByTagName("Borrower");
            if (borrowerNodes.getLength() > 0) {
                Element bEl = (Element) borrowerNodes.item(0);
                Borrower b = new Borrower();
                String lastName = getTagValue("lastName", bEl);
                String firstName = getTagValue("firstName", bEl);
                String middleName = getTagValue("middleName", bEl);
                if (lastName != null) b.setLastName(lastName);
                if (firstName != null) b.setFirstName(firstName);
                if (middleName != null) b.setMiddleName(middleName);

                String fullName = getTagValue("fullName", bEl);
                if (fullName != null && !fullName.isEmpty()) b.setFullName(fullName);

                b.setCitizenship(getTagValue("citizenship", bEl));
                b.setIdNumber(getTagValue("idNumber", bEl));
                b.setBirthDate(parseDateFlexible(getTagValue("birthDate", bEl)));

                String docType = getTagValue("documentType", bEl);
                String docSeries = getTagValue("documentSeriesNumber", bEl);
                Date docIssue = parseDateFlexible(getTagValue("documentIssueDate", bEl));
                if (docType != null) b.setDocumentType(docType);
                if (docSeries != null) b.setDocumentSeriesNumber(docSeries);
                if (docIssue != null) b.setDocumentIssueDate(docIssue);
                String flatDoc = getTagValue("document", bEl);
                if (flatDoc != null && !flatDoc.isEmpty()) b.setDocument(flatDoc);

                String country = getTagValue("country", bEl);
                String region = getTagValue("region", bEl);
                String district = getTagValue("district", bEl);
                String locality = getTagValue("locality", bEl);
                if (country != null) b.setCountry(country);
                if (region != null) b.setRegion(region);
                if (district != null) b.setDistrict(district);
                if (locality != null) b.setLocality(locality);
                String flatAddr = getTagValue("address", bEl);
                if (flatAddr != null && !flatAddr.isEmpty()) b.setAddress(flatAddr);

                b.setMonthlyIncome(parseDoubleNullable(getTagValue("monthlyIncome", bEl), "monthlyIncome"));
                this.importedBorrower = b;
            } else {
                this.importedBorrower = null;
            }

            // Кредитные сделки
            NodeList nList = doc.getElementsByTagName("CreditDeal");
            creditDeals.clear();
            for (int i = 0; i < nList.getLength(); i++) {
                Element element = (Element) nList.item(i);
                CreditDeal deal = new CreditDeal();
                deal.setContractNumber(getTagValue("contractNumber", element));
                deal.setDealType(getTagValue("dealType", element));
                deal.setCreditType(getTagValue("creditType", element));
                deal.setConclusionDate(parseDateFlexible(getTagValue("conclusionDate", element)));
                deal.setAmount(parseDoubleNullable(getTagValue("amount", element), "amount"));
                deal.setCurrency(getTagValue("currency", element));
                deal.setTerm(parseIntNullable(getTagValue("term", element), "term"));
                deal.setRenewalTerm(parseIntNullable(getTagValue("renewalTerm", element), "renewalTerm"));
                deal.setRepaymentDate(parseDateFlexible(getTagValue("repaymentDate", element)));
                deal.setSecurity(getTagValue("security", element));
                deal.setRiskGroup(getTagValue("riskGroup", element));
                deal.setRelevanceDate(parseDateFlexible(getTagValue("relevanceDate", element)));
                deal.setOverdueDays(parseIntNullable(getTagValue("overdueDays", element), "overdueDays"));
                deal.setOverdueAmount(parseDoubleNullable(getTagValue("overdueAmount", element), "overdueAmount"));
                deal.setMonthlyPayment(parseDoubleNullable(getTagValue("monthlyPayment", element), "monthlyPayment"));
                creditDeals.add(deal);
            }

            // Сведения об овердрафте
            overdrafts.clear();
            parseOverdraftXmlNodes(doc.getElementsByTagName("Overdraft"));
            parseOverdraftXmlNodes(doc.getElementsByTagName("OverdraftInfo"));

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении XML: " + e.getMessage(), e);
        }
    }

    private void parseOverdraftXmlNodes(NodeList nodes) throws ParseException {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            OverdraftInfo info = new OverdraftInfo();
            info.setContractDate(parseDateFlexible(getTagValue("contractDate", el)));
            info.setLimitAmount(parseDoubleNullable(getTagValue("limitAmount", el), "limitAmount"));
            info.setLimitSetDate(parseDateFlexible(getTagValue("limitSetDate", el)));
            overdrafts.add(info);
        }
    }

    /**
     * Импорт Формы 2501 (и сведений о заёмщике при наличии) из JSON файла.
     */
    public void importJSON(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Файл не существует");
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(file);
            if (root == null || root.isNull()) {
                throw new IllegalArgumentException("JSON файл пуст");
            }

            this.importedBorrower = null;
            creditDeals.clear();
            overdrafts.clear();

            if (root.isArray()) {
                for (JsonNode dealNode : root) {
                    creditDeals.add(parseDealFromJson(dealNode));
                }
                return;
            }

            if (!root.isObject()) {
                throw new IllegalArgumentException("Корневой элемент JSON должен быть объектом или массивом сделок");
            }

            JsonNode borrowerNode = root.has("borrower") ? root.get("borrower") : root.get("Borrower");
            if (borrowerNode != null && borrowerNode.isObject()) {
                this.importedBorrower = parseBorrowerFromJson(borrowerNode);
            }

            JsonNode formNode = root.has("form2501") ? root.get("form2501") : root;
            JsonNode dealsNode = formNode.has("creditDeals") ? formNode.get("creditDeals")
                    : (formNode.has("deals") ? formNode.get("deals") : formNode.get("CreditDeal"));
            if (dealsNode != null) {
                if (!dealsNode.isArray()) {
                    throw new IllegalArgumentException("Поле creditDeals должно быть массивом");
                }
                for (JsonNode dNode : dealsNode) {
                    creditDeals.add(parseDealFromJson(dNode));
                }
            }

            JsonNode overdraftsNode = formNode.has("overdrafts") ? formNode.get("overdrafts")
                    : formNode.get("overdraft");
            if (overdraftsNode != null) {
                if (overdraftsNode.isArray()) {
                    for (JsonNode oNode : overdraftsNode) {
                        overdrafts.add(parseOverdraftFromJson(oNode));
                    }
                } else if (overdraftsNode.isObject()) {
                    overdrafts.add(parseOverdraftFromJson(overdraftsNode));
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении JSON: " + e.getMessage(), e);
        }
    }

    private Borrower parseBorrowerFromJson(JsonNode node) throws ParseException {
        Borrower b = new Borrower();
        if (hasText(node, "lastName")) b.setLastName(node.get("lastName").asText());
        if (hasText(node, "firstName")) b.setFirstName(node.get("firstName").asText());
        if (hasText(node, "middleName")) b.setMiddleName(node.get("middleName").asText());
        if (hasText(node, "fullName")) b.setFullName(node.get("fullName").asText());
        if (hasText(node, "citizenship")) b.setCitizenship(node.get("citizenship").asText());
        if (hasText(node, "idNumber")) b.setIdNumber(node.get("idNumber").asText());
        if (hasText(node, "birthDate")) b.setBirthDate(parseDateFlexible(node.get("birthDate").asText()));

        if (hasText(node, "documentType")) b.setDocumentType(node.get("documentType").asText());
        if (hasText(node, "documentSeriesNumber")) b.setDocumentSeriesNumber(node.get("documentSeriesNumber").asText());
        if (hasText(node, "documentIssueDate")) b.setDocumentIssueDate(parseDateFlexible(node.get("documentIssueDate").asText()));
        if (hasText(node, "document")) b.setDocument(node.get("document").asText());

        if (hasText(node, "country")) b.setCountry(node.get("country").asText());
        if (hasText(node, "region")) b.setRegion(node.get("region").asText());
        if (hasText(node, "district")) b.setDistrict(node.get("district").asText());
        if (hasText(node, "locality")) b.setLocality(node.get("locality").asText());
        if (hasText(node, "address")) b.setAddress(node.get("address").asText());

        if (node.has("monthlyIncome") && !node.get("monthlyIncome").isNull()) {
            b.setMonthlyIncome(parseJsonDouble(node.get("monthlyIncome"), "monthlyIncome"));
        }
        return b;
    }

    private CreditDeal parseDealFromJson(JsonNode node) throws ParseException {
        if (!node.isObject()) {
            throw new IllegalArgumentException("Элемент сделки в JSON должен быть объектом");
        }
        CreditDeal deal = new CreditDeal();
        if (hasText(node, "contractNumber")) deal.setContractNumber(node.get("contractNumber").asText());
        if (hasText(node, "dealType")) deal.setDealType(node.get("dealType").asText());
        if (hasText(node, "creditType")) deal.setCreditType(node.get("creditType").asText());
        if (hasText(node, "conclusionDate")) deal.setConclusionDate(parseDateFlexible(node.get("conclusionDate").asText()));
        if (node.has("amount") && !node.get("amount").isNull()) deal.setAmount(parseJsonDouble(node.get("amount"), "amount"));
        if (hasText(node, "currency")) deal.setCurrency(node.get("currency").asText());
        if (node.has("term") && !node.get("term").isNull()) deal.setTerm(parseJsonInt(node.get("term"), "term"));
        if (node.has("renewalTerm") && !node.get("renewalTerm").isNull()) deal.setRenewalTerm(parseJsonInt(node.get("renewalTerm"), "renewalTerm"));
        if (hasText(node, "repaymentDate")) deal.setRepaymentDate(parseDateFlexible(node.get("repaymentDate").asText()));
        if (hasText(node, "security")) deal.setSecurity(node.get("security").asText());
        if (hasText(node, "riskGroup")) deal.setRiskGroup(node.get("riskGroup").asText());
        if (hasText(node, "relevanceDate")) deal.setRelevanceDate(parseDateFlexible(node.get("relevanceDate").asText()));
        if (node.has("overdueDays") && !node.get("overdueDays").isNull()) deal.setOverdueDays(parseJsonInt(node.get("overdueDays"), "overdueDays"));
        if (node.has("overdueAmount") && !node.get("overdueAmount").isNull()) deal.setOverdueAmount(parseJsonDouble(node.get("overdueAmount"), "overdueAmount"));
        if (node.has("monthlyPayment") && !node.get("monthlyPayment").isNull()) deal.setMonthlyPayment(parseJsonDouble(node.get("monthlyPayment"), "monthlyPayment"));
        return deal;
    }

    private OverdraftInfo parseOverdraftFromJson(JsonNode node) throws ParseException {
        OverdraftInfo info = new OverdraftInfo();
        if (hasText(node, "contractDate")) info.setContractDate(parseDateFlexible(node.get("contractDate").asText()));
        if (node.has("limitAmount") && !node.get("limitAmount").isNull()) info.setLimitAmount(parseJsonDouble(node.get("limitAmount"), "limitAmount"));
        if (hasText(node, "limitSetDate")) info.setLimitSetDate(parseDateFlexible(node.get("limitSetDate").asText()));
        return info;
    }

    private static boolean hasText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() && !node.get(field).asText().trim().isEmpty();
    }

    private static Double parseJsonDouble(JsonNode node, String field) {
        if (node.isNumber()) return node.asDouble();
        return parseDoubleNullable(node.asText(), field);
    }

    private static Integer parseJsonInt(JsonNode node, String field) {
        if (node.isInt() || node.isLong()) return node.asInt();
        return parseIntNullable(node.asText(), field);
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getTextContent() != null) {
            String text = nodeList.item(0).getTextContent().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private static Double parseDoubleNullable(String value, String field) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(value.trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное числовое значение в поле «" + field + "»: " + value);
        }
    }

    private static Integer parseIntNullable(String value, String field) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное целое значение в поле «" + field + "»: " + value);
        }
    }

    public static Date parseDateFlexible(String text) throws ParseException {
        if (text == null || text.trim().isEmpty()) return null;
        String trimmed = text.trim();
        for (String pattern : new String[]{"yyyy-MM-dd", "dd.MM.yyyy", "yyyy-MM-dd HH:mm:ss"}) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(trimmed);
            } catch (ParseException ignored) {
            }
        }
        throw new ParseException("Неподдерживаемый формат даты: " + trimmed, 0);
    }
}
