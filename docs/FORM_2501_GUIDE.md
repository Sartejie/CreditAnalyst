# Справочник по структуре входного файла Формы 2501

Система поддерживает импорт кредитной истории (Постановление Правления НБ РБ от 22.06.2018 № 291) в форматах **XML** и **JSON**.

## 1. Структура полей

### Субъект кредитной истории (`Borrower`)
- `lastName`, `firstName`, `middleName` (или объединённое `fullName`) — ФИО;
- `citizenship` — гражданство;
- `idNumber` — идентификационный номер;
- `birthDate` — дата рождения (`yyyy-MM-dd` или `dd.MM.yyyy`);
- `documentType`, `documentSeriesNumber`, `documentIssueDate` (или `document`) — документ, удостоверяющий личность;
- `country`, `region`, `district`, `locality` (или `address`) — место жительства;
- `monthlyIncome` — среднемесячный доход.

### Кредитные сделки (`CreditDeal` / `creditDeals`)
- `contractNumber` — номер кредитного договора *(обязательно)*;
- `dealType` — вид кредитной сделки;
- `creditType` — вид кредита;
- `conclusionDate` — дата заключения;
- `amount` — сумма кредита *(> 0)*;
- `currency` — наименование валюты (например, `BYN`, `USD`, `EUR`);
- `term` — срок возврата в месяцах;
- `renewalTerm` — срок возобновляемости линии (мес.);
- `repaymentDate` — срок возврата (должен быть позже `conclusionDate`);
- `security` — способ обеспечения исполнения обязательств;
- `riskGroup` — классификация актива (группа риска);
- `relevanceDate` — дата актуальности сведений;
- `overdueDays` — длительность просрочки (дней);
- `overdueAmount` — сумма просроченной задолженности;
- `monthlyPayment` — ежемесячный платёж (для расчёта ПДН; если не указан, оценивается как `amount / term`).

### Сведения об овердрафте (`Overdraft` / `overdrafts`)
- `contractDate` — дата заключения договора овердрафта;
- `limitAmount` — лимит овердрафта;
- `limitSetDate` — дата установления лимита.

## 2. Готовые примеры файлов
- XML: [form2501_sample.xml](file:///d:/INT/CreditAnalyst/examples/form2501_sample.xml)
- JSON: [form2501_sample.json](file:///d:/INT/CreditAnalyst/examples/form2501_sample.json)
