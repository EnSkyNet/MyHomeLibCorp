package com.myhomelibcorp.domain.model;

/**
 * Критерії пошуку книг. Використовується як для простого, так і для розширеного пошуку.
 */
public record SearchCriteria(
        String title,
        String author,
        String genre,
        String series,
        String language,
        Integer rateFrom,
        Integer rateTo,
        Integer progressFrom,
        Integer progressTo,
        Long sizeFrom,
        Long sizeTo,
        String fileFolder,
        String fileArchive,
        String keywords,
        String annotation,
        String group
) {
    public SearchCriteria {
        title = title != null ? title.trim() : "";
        author = author != null ? author.trim() : "";
        genre = genre != null ? genre.trim() : "";
        series = series != null ? series.trim() : "";
        language = language != null ? language.trim() : "";
        fileFolder = fileFolder != null ? fileFolder.trim() : "";
        fileArchive = fileArchive != null ? fileArchive.trim() : "";
        keywords = keywords != null ? keywords.trim() : "";
        annotation = annotation != null ? annotation.trim() : "";
        group = group != null ? group.trim() : "";
    }

    // Спрощені конструктори для базового пошуку
    public SearchCriteria(String title, String author, String genre, String series, String language) {
        this(title, author, genre, series, language, null, null, null, null, null, null, "", "", "", "", "");
    }

    public SearchCriteria(String title, String author, String genre, String series) {
        this(title, author, genre, series, "", null, null, null, null, null, null, "", "", "", "", "");
    }

    public static SearchCriteria empty() {
        return new SearchCriteria("", "", "", "", "", null, null, null, null, null, null, "", "", "", "", "");
    }
}