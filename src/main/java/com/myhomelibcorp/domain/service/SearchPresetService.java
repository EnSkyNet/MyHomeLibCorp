package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.domain.model.SearchPreset;
import com.myhomelibcorp.infrastructure.database.BookCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Сервіс для роботи зі збереженими шаблонами пошуку.
 */
public final class SearchPresetService {

    private static final Logger logger = LoggerFactory.getLogger(SearchPresetService.class);
    private final BookCollection database;

    public SearchPresetService(BookCollection database) {
        this.database = database;
    }

    /**
     * Повертає всі збережені пресети.
     */
    public List<SearchPreset> getAllPresets() {
        return database.loadSearchPresets();
    }

    /**
     * Створює новий пресет пошуку.
     * @param name назва пресета (не може бути порожньою)
     * @param criteria критерії пошуку
     */
    public void createPreset(String name, SearchCriteria criteria) {
        if (name == null || name.strip().isEmpty()) {
            throw new IllegalArgumentException("Назва пресета не може бути порожньою");
        }
        database.saveSearchPreset(name, criteria);
        logger.info("Створено пресет пошуку: {}", name);
    }

    /**
     * Видаляє пресет за його ідентифікатором.
     */
    public void removePreset(long id) {
        database.deleteSearchPreset(id);
        logger.info("Видалено пресет пошуку з id={}", id);
    }
}