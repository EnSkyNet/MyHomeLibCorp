package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

public class CollectionManager {
    private static final Logger logger = LoggerFactory.getLogger(CollectionManager.class);
    private DatabaseManager collectionDatabase;
    private Path currentCollectionPath;
    public void openCollection(Path path) {
        try {
            if (collectionDatabase != null) closeCollection();
            this.currentCollectionPath = path;
            this.collectionDatabase = new DatabaseManager(path);
            this.collectionDatabase.open();
            logger.info("Відкрито колекцію: {}", path.getFileName());
        } catch (SQLException e) { logger.error("Помилка відкриття колекції", e); }
    }
    public void closeCollection() {
        try {
            if (collectionDatabase != null) { collectionDatabase.close(); collectionDatabase = null; currentCollectionPath = null; }
        } catch (SQLException e) { logger.error("Помилка закриття колекції", e); }
    }
    public void createNewCollection(Path path) {
        try {
            if (Files.exists(path)) Files.delete(path);
            openCollection(path);
        } catch (Exception e) { logger.error("Помилка створення колекції", e); }
    }
    public DatabaseManager getCollectionDatabase() { return collectionDatabase; }
    public DatabaseManager collectionDatabase() { return collectionDatabase; }
    public Path getCurrentCollectionPath() { return currentCollectionPath; }
}