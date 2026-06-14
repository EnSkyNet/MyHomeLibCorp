package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Репозиторій для роботи з рецензіями (відгуками) на книги.
 */
public class SqliteReviewRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteReviewRepository.class);
    private final DatabaseManager dbManager;

    public SqliteReviewRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Отримує рецензію для книги за її ID.
     * @param bookId ідентифікатор книги
     * @return текст рецензії (порожній рядок, якщо рецензії немає)
     */
    public String getReview(long bookId) {
        String sql = "SELECT review FROM books WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String review = rs.getString("review");
                return review == null ? "" : review;
            }
        } catch (SQLException e) {
            logger.error("Помилка отримання рецензії для книги {}", bookId, e);
            throw new RuntimeException(e);
        }
        return "";
    }

    /**
     * Зберігає рецензію для книги.
     * @param bookId ідентифікатор книги
     * @param review текст рецензії
     */
    public void setReview(long bookId, String review) {
        String sql = "UPDATE books SET review = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, review == null ? "" : review);
            ps.setLong(2, bookId);
            ps.executeUpdate();
            logger.debug("Оновлено рецензію для книги {}", bookId);
        } catch (SQLException e) {
            logger.error("Помилка збереження рецензії для книги {}", bookId, e);
            throw new RuntimeException(e);
        }
    }
}