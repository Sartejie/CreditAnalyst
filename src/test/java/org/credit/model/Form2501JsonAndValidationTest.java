package org.credit.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Form2501JsonAndValidationTest {

    @Test
    void testImportJsonWithBorrowerDealsAndOverdraft(@TempDir Path tempDir) throws Exception {
        File jsonFile = tempDir.resolve("form2501.json").toFile();
        String json = """
                {
                  "borrower": {
                    "lastName": "Иванов",
                    "firstName": "Иван",
                    "middleName": "Иванович",
                    "idNumber": "3010190A001PB1",
                    "birthDate": "1990-01-01",
                    "monthlyIncome": 2500.0
                  },
                  "form2501": {
                    "creditDeals": [
                      {
                        "contractNumber": "JSON-001",
                        "amount": 5000.0,
                        "term": 10,
                        "conclusionDate": "2025-01-01",
                        "repaymentDate": "2025-11-01",
                        "riskGroup": "1"
                      }
                    ],
                    "overdrafts": [
                      {
                        "contractDate": "2025-02-01",
                        "limitAmount": 1000.0,
                        "limitSetDate": "2025-02-01"
                      }
                    ]
                  }
                }
                """;
        Files.writeString(jsonFile.toPath(), json);

        Form2501 form = new Form2501();
        form.importJSON(jsonFile);

        assertNotNull(form.getImportedBorrower());
        assertEquals("Иванов Иван Иванович", form.getImportedBorrower().getFullName());
        assertEquals(1, form.getCreditDeals().size());
        assertEquals("JSON-001", form.getCreditDeals().get(0).getContractNumber());
        assertEquals(1, form.getOverdrafts().size());
        assertEquals(1000.0, form.getOverdrafts().get(0).getLimitAmount());
    }

    @Test
    void testDetailedValidationIncludesFieldAndReason() {
        CreditDeal deal = new CreditDeal();
        deal.setContractNumber("");
        deal.setAmount(-10.0);
        deal.setConclusionDate(new Date(2000000000000L));
        deal.setRepaymentDate(new Date(1000000000000L));

        List<ValidationError> errors = deal.validateDetailed(1);
        assertEquals(3, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.getField().contains("Номер договора")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().contains("Сумма кредита")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().contains("Срок возврата")));
    }
}
