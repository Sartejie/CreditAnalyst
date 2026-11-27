package org.credit.model;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.credit.db.Database;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Итоговый аналитический отчёт, агрегирующий график, ПДН, состояние кредита и скоринг.
 */
public class FinalReport {
    private long id;
    private long applicationId;
    private String createdBy;
    private Date createdAt = new Date();

    private Borrower borrower;
    private LoanApplication application;
    private Form2501 form2501;

    private PaymentSchedule schedule;
    private DebtBurden pdn;
    private CreditStateReport state;
    private ScoringResult scoring;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getApplicationId() { return applicationId; }
    public void setApplicationId(long applicationId) { this.applicationId = applicationId; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Borrower getBorrower() { return borrower; }
    public void setBorrower(Borrower borrower) { this.borrower = borrower; }

    public LoanApplication getApplication() { return application; }
    public void setApplication(LoanApplication application) { this.application = application; }

    public Form2501 getForm2501() { return form2501; }
    public void setForm2501(Form2501 form2501) { this.form2501 = form2501; }

    public void aggregate(PaymentSchedule schedule, DebtBurden pdn, ScoringResult scoring) {
        CreditStateReport defaultState = new CreditStateReport();
        if (schedule != null && schedule.getLoanAmount() != null) {
            defaultState.setOutstandingPrincipal(schedule.getLoanAmount());
            defaultState.setAccruedInterest(0.0);
            defaultState.setOverdueAmount(0.0);
            defaultState.setStatus("На рассмотрении");
        }
        aggregate(schedule, pdn, defaultState, scoring);
    }

    public void aggregate(PaymentSchedule schedule, DebtBurden pdn, CreditStateReport state, ScoringResult scoring) {
        this.schedule = schedule;
        this.pdn = pdn;
        this.state = state;
        this.scoring = scoring;
    }

    public File exportPDF() {
        File out = new File("final_report_" + System.currentTimeMillis() + ".pdf");
        return exportPDF(out);
    }

    public File exportPDF(File targetFile) {
        if (targetFile == null) {
            throw new IllegalArgumentException("Не указан файл для экспорта PDF");
        }
        try {
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }

            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, new FileOutputStream(targetFile));
            document.open();

            BaseFont baseFont = CYRILLIC_FONT;
            Font titleFont = new Font(baseFont, 15, Font.BOLD);
            Font sectionFont = new Font(baseFont, 11, Font.BOLD);
            Font regularFont = new Font(baseFont, 9.5f, Font.NORMAL);
            Font boldFont = new Font(baseFont, 9.5f, Font.BOLD);
            Font warnFont = new Font(baseFont, 9.5f, Font.BOLD, new Color(180, 30, 30));

            SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");

            Paragraph title = new Paragraph("ИТОГОВЫЙ АНАЛИТИЧЕСКИЙ ОТЧЁТ «КРЕДИТ-АНАЛИТИК»", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            Paragraph meta = new Paragraph("Дата формирования: " + df.format(createdAt != null ? createdAt : new Date()), regularFont);
            meta.setSpacingAfter(8);
            document.add(meta);

            if (borrower != null) {
                document.add(new Paragraph("1. Сведения о заёмщике", sectionFont));
                document.add(new Paragraph("ФИО: " + safe(borrower.getFullName()), regularFont));
                document.add(new Paragraph("Идентификационный номер: " + safe(borrower.getIdNumber()), regularFont));
                document.add(new Paragraph("Гражданство: " + safe(borrower.getCitizenship()), regularFont));
                document.add(new Paragraph("Документ: " + safe(borrower.getDocument()), regularFont));
                document.add(new Paragraph("Адрес: " + safe(borrower.getAddress()), regularFont));
                document.add(new Paragraph(String.format("Среднемесячный доход: %.2f",
                        borrower.getIncome() != null ? borrower.getIncome() : 0.0), regularFont));
                document.add(new Paragraph(" ", regularFont));
            }

            if (schedule != null) {
                document.add(new Paragraph("2. Параметры кредита и итоговые суммы", sectionFont));
                document.add(new Paragraph(String.format("Сумма кредита: %.2f | Срок: %d мес. | Ставка: %.2f%%",
                        val(schedule.getLoanAmount()),
                        schedule.getTermMonths() != null ? schedule.getTermMonths() : 0,
                        val(schedule.getInterestRate())), regularFont));
                document.add(new Paragraph("Способ возврата: " + safe(schedule.getMethod()), regularFont));
                document.add(new Paragraph(String.format("Ежемесячный платёж: %.2f | Общая сумма выплат: %.2f",
                        val(schedule.getMonthlyPayment()), val(schedule.getTotalAmount())), regularFont));
                document.add(new Paragraph(" ", regularFont));
            }

            if (pdn != null) {
                document.add(new Paragraph("3. Показатель долговой нагрузки (ПДН)", sectionFont));
                document.add(new Paragraph(String.format(
                        "ПДН: %.2f%% (действ. платежи: %.2f, новый кредит: %.2f, порог: %.2f%%)",
                        val(pdn.getValue()), val(pdn.getExistingMonthlyPayments()),
                        val(pdn.getNewLoanMonthlyPayment()), val(pdn.getThresholdPercent())), regularFont));
                if (pdn.isThresholdExceeded() && pdn.getWarningMessage() != null) {
                    document.add(new Paragraph(pdn.getWarningMessage(), warnFont));
                }
                document.add(new Paragraph(" ", regularFont));
            }

            if (scoring != null) {
                document.add(new Paragraph("4. Скоринговая оценка (горизонт 12 мес.)", sectionFont));
                document.add(new Paragraph(String.format("Скоринговый балл: %d | Категория риска: %s",
                        scoring.getScore() != null ? scoring.getScore() : 0,
                        safe(scoring.getRiskCategory())), boldFont));
                if (scoring.getFactors() != null) {
                    for (String f : scoring.getFactors()) {
                        document.add(new Paragraph(" • " + f, regularFont));
                    }
                }
                document.add(new Paragraph(" ", regularFont));
            }

            if (state != null) {
                document.add(new Paragraph("5. Текущее состояние кредита", sectionFont));
                document.add(new Paragraph(String.format(
                        "Статус: %s | Остаток ОД: %.2f | Начисленные %%: %.2f | Просрочка: %.2f (%d дн.)",
                        safe(state.getStatus()), val(state.getOutstandingPrincipal()),
                        val(state.getAccruedInterest()), val(state.getOverdue()),
                        state.getOverdueDays() != null ? state.getOverdueDays() : 0), regularFont));
                document.add(new Paragraph(" ", regularFont));
            }

            if (schedule != null && schedule.getPayments() != null && !schedule.getPayments().isEmpty()) {
                document.add(new Paragraph("6. График платежей (база 30/360)", sectionFont));
                Paragraph spacer = new Paragraph(" ");
                spacer.setSpacingAfter(4);
                document.add(spacer);

                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{0.8f, 1.8f, 1.8f, 1.6f, 1.8f, 2.0f});

                String[] headers = {"№", "Дата", "Основной долг", "Проценты", "Итого платёж", "Остаток долга"};
                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                    cell.setBackgroundColor(new Color(235, 240, 248));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setPadding(4);
                    table.addCell(cell);
                }

                List<Payment> payments = schedule.getPayments();
                for (int i = 0; i < payments.size(); i++) {
                    Payment p = payments.get(i);
                    table.addCell(new Phrase(String.valueOf(p.getPeriodNumber() > 0 ? p.getPeriodNumber() : (i + 1)), regularFont));
                    table.addCell(new Phrase(p.getDate() != null ? df.format(p.getDate()) : "", regularFont));
                    table.addCell(new Phrase(String.format("%.2f", val(p.getPrincipalAmount())), regularFont));
                    table.addCell(new Phrase(String.format("%.2f", val(p.getInterestAmount())), regularFont));
                    table.addCell(new Phrase(String.format("%.2f", val(p.getTotalAmount())), regularFont));
                    table.addCell(new Phrase(String.format("%.2f", val(p.getRemainingBalance())), regularFont));
                }
                document.add(table);
            }

            document.close();
            return targetFile;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте PDF: " + e.getMessage(), e);
        }
    }

    private static final BaseFont CYRILLIC_FONT = loadCyrillicBaseFont();

    private static BaseFont loadCyrillicBaseFont() {
        try (InputStream is = FinalReport.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is == null) {
                throw new IllegalStateException("Шрифт /fonts/DejaVuSans.ttf не найден в classpath");
            }

            Path tmp = Files.createTempFile("DejaVuSans", ".ttf");
            Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().deleteOnExit();

            return BaseFont.createFont(
                    tmp.toAbsolutePath().toString(),
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED
            );
        } catch (Exception e) {
            throw new RuntimeException("Не удалось загрузить шрифт для PDF", e);
        }
    }

    public File exportXML() {
        File out = new File("final_report_" + System.currentTimeMillis() + ".xml");
        return exportXML(out);
    }

    public File exportXML(File targetFile) {
        if (targetFile == null) {
            throw new IllegalArgumentException("Не указан файл для экспорта XML");
        }
        try {
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document doc = builder.newDocument();

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            org.w3c.dom.Element root = doc.createElement("FinalReport");
            root.setAttribute("applicationId", String.valueOf(applicationId));
            root.setAttribute("createdAt", df.format(createdAt != null ? createdAt : new Date()));
            doc.appendChild(root);

            if (borrower != null) {
                org.w3c.dom.Element bEl = doc.createElement("Borrower");
                appendChild(doc, bEl, "fullName", safe(borrower.getFullName()));
                appendChild(doc, bEl, "idNumber", safe(borrower.getIdNumber()));
                appendChild(doc, bEl, "citizenship", safe(borrower.getCitizenship()));
                appendChild(doc, bEl, "document", safe(borrower.getDocument()));
                appendChild(doc, bEl, "address", safe(borrower.getAddress()));
                appendChild(doc, bEl, "monthlyIncome", String.format(java.util.Locale.US, "%.2f", val(borrower.getIncome())));
                root.appendChild(bEl);
            }

            if (pdn != null) {
                org.w3c.dom.Element pEl = doc.createElement("DebtBurden");
                appendChild(doc, pEl, "valuePercent", String.format(java.util.Locale.US, "%.2f", val(pdn.getValue())));
                appendChild(doc, pEl, "existingMonthlyPayments", String.format(java.util.Locale.US, "%.2f", val(pdn.getExistingMonthlyPayments())));
                appendChild(doc, pEl, "newLoanMonthlyPayment", String.format(java.util.Locale.US, "%.2f", val(pdn.getNewLoanMonthlyPayment())));
                appendChild(doc, pEl, "thresholdPercent", String.format(java.util.Locale.US, "%.2f", val(pdn.getThresholdPercent())));
                appendChild(doc, pEl, "thresholdExceeded", String.valueOf(pdn.isThresholdExceeded()));
                root.appendChild(pEl);
            }

            if (scoring != null) {
                org.w3c.dom.Element sEl = doc.createElement("ScoringResult");
                appendChild(doc, sEl, "score", String.valueOf(scoring.getScore() != null ? scoring.getScore() : 0));
                appendChild(doc, sEl, "riskCategory", safe(scoring.getRiskCategory()));
                appendChild(doc, sEl, "incompleteData", String.valueOf(scoring.isIncompleteData()));
                root.appendChild(sEl);
            }

            if (state != null) {
                org.w3c.dom.Element stEl = doc.createElement("CreditStateReport");
                appendChild(doc, stEl, "status", safe(state.getStatus()));
                appendChild(doc, stEl, "outstandingPrincipal", String.format(java.util.Locale.US, "%.2f", val(state.getOutstandingPrincipal())));
                appendChild(doc, stEl, "accruedInterest", String.format(java.util.Locale.US, "%.2f", val(state.getAccruedInterest())));
                appendChild(doc, stEl, "overdueAmount", String.format(java.util.Locale.US, "%.2f", val(state.getOverdue())));
                appendChild(doc, stEl, "overdueDays", String.valueOf(state.getOverdueDays() != null ? state.getOverdueDays() : 0));
                root.appendChild(stEl);
            }

            if (schedule != null) {
                org.w3c.dom.Element schEl = doc.createElement("PaymentSchedule");
                appendChild(doc, schEl, "loanAmount", String.format(java.util.Locale.US, "%.2f", val(schedule.getLoanAmount())));
                appendChild(doc, schEl, "termMonths", String.valueOf(schedule.getTermMonths() != null ? schedule.getTermMonths() : 0));
                appendChild(doc, schEl, "interestRate", String.format(java.util.Locale.US, "%.2f", val(schedule.getInterestRate())));
                appendChild(doc, schEl, "method", safe(schedule.getMethod()));
                appendChild(doc, schEl, "monthlyPayment", String.format(java.util.Locale.US, "%.2f", val(schedule.getMonthlyPayment())));
                appendChild(doc, schEl, "totalAmount", String.format(java.util.Locale.US, "%.2f", val(schedule.getTotalAmount())));

                org.w3c.dom.Element paymentsEl = doc.createElement("Payments");
                for (int i = 0; i < schedule.getPayments().size(); i++) {
                    Payment p = schedule.getPayments().get(i);
                    org.w3c.dom.Element payEl = doc.createElement("Payment");
                    payEl.setAttribute("number", String.valueOf(p.getPeriodNumber() > 0 ? p.getPeriodNumber() : (i + 1)));
                    appendChild(doc, payEl, "date", p.getDate() != null ? df.format(p.getDate()) : "");
                    appendChild(doc, payEl, "principal", String.format(java.util.Locale.US, "%.2f", val(p.getPrincipalAmount())));
                    appendChild(doc, payEl, "interest", String.format(java.util.Locale.US, "%.2f", val(p.getInterestAmount())));
                    appendChild(doc, payEl, "total", String.format(java.util.Locale.US, "%.2f", val(p.getTotalAmount())));
                    appendChild(doc, payEl, "remainingBalance", String.format(java.util.Locale.US, "%.2f", val(p.getRemainingBalance())));
                    paymentsEl.appendChild(payEl);
                }
                schEl.appendChild(paymentsEl);
                root.appendChild(schEl);
            }

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(targetFile));
            return targetFile;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при экспорте XML: " + e.getMessage(), e);
        }
    }

    private static void appendChild(org.w3c.dom.Document doc, Node parent, String tag, String text) {
        org.w3c.dom.Element el = doc.createElement(tag);
        el.setTextContent(text != null ? text : "");
        parent.appendChild(el);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double val(Double d) {
        return d == null ? 0.0 : d;
    }

    public void save() {
        try (Database db = new Database()) {
            db.SaveReport(this);
        }
    }

    public void save(Database db) {
        if (db != null) {
            db.SaveReport(this);
        }
    }

    public PaymentSchedule getSchedule() { return schedule; }
    public DebtBurden getPdn() { return pdn; }
    public CreditStateReport getState() { return state; }
    public ScoringResult getScoring() { return scoring; }
}
