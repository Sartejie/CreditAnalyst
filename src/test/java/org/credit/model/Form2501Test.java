package org.credit.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Form2501Test {
    private Form2501 form;

    @BeforeEach
    void setUp() {
        form = new Form2501();
    }

    @Test
    void testValidateEmptyList() {
        assertFalse(form.validate(), "Пустая форма не должна проходить валидацию");
    }

    @Test
    void testValidateWithInvalidDeal() {
        CreditDeal deal = new CreditDeal();
        deal.setAmount(-500.0); // Невалидная сумма
        deal.setContractNumber("123");

        form.getCreditDeals().add(deal);
        assertFalse(form.validate(), "Форма с невалидной сделкой не проходит проверку");
    }

    @Test
    void testValidateWithValidDeal() {
        CreditDeal deal = new CreditDeal();
        deal.setAmount(1000.0);
        deal.setContractNumber("123");

        form.getCreditDeals().add(deal);
        assertTrue(form.validate(), "Форма с валидной сделкой должна проходить проверку");
    }

    @Test
    void testImportXML(@TempDir Path tempDir) throws Exception {
        File xmlFile = tempDir.resolve("test_form.xml").toFile();

        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Form2501>
                    <CreditDeal>
                        <contractNumber>XML-999</contractNumber>
                        <dealType>Кредит</dealType>
                        <creditType>Ипотека</creditType>
                        <amount>5000000.0</amount>
                        <currency>RUB</currency>
                        <term>120</term>
                    </CreditDeal>
                </Form2501>
                """;

        try (FileWriter writer = new FileWriter(xmlFile)) {
            writer.write(xmlContent);
        }

        form.importXML(xmlFile);

        List<CreditDeal> deals = form.getCreditDeals();
        assertEquals(1, deals.size(), "Должна импортироваться одна сделка");

        CreditDeal importedDeal = deals.get(0);
        assertEquals("XML-999", importedDeal.getContractNumber());
        assertEquals("Ипотека", importedDeal.getCreditType());
        assertEquals(5000000.0, importedDeal.getAmount());
        assertEquals(120, importedDeal.getTerm());
    }
}