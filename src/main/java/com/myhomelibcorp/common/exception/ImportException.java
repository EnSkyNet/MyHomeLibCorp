package com.myhomelibcorp.common.exception;

/**
 * Виняток, що виникає при імпорті книг (некоректний формат, помилки читання файлу тощо).
 */
public class ImportException extends AppException {

    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}