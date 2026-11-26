package org.credit.model;

/**
 * Детализированная ошибка валидации конкретного поля формы или модели.
 */
public class ValidationError {
    private final String section;
    private final String field;
    private final String message;

    public ValidationError(String section, String field, String message) {
        this.section = section;
        this.field = field;
        this.message = message;
    }

    public String getSection() {
        return section;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format("[%s] Поле «%s»: %s", section, field, message);
    }
}
