package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SearchRepository {
    private static final Logger logger = LoggerFactory.getLogger(SearchRepository.class);
    private final DatabaseManager dbManager;
    private final SqliteBookRepository bookRepo;

    public SearchRepository(DatabaseManager dbManager) { this.dbManager = dbManager; this.bookRepo = new SqliteBookRepository(dbManager); }
    public List<Book> searchBooks(SearchCriteria criteria) {
        List<Book> result = new ArrayList<>();
        if (criteria == null) return result;
        String query = (criteria.title() != null && !criteria.title().isBlank()) ? criteria.title() : "";
        if (query.isBlank()) return bookRepo.findAll();
        String ftsQuery = buildFtsQuery(query);
        String sql = "SELECT id FROM books JOIN books_fts ON books.id = books_fts.rowid WHERE books_fts MATCH ? ORDER BY rank LIMIT 1000";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ftsQuery);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) bookRepo.findById(rs.getLong("id")).ifPresent(result::add);
        } catch (SQLException e) { logger.error("FTS пошук не вдався", e); return fallbackSearch(query); }
        logger.debug("FTS запит: {}", ftsQuery);
        return result;
    }
    private String buildFtsQuery(String query) {
        // Переводимо весь запит у нижній регістр і видаляємо зайві пробіли
        String lowerQuery = query.toLowerCase().trim();
        // Розбиваємо на слова
        String[] words = lowerQuery.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (sb.length() > 0) sb.append(" ");
            // Додаємо * для пошуку за префіксом (якщо слово не дуже коротке)
            if (w.length() >= 2) {
                sb.append(w).append("*");
            } else {
                sb.append(w);
            }
        }
        return sb.toString();
    }
    private List<Book> fallbackSearch(String q) {
        List<Book> result = new ArrayList<>();
        String pattern = "%" + q.toLowerCase() + "%";
        String sql = "SELECT id FROM books WHERE lower(title) LIKE ? OR lower(series) LIKE ? OR lower(keywords) LIKE ? ORDER BY title COLLATE NOCASE";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) bookRepo.findById(rs.getLong("id")).ifPresent(result::add);
        } catch (SQLException e) { logger.error("Fallback пошук не вдався", e); }
        return result;
    }
    public List<String> getDistinctLanguages() {
        List<String> langs = new ArrayList<>();
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT language FROM books WHERE language NOT NULL AND language != '' ORDER BY language COLLATE NOCASE")) {
            while (rs.next()) langs.add(rs.getString(1));
        } catch (SQLException e) { logger.error("Помилка отримання мов", e); }
        return langs;
    }
}