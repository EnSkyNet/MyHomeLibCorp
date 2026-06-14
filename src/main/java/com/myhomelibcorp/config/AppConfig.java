package com.myhomelibcorp.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Глобальна конфігурація додатку.
 * Містить шляхи до директорій, налаштування БД тощо.
 */
public final class AppConfig {

    private static final String USER_HOME = System.getProperty("user.home");
    private static final String APP_DIR_NAME = ".myhomelibcorp";

    // Шлях до директорії налаштувань користувача
    public static final Path USER_CONFIG_DIR = Paths.get(USER_HOME, APP_DIR_NAME);

    // Шлях до файлу налаштувань (JSON)
    public static final Path SETTINGS_FILE = USER_CONFIG_DIR.resolve("settings.json");

    // Шлях до кешу обкладинок
    public static final Path COVERS_CACHE_DIR = USER_CONFIG_DIR.resolve("covers");

    // Шлях до індексу Lucene (якщо використовується)
    public static final Path LUCENE_INDEX_DIR = USER_CONFIG_DIR.resolve("lucene_index");

    // Ім'я файлу БД колекції за замовчуванням
    public static final String DEFAULT_DB_NAME = "library.db";

    // Ім'я системної БД (реєстр колекцій)
    public static final String SYSTEM_DB_NAME = "system.db";

    private AppConfig() {} // заборона створення екземплярів
}