package com.myhomelibcorp.infrastructure.database;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.BookEdit;
import com.myhomelibcorp.domain.model.Fb2Book;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.domain.model.SearchPreset;

import java.nio.file.Path;
import java.util.List;

/**
 * Абстракція для роботи з колекцією книг (базою даних).
 * Реалізується, наприклад, SQLite.
 */
public interface BookCollection {

    // --- Життєвий цикл ---
    void open(Path dbPath);
    void close();
    boolean isOpen();

    // --- Лічильники ---
    int getBooksCount();
    int getAuthorsCount();
    int getGenresCount();

    // --- Імпорт та оновлення ---
    int importBooks(List<Fb2Book> books);
    void updateBookFields(long bookId, BookEdit editData);
    List<Book> findBooks(SearchCriteria criteria);

    // --- Додаткові методи ---
    List<Book> searchBooks(String keyword);
    List<Book> searchAdvanced(SearchCriteria criteria);
    List<String> listAuthors();
    List<String> listSeries();
    List<String> listGenres();
    String statistics();

    void importGenreList(List<?> genres, String lang);

    // --- Збережені пошуки ---
    List<SearchPreset> loadSearchPresets();
    void saveSearchPreset(String name, SearchCriteria criteria);
    void deleteSearchPreset(long id);

    // --- Низькорівневі операції ---
    void executeRawSql(String sql);
    void beginTransaction();
    void commitTransaction();
    void rollbackTransaction();
}