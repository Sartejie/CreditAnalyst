package org.credit.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Form2501 {
    private List<CreditDeal> creditDeals;

    public Form2501() {
        this.creditDeals = new ArrayList<>();
    }

    public List<CreditDeal> getCreditDeals() {
        return creditDeals;
    }

    public void setCreditDeals(List<CreditDeal> creditDeals) {
        this.creditDeals = creditDeals;
    }

    public Boolean validate() {
        if (creditDeals == null || creditDeals.isEmpty()) {
            return false;
        }
        for (CreditDeal deal : creditDeals) {
            if (deal.getContractNumber() == null || deal.getContractNumber().trim().isEmpty()) {
                return false;
            }
            if (deal.getAmount() == null || deal.getAmount() <= 0) {
                return false;
            }
        }
        return true;
    }

    public void importXML(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("Файл не существует");
        }

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("CreditDeal");
            creditDeals.clear();

            for (int temp = 0; temp < nList.getLength(); temp++) {
                Element element = (Element) nList.item(temp);
                CreditDeal deal = new CreditDeal();

                deal.setContractNumber(getTagValue("contractNumber", element));
                deal.setDealType(getTagValue("dealType", element));
                deal.setCreditType(getTagValue("creditType", element));

                String amountStr = getTagValue("amount", element);
                if (amountStr != null && !amountStr.isEmpty()) {
                    deal.setAmount(Double.parseDouble(amountStr));
                }

                String termStr = getTagValue("term", element);
                if (termStr != null && !termStr.isEmpty()) {
                    deal.setTerm(Integer.parseInt(termStr));
                }

                deal.setCurrency(getTagValue("currency", element));
                deal.setSecurity(getTagValue("security", element));
                deal.setRiskGroup(getTagValue("riskGroup", element));

                creditDeals.add(deal);
            }
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при чтении XML: " + e.getMessage(), e);
        }
    }

    private String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}