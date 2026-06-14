package com.myhomelibcorp.common.exception;

/**
 * Базовий клас для всіх винятків програми.
 * Всі специфічні винятки успадковуються від нього.
 */
public class AppException extends Exception {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}