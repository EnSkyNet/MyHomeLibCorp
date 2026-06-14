package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторій для пошуку книг з різними критеріями.
 * (Поки що використовує простий LIKE-пошук, у майбутньому буде замінено на FTS5 або Lucene.)
 */
public class SearchRepository {

    private static final Logger logger = LoggerFactory.getLogger(SearchRepository.class);
    private final DatabaseManager databaseManager;
    private final SqliteBookRepository bookRepo;
    private final SqliteGenreRepository genreRepo;

    public SearchRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.bookRepo = new SqliteBookRepository(databaseManager);
        this.genreRepo = new SqliteGenreRepository(databaseManager);
    }

    /**
     * Виконує пошук книг за заданими критеріями.
     */
    public List<Book> searchBooks(SearchCriteria criteria) {
        List<Book> result = new ArrayList<>();
        if (criteria == null) return result;

        String queryText = "";
        if (criteria.title() != null && !criteria.title().isBlank()) {
            queryText = criteria.title();
        } else if (criteria.author() != null && !criteria.author().isBlank()) {
            queryText = criteria.author();
        } else if (criteria.series() != null && !criteria.series().isBlank()) {
            queryText = criteria.series();
        }

        if (queryText.isBlank()) {
            // Якщо немає жодного критерію, повертаємо перші 1000 книг (ліміт для продуктивності)
            String sql = "SELECT * FROM books ORDER BY title COLLATE NOCASE LIMIT 1000;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToBook(rs));
                }
            } catch (SQLException e) {
                logger.error("Помилка виконання пошуку без критеріїв", e);
                throw new RuntimeException(e);
            }
            return result;
        }
        return search(queryText);
    }

    /**
     * Простий пошук за текстом у полях title, series, keywords.
     */
    public List<Book> search(String query) {
        List<Book> result = new ArrayList<>();
        String sql = """
            SELECT * FROM books 
            WHERE title LIKE ? OR series LIKE ? OR keywords LIKE ? 
            ORDER BY title COLLATE NOCASE;
        """;
        String searchPattern = "%" + query.trim() + "%";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка пошуку за запитом: {}", query, e);
            throw new RuntimeException(e);
        }
        return result;
    }

    /**
     * Повертає список унікальних мов книг.
     */
    public List<String> getDistinctLanguages() {
        List<String> languages = new ArrayList<>();
        String sql = "SELECT DISTINCT language FROM books WHERE language IS NOT NULL AND language != '' ORDER BY language COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                languages.add(rs.getString("language"));
            }
        } catch (SQLException e) {
            logger.error("Помилка отримання списку мов", e);
            throw new RuntimeException(e);
        }
        return languages;
    }

    /**
     * Пошук книг за ID автора.
     */
    public List<Book> findBooksByAuthorId(long authorId) {
        List<Book> result = new ArrayList<>();
        String sql = """
            SELECT b.* FROM books b
            JOIN book_authors ba ON b.id = ba.book_id
            WHERE ba.author_id = ?
            ORDER BY b.title COLLATE NOCASE;
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, authorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка пошуку книг за автором {}", authorId, e);
            throw new RuntimeException(e);
        }
        return result;
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        long bookId = rs.getLong("id");
        List<com.myhomelibcorp.domain.model.Author> authorsList = bookRepo.findAuthorsForBook(bookId);
        List<String> genreCodes = bookRepo.findGenreCodesForBook(bookId);
        List<String> genreNames = new ArrayList<>();
        for (String code : genreCodes) {
            String name = genreRepo.getGenreName(code);
            genreNames.add(name);
        }
        String dtStr = rs.getString("date_time");
        LocalDateTime bookDate = (dtStr == null || dtStr.isBlank()) ? LocalDateTime.now() : LocalDateTime.parse(dtStr);
        return new Book(
                bookId,
                rs.getString("title"),
                authorsList,
                genreNames,
                rs.getString("series"),
                rs.getInt("sequence_number"),
                rs.getString("language"),
                rs.getString("file_name"),
                rs.getString("folder"),
                rs.getString("archive_entry"),
                rs.getLong("file_size"),
                rs.getString("keywords"),
                rs.getString("annotation"),
                rs.getInt("rate"),
                rs.getInt("progress"),
                bookDate
        );
    }
}