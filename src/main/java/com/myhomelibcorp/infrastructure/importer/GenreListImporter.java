package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteGenreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Імпортер списку жанрів з текстового файлу.
 * Формат: код назва   або   код;назва
 */
public class GenreListImporter {

    private static final Logger logger = LoggerFactory.getLogger(GenreListImporter.class);
    private final DatabaseManager dbManager;
    private final SqliteGenreRepository genreRepo;

    public GenreListImporter(DatabaseManager dbManager, SqliteGenreRepository genreRepo) {
        this.dbManager = dbManager;
        this.genreRepo = genreRepo;
    }

    public void importGenresFromFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            logger.warn("Файл жанрів не знайдено: {}", filePath);
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int firstSpace = line.indexOf(' ');
                if (firstSpace < 0) continue;

                String afterSpace = line.substring(firstSpace + 1).trim();
                if (afterSpace.contains(";")) {
                    String[] parts = afterSpace.split(";", 2);
                    if (parts.length == 2) {
                        String code = parts[0].trim();
                        String name = parts[1].trim();
                        if (!code.isEmpty() && !name.isEmpty()) {
                            genreRepo.saveGenre(code, name);
                        }
                    }
                } else {
                    String code = line.substring(0, firstSpace).trim();
                    String name = afterSpace;
                    genreRepo.saveGenre(code, name);
                }
            }
            logger.info("Жанри успішно імпортовано з {}", filePath);
        } catch (Exception e) {
            logger.error("Помилка імпорту жанрів", e);
        }
    }
}