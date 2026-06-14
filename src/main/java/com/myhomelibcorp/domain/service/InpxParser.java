package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Високопродуктивний потоковий парсер індексних файлів .INPX.
 * Читає ZIP-архів, знаходить файли .inp та обробляє їх рядок за рядком.
 */
public class InpxParser {

    private static final Logger logger = LoggerFactory.getLogger(InpxParser.class);

    /**
     * Парсить INPX-потік і викликає batchConsumer для кожної партії книг.
     * @param inpxInputStream вхідний потік INPX (ZIP)
     * @param batchSize розмір партії
     * @param batchConsumer споживач, який отримує партії книг
     * @return список всіх книг (можна проігнорувати, якщо використовується batchConsumer)
     */
    public List<Book> parseInpxStream(InputStream inpxInputStream, int batchSize, Consumer<List<Book>> batchConsumer) throws Exception {
        List<Book> currentBatch = new ArrayList<>(batchSize);

        try (ZipInputStream zis = new ZipInputStream(inpxInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".inp")) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;

                        Book book = parseInpxLine(line, entry.getName());
                        if (book != null) {
                            currentBatch.add(book);
                            if (currentBatch.size() >= batchSize) {
                                batchConsumer.accept(new ArrayList<>(currentBatch));
                                currentBatch.clear();
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        // остання неповна партія
        if (!currentBatch.isEmpty()) {
            batchConsumer.accept(currentBatch);
        }

        return Collections.emptyList();
    }

    /**
     * Парсить один рядок INPX-файлу.
     * Формат: автори|жанри|назва|серія|номер|ім'я_файлу|розмір|видалена|мова|... (до 12+ полів)
     */
    private Book parseInpxLine(String line, String sourceInpName) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 9) {
                logger.warn("Недостатньо полів у рядку INPX: {}", line);
                return null;
            }

            String rawAuthors = parts[0].trim();
            String rawGenres = parts[1].trim();
            String title = parts[2].trim();
            String series = parts[3].trim();

            Integer sequenceNumber = 0;
            if (!parts[4].isBlank()) {
                try {
                    sequenceNumber = Integer.valueOf(parts[4].trim());
                } catch (NumberFormatException ignored) {}
            }

            String fileName = parts[5].trim();

            long fileSize = 0;
            if (!parts[6].isBlank()) {
                try {
                    fileSize = Long.parseLong(parts[6].trim());
                } catch (NumberFormatException ignored) {}
            }

            boolean isDeleted = parts.length > 7 && "1".equals(parts[7].trim());
            if (isDeleted) return null; // пропускаємо видалені записи

            String language = parts.length > 8 ? parts[8].trim() : "uk";

            // Список авторів
            List<Author> authors = new ArrayList<>();
            if (rawAuthors.isEmpty()) {
                long defAuthorId = Math.abs((long) "Невідомий автор".hashCode());
                authors.add(new Author(defAuthorId, "Невідомий автор", "", ""));
            } else {
                for (String authorName : rawAuthors.split(":")) {
                    String cleanName = authorName.trim();
                    if (!cleanName.isBlank()) {
                        long authorId = Math.abs((long) cleanName.hashCode());
                        authors.add(new Author(authorId, cleanName, "", ""));
                    }
                }
            }

            // Жанри
            List<String> genres = rawGenres.isEmpty() ? List.of("unknown") : List.of(rawGenres.split(":"));

            long uniqueLongId = Math.abs((long) (fileName + title).hashCode());
            String containerFolder = sourceInpName.toLowerCase().replace(".inp", ".zip");
            String archiveEntryName = fileName + ".fb2";

            return new Book(
                    uniqueLongId,
                    title,
                    authors,
                    genres,
                    series,
                    sequenceNumber,
                    language,
                    archiveEntryName,
                    containerFolder,
                    fileName,
                    fileSize,
                    "",
                    "Книга завантажена через індекс колекції " + sourceInpName,
                    0,
                    0,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            logger.error("Помилка парсингу рядка індексу: {}", line, e);
            return null;
        }
    }
}