package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторій для пошуку книг з використанням FTS5.
 * Забезпечує регістронезалежний пошук завдяки токенізатору unicode61 та зберіганню тексту в нижньому регістрі.
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

    public List<Book> searchBooks(SearchCriteria criteria) {
        List<Book> result = new ArrayList<>();
        if (criteria == null) return result;

        String queryText = "";
        if (criteria.title() != null && !criteria.title().isBlank()) queryText = criteria.title();
        else if (criteria.author() != null && !criteria.author().isBlank()) queryText = criteria.author();
        else if (criteria.series() != null && !criteria.series().isBlank()) queryText = criteria.series();
        else if (criteria.keywords() != null && !criteria.keywords().isBlank()) queryText = criteria.keywords();

        if (queryText == null || queryText.isBlank()) {
            String sql = "SELECT * FROM books ORDER BY title COLLATE NOCASE LIMIT 1000;";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) result.add(mapResultSetToBook(rs));
            } catch (SQLException e) {
                logger.error("Помилка отримання всіх книг", e);
                throw new RuntimeException(e);
            }
            return result;
        }

        // Використовуємо FTS5 з регістронезалежним пошуком
        String sql = """
            SELECT b.* FROM books b
            JOIN books_fts fts ON b.id = fts.rowid
            WHERE books_fts MATCH ?
            ORDER BY rank
            LIMIT 1000;
        """;
        String ftsQuery = buildFtsQuery(queryText);
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, ftsQuery);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) result.add(mapResultSetToBook(rs));
            }
        } catch (SQLException e) {
            logger.error("Помилка FTS5 пошуку", e);
            return searchLike(queryText);
        }
        return result;
    }

    /**
     * Будує FTS5 запит: переводить у нижній регістр, додає * до кожного слова.
     */
    private String buildFtsQuery(String query) {
        String lowerQuery = query.toLowerCase();
        String[] words = lowerQuery.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(w).append("*");
        }
        return sb.toString();
    }

    private List<Book> searchLike(String query) {
        List<Book> result = new ArrayList<>();
        String sql = """
            SELECT * FROM books 
            WHERE title LIKE ? OR series LIKE ? OR keywords LIKE ? 
            ORDER BY title COLLATE NOCASE;
        """;
        String pattern = "%" + query.trim() + "%";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) result.add(mapResultSetToBook(rs));
        } catch (SQLException e) {
            logger.error("Помилка LIKE пошуку", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    public List<String> getDistinctLanguages() {
        List<String> languages = new ArrayList<>();
        String sql = "SELECT DISTINCT language FROM books WHERE language IS NOT NULL AND language != '' ORDER BY language COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) languages.add(rs.getString("language"));
        } catch (SQLException e) {
            logger.error("Помилка отримання списку мов", e);
            throw new RuntimeException(e);
        }
        return languages;
    }

    public List<Book> search(String query) {
        return searchBooks(new SearchCriteria(query, "", "", ""));
    }

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
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) result.add(mapResultSetToBook(rs));
        } catch (SQLException e) {
            logger.error("Помилка пошуку книг за автором", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        long bookId = rs.getLong("id");
        List<Author> authorsList = bookRepo.findAuthorsForBook(bookId);
        List<String> genreCodes = bookRepo.findGenreCodesForBook(bookId);
        List<String> genreNames = new ArrayList<>();
        for (String code : genreCodes) {
            genreNames.add(genreRepo.getGenreName(code));
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