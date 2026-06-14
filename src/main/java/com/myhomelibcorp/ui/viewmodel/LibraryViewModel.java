package com.myhomelibcorp.ui.viewmodel;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * ViewModel для головного вікна програми.
 * Відповідає за завантаження даних з репозиторію та надання їх у вигляді ObservableList для JavaFX TableView.
 */
public class LibraryViewModel {

    private static final Logger logger = LoggerFactory.getLogger(LibraryViewModel.class);

    private final SqliteBookRepository bookRepository;
    private final Fb2Importer fb2Importer;
    private final ObservableList<Book> booksList = FXCollections.observableArrayList();

    /**
     * Конструктор ViewModel.
     * @param bookRepository репозиторій для роботи з книгами
     * @param fb2Importer імпортер FB2-файлів (поки не використовується в цьому класі)
     */
    public LibraryViewModel(SqliteBookRepository bookRepository, Fb2Importer fb2Importer) {
        this.bookRepository = bookRepository;
        this.fb2Importer = fb2Importer;
    }

    /**
     * Завантажує всі книги з бази даних в ObservableList.
     * У разі помилки логує її та очищує список.
     */
    public void loadAllBooks() {
        if (bookRepository == null) {
            logger.error("Спроба завантаження книг, але BookRepository є null");
            return;
        }
        try {
            List<Book> allBooks = bookRepository.findAll();
            if (allBooks != null) {
                booksList.setAll(allBooks);
                logger.info("Завантажено {} книг", allBooks.size());
            } else {
                booksList.clear();
                logger.warn("Метод findAll() повернув null, список очищено");
            }
        } catch (Exception e) {
            logger.error("Помилка синхронізації ViewModel з репозиторієм", e);
            booksList.clear();
        }
    }

    /**
     * Повертає ObservableList книг для прив'язки до TableView.
     * @return список книг
     */
    public ObservableList<Book> getBooksList() {
        return booksList;
    }

    /**
     * Отримує репозиторій книг (може знадобитися для деяких операцій).
     * @return екземпляр BookRepository
     */
    public SqliteBookRepository getBookRepository() {
        return bookRepository;
    }

    /**
     * Отримує імпортер FB2 (може знадобитися для імпорту).
     * @return екземпляр Fb2Importer
     */
    public Fb2Importer getFb2Importer() {
        return fb2Importer;
    }
}