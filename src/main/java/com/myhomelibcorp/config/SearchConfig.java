package com.myhomelibcorp.config;

/**
 * Налаштування пошуку (FTS5, Lucene тощо).
 */
public final class SearchConfig {

    // Тип пошукового рушія: "fts5" або "lucene"
    public static final String SEARCH_ENGINE_TYPE = "fts5";

    // Максимальна кількість результатів пошуку за замовчуванням
    public static final int DEFAULT_MAX_RESULTS = 1000;

    // Мінімальна довжина слова для індексації (FTS5)
    public static final int FTS_MIN_TOKEN_LEN = 2;

    private SearchConfig() {}
}