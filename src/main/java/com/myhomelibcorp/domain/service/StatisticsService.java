package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.sqlite.SearchRepository;

/**
 * Сервіс для отримання статистичних даних про бібліотеку.
 */
public class StatisticsService {

    private final SearchRepository searchRepository;

    public StatisticsService(SearchRepository searchRepository) {
        this.searchRepository = searchRepository;
    }

    /**
     * Повертає загальну кількість книг у бібліотеці.
     */
    public long getTotalBooksCount() {
        // Отримуємо всі книги через пошук без критеріїв та рахуємо
        return searchRepository.searchBooks(SearchCriteria.empty()).size();
    }

    /**
     * Інформація про рушій бази даних (для відображення в інтерфейсі).
     */
    public String getDatabaseEngineInfo() {
        return "SQLite Core with Full-Text Search (FTS5) та WAL режим";
    }
}