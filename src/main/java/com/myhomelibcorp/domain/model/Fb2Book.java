package com.myhomelibcorp.domain.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель даних, отриманих при парсингу FB2-файлу.
 */
public record Fb2Book(
        String title,
        List<Author> authors,
        List<String> genres,
        String series,
        Integer sequenceNumber,
        String language,
        Path sourcePath,
        String archiveEntry,
        long fileSize,
        String keywords,
        String annotation
) {
    public Fb2Book {
        title = title != null ? title.trim() : "Без назви";
        authors = authors != null ? new ArrayList<>(authors) : new ArrayList<>();
        genres = genres != null ? new ArrayList<>(genres) : new ArrayList<>();
        series = series != null ? series.trim() : "";
        language = language != null ? language.trim().toLowerCase() : "ru";
        archiveEntry = archiveEntry != null ? archiveEntry.trim() : "";
        keywords = keywords != null ? keywords.trim() : "";
        annotation = annotation != null ? annotation.trim() : "";
    }
}