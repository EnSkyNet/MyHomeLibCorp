package com.myhomelibcorp.infrastructure.database;

import com.myhomelibcorp.domain.model.*;
import java.nio.file.Path;
import java.util.List;

public interface BookCollection {
    void open(Path dbPath);
    void close();
    boolean isOpen();
    int getBooksCount();
    int getAuthorsCount();
    int getGenresCount();
    int importBooks(List<Fb2Book> books);
    void updateBookFields(long bookId, BookEdit editData);
    List<Book> findBooks(SearchCriteria criteria);
    List<Book> searchBooks(String keyword);
    List<Book> searchAdvanced(SearchCriteria criteria);
    List<String> listAuthors();
    List<String> listSeries();
    List<String> listGenres();
    String statistics();
    void importGenreList(List<?> genres, String lang);
    List<SearchPreset> loadSearchPresets();
    void saveSearchPreset(String name, SearchCriteria criteria);
    void deleteSearchPreset(long id);
    void executeRawSql(String sql);
    void beginTransaction();
    void commitTransaction();
    void rollbackTransaction();
}