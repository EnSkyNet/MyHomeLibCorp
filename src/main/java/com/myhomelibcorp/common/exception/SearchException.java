package com.myhomelibcorp.common.exception;

/**
 * Виняток, що виникає при виконанні пошуку (помилки індексації, некоректний запит).
 */
public class SearchException extends AppException {

    public SearchException(String message) {
        super(message);
    }

    public SearchException(String message, Throwable cause) {
        super(message, cause);
    }
}