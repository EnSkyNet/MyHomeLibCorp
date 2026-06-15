package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteGenreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GenreListImporter {
    private static final Logger logger = LoggerFactory.getLogger(GenreListImporter.class);
    private final DatabaseManager dbManager;
    private final SqliteGenreRepository genreRepo;
    public GenreListImporter(DatabaseManager dbManager, SqliteGenreRepository genreRepo) { this.dbManager = dbManager; this.genreRepo = genreRepo; }
    public void importGenresFromFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) { logger.warn("Файл жанрів не знайдено: {}", filePath); return; }
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int space = line.indexOf(' ');
                if (space < 0) continue;
                String after = line.substring(space + 1).trim();
                if (after.contains(";")) {
                    String[] parts = after.split(";", 2);
                    if (parts.length == 2) genreRepo.saveGenre(parts[0].trim(), parts[1].trim());
                } else {
                    genreRepo.saveGenre(line.substring(0, space).trim(), after);
                }
            }
            logger.info("Жанри успішно імпортовано з {}", filePath);
        } catch (Exception e) { logger.error("Помилка імпорту жанрів", e); }
    }
}