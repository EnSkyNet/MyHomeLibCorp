package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Fb2Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Імпортер книг з INPX-файлів (формат індексів бібліотек).
 */
public class InpxImporter {

    private static final Logger logger = LoggerFactory.getLogger(InpxImporter.class);

    /**
     * Парсить INPX-файл (звичайний текстовий, не ZIP) і повертає список книг.
     * @param inputStream потік даних INPX
     * @param parentArchivePath шлях до батьківського архіву (ZIP)
     * @return список книг Fb2Book
     */
    public List<Fb2Book> parseInpFolder(InputStream inputStream, Path parentArchivePath) {
        List<Fb2Book> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("[\\|" + ((char) 4) + "]");
                if (parts.length < 8) continue;

                // Парсинг авторів (поле 0)
                List<Author> authors = new ArrayList<>();
                String authorsStr = parts[0].trim();
                if (!authorsStr.isEmpty()) {
                    for (String name : authorsStr.split(":")) {
                        String clean = name.trim();
                        if (!clean.isEmpty()) {
                            long id = Math.abs(clean.hashCode());
                            authors.add(new Author(id, clean));
                        }
                    }
                }
                if (authors.isEmpty()) {
                    authors.add(new Author(0, "Невідомий автор"));
                }

                // Жанри (поле 1)
                List<String> genres = List.of(parts[1].split(":"));

                String title = parts[2].trim();
                String series = parts[3].trim();
                int seqNumber = parts[4].isEmpty() ? 0 : Integer.parseInt(parts[4].trim());
                String fileName = parts[5].trim();
                long fileSize = parts[6].isEmpty() ? 0 : Long.parseLong(parts[6].trim());
                // parts[7] – ознака видалення (ігноруємо)
                String language = (parts.length > 11) ? parts[11].trim() : "ru";
                String keywords = (parts.length > 12) ? parts[12].trim() : "";
                String annotation = (parts.length > 13) ? parts[13].trim() : "";

                Fb2Book book = new Fb2Book(
                        title, authors, genres, series, seqNumber, language,
                        parentArchivePath, fileName, fileSize, keywords, annotation
                );
                list.add(book);
            }
        } catch (Exception e) {
            logger.error("Помилка парсингу INPX", e);
        }
        return list;
    }
}