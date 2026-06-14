package com.myhomelibcorp.domain.repository;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    List<Book> findAll();
    Optional<Book> findById(long id);
    void save(Book book);
    void update(Book book);
    void deleteById(long id);
    List<Book> findByTitle(String titlePattern);
    List<Book> findByAuthor(String authorName);
    void updateRate(long bookId, int rate);
    void updateProgress(long bookId, int progress);
    void saveAuthorsForBook(long bookId, List<Author> authors);
    void saveGenresForBook(long bookId, List<String> genreCodes);
    List<Author> findAuthorsForBook(long bookId);
    List<String> findGenreCodesForBook(long bookId);
}