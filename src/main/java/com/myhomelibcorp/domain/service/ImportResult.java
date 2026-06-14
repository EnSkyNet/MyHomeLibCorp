package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Fb2Book;

import java.util.List;

/**
 * Результат операції імпорту книг.
 * @param scannedBooks список книг, отриманих під час сканування
 * @param savedBooks кількість книг, які було збережено в БД
 */
public record ImportResult(List<Fb2Book> scannedBooks, int savedBooks) {
}