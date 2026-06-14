package com.myhomelibcorp.domain.model;

/**
 * Збережений шаблон пошуку.
 */
public record SearchPreset(
        long presetId,
        String name,
        SearchCriteria criteria
) {
    public SearchPreset {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Назва пресету пошуку не може бути порожньою");
        }
        if (criteria == null) {
            criteria = SearchCriteria.empty();
        }
    }

    public long id() {
        return presetId;
    }
}