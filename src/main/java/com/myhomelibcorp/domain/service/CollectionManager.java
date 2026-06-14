package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

/**
 * Сервіс управління життєвим циклом підключення до колекції (бази даних).
 * Відповідає за відкриття, закриття та створення нових колекцій.
 */
public class CollectionManager {

    private static final Logger logger = LoggerFactory.getLogger(CollectionManager.class);

    private DatabaseManager collectionDatabase;
    private Path currentCollectionPath;

    /**
     * Відкриває існуючу колекцію за шляхом.
     * @param path шлях до файлу бази даних (.db)
     */
    public void openCollection(Path path) {
        try {
            if (collectionDatabase != null) {
                closeCollection();
            }
            this.currentCollectionPath = path;
            this.collectionDatabase = new DatabaseManager(path);
            this.collectionDatabase.open();
            logger.info("Успішно підключено колекцію: {}", path.getFileName());
        } catch (SQLException e) {
            logger.error("Не вдалося відкрити базу даних: {}", path, e);
        }
    }

    /**
     * Закриває поточну активну колекцію.
     */
    public void closeCollection() {
        try {
            if (collectionDatabase != null) {
                collectionDatabase.close();
                collectionDatabase = null;
                currentCollectionPath = null;
                logger.info("Поточну колекцію вивантажено з пам'яті.");
            }
        } catch (SQLException e) {
            logger.error("Помилка при закритті підключення", e);
        }
    }

    /**
     * Створює нову порожню колекцію (файл бази даних).
     * Якщо файл вже існує, він буде перезаписаний.
     * @param path шлях для створення нового файлу .db
     */
    public void createNewCollection(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
            }
            openCollection(path);
            logger.info("Створено новий файл бази даних: {}", path);
        } catch (IOException e) {
            logger.error("Помилка створення файлу структури", e);
        }
    }

    /**
     * Повертає менеджер бази даних поточної колекції.
     */
    public DatabaseManager getCollectionDatabase() {
        return collectionDatabase;
    }

    /**
     * Альтернативний геттер (без префікса get) для зручності.
     */
    public DatabaseManager collectionDatabase() {
        return collectionDatabase;
    }

    /**
     * Повертає шлях до поточної колекції.
     */
    public Path getCurrentCollectionPath() {
        return currentCollectionPath;
    }
}