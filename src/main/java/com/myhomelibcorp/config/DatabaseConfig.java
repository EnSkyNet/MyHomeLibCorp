package com.myhomelibcorp.config;

/**
 * Параметри підключення до бази даних SQLite.
 */
public final class DatabaseConfig {

    // PRAGMA для оптимізації продуктивності
    public static final String JOURNAL_MODE = "WAL";
    public static final String SYNCHRONOUS = "OFF";
    public static final String CACHE_SIZE = "-64000"; // 64 МБ
    public static final boolean FOREIGN_KEYS = true;

    private DatabaseConfig() {}
}