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
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InpxParser {
    private static final Logger logger = LoggerFactory.getLogger(InpxParser.class);
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
        if (!currentBatch.isEmpty()) batchConsumer.accept(currentBatch);
        return List.of();
    }
    private Book parseInpxLine(String line, String sourceInpName) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 9) return null;
            String rawAuthors = parts[0].trim();
            String rawGenres = parts[1].trim();
            String title = parts[2].trim();
            String series = parts[3].trim();
            Integer seq = 0;
            if (!parts[4].isBlank()) try { seq = Integer.valueOf(parts[4].trim()); } catch (NumberFormatException e) {}
            String fileName = parts[5].trim();
            long fileSize = 0;
            if (!parts[6].isBlank()) try { fileSize = Long.parseLong(parts[6].trim()); } catch (NumberFormatException e) {}
            boolean isDeleted = parts.length > 7 && "1".equals(parts[7].trim());
            if (isDeleted) return null;
            String lang = (parts.length > 8) ? parts[8].trim() : "uk";
            List<Author> authors = new ArrayList<>();
            if (rawAuthors.isEmpty()) authors.add(new Author(Math.abs("Невідомий автор".hashCode()), "Невідомий автор", "", ""));
            else for (String an : rawAuthors.split(":")) { String cn = an.trim(); if (!cn.isBlank()) authors.add(new Author(Math.abs(cn.hashCode()), cn, "", "")); }
            List<String> genres = rawGenres.isEmpty() ? List.of("unknown") : List.of(rawGenres.split(":"));
            long uniqueId = Math.abs((fileName + title).hashCode());
            String containerFolder = sourceInpName.toLowerCase().replace(".inp", ".zip");
            String archiveEntryName = fileName + ".fb2";
            return new Book(uniqueId, title, authors, genres, series, seq, lang, archiveEntryName, containerFolder, fileName, fileSize, "", "Книга з індексу " + sourceInpName, 0, 0, LocalDateTime.now());
        } catch (Exception e) { logger.error("Помилка парсингу рядка INPX", e); return null; }
    }
}