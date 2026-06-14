package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторій для роботи з авторами.
 */
public class SqliteAuthorRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteAuthorRepository.class);
    private final DatabaseManager databaseManager;

    public SqliteAuthorRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Повертає список всіх авторів, відсортованих за ПІБ.
     */
    public List<Author> findAll() {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT id, full_name, first_name, middle_name, last_name FROM authors ORDER BY full_name COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String fullName = rs.getString("full_name");
                if (fullName != null && !fullName.isBlank()) {
                    authors.add(new Author(rs.getLong("id"), fullName));
                } else {
                    authors.add(new Author(rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("middle_name"),
                            rs.getString("last_name")));
                }
            }
            logger.debug("Отримано {} авторів", authors.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання списку авторів", e);
            throw new RuntimeException(e);
        }
        return authors;
    }

    /**
     * Вставляє автора з вказаним ID та ім'ям (ігнорує, якщо вже існує).
     * @deprecated Використовуйте {@link #save(Author)}.
     */
    @Deprecated
    public void insertAuthor(long id, String name) {
        String sql = "INSERT OR IGNORE INTO authors (id, full_name, field3, field4) VALUES (?, ?, ?, ?);";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.setString(2, name);
            pstmt.setString(3, "");
            pstmt.setString(4, "");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Помилка вставки автора", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Зберігає автора (вставляє або оновлює).
     */
    public void save(Author author) {
        String sql = "INSERT OR REPLACE INTO authors (id, full_name, first_name, middle_name, last_name, field3, field4) VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, author.id());
            pstmt.setString(2, author.displayFullName());
            pstmt.setString(3, author.firstName());
            pstmt.setString(4, author.middleName());
            pstmt.setString(5, author.lastName());
            pstmt.setString(6, "");
            pstmt.setString(7, "");
            pstmt.executeUpdate();
            logger.debug("Збережено автора: {}", author.displayFullName());
        } catch (SQLException e) {
            logger.error("Помилка збереження автора {}", author.displayFullName(), e);
            throw new RuntimeException(e);
        }
    }
}