package com.myhomelibcorp.common.exception;

/**
 * Виняток, що виникає при роботі з базою даних (SQL-помилки, проблеми підключення тощо).
 */
public class DatabaseException extends AppException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}