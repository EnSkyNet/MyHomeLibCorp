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

public class InpxImporter {
    private static final Logger logger = LoggerFactory.getLogger(InpxImporter.class);
    public List<Fb2Book> parseInpFolder(InputStream inputStream, Path parentArchivePath) {
        List<Fb2Book> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split("[\\|" + ((char) 4) + "]");
                if (parts.length < 8) continue;
                List<Author> authors = new ArrayList<>();
                String authorsStr = parts[0].trim();
                if (!authorsStr.isEmpty()) {
                    for (String name : authorsStr.split(":")) { if (!name.trim().isEmpty()) authors.add(new Author(0, name.trim())); }
                }
                if (authors.isEmpty()) authors.add(new Author(0, "Невідомий автор"));
                List<String> genres = List.of(parts[1].split(":"));
                String title = parts[2].trim(), series = parts[3].trim();
                int seq = parts[4].isEmpty() ? 0 : Integer.parseInt(parts[4].trim());
                String fileName = parts[5].trim();
                long size = parts[6].isEmpty() ? 0 : Long.parseLong(parts[6].trim());
                String lang = (parts.length > 11) ? parts[11].trim() : "ru";
                String keywords = (parts.length > 12) ? parts[12].trim() : "";
                String annotation = (parts.length > 13) ? parts[13].trim() : "";
                Fb2Book book = new Fb2Book(title, authors, genres, series, seq, lang, parentArchivePath, fileName, size, keywords, annotation);
                list.add(book);
            }
        } catch (Exception e) { logger.error("Помилка парсингу INPX", e); }
        return list;
    }
}