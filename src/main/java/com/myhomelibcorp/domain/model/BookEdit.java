package com.myhomelibcorp.domain.model;

public record BookEdit(
        String title,
        String series,
        Integer sequenceNumber,
        String language,
        String keywords,
        String annotation,
        int rate,
        int progress
) {
    public BookEdit {
        title = title != null ? title.trim() : "";
        series = series != null ? series.trim() : "";
        language = language != null ? language.trim() : "";
        keywords = keywords != null ? keywords.trim() : "";
        annotation = annotation != null ? annotation.trim() : "";
        if (sequenceNumber == null) sequenceNumber = 0;
    }
}