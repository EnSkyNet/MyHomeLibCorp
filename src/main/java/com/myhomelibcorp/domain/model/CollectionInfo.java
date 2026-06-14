package com.myhomelibcorp.domain.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Інформація про зареєстровану колекцію (базу даних).
 */
public record CollectionInfo(
        long id,
        String displayName,
        Path databasePath,
        Path rootFolder,
        int dataVersion,
        CollectionType type,
        String notes,
        String user,
        String password,
        String url,
        String connectionScript,
        LocalDateTime creationDate
) {
    public String rootPathText() {
        return rootFolder == null ? "" : rootFolder.toString();
    }

    public String databasePathText() {
        return databasePath == null ? "" : databasePath.toString();
    }
}