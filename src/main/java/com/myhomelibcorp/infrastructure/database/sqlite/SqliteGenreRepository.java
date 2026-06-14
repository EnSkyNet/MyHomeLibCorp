package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Репозиторій для роботи з жанрами книг.
 */
public class SqliteGenreRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteGenreRepository.class);
    private final DatabaseManager dbManager;

    public SqliteGenreRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Зберігає жанр: код -> назва.
     */
    public void saveGenre(String code, String name) {
        String sql = "INSERT OR REPLACE INTO genres (code, name) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
            logger.debug("Збережено жанр: {} -> {}", code, name);
        } catch (SQLException e) {
            logger.error("Помилка збереження жанру {} -> {}", code, name, e);
            throw new RuntimeException("Помилка збереження жанру: " + code + "/" + name, e);
        }
    }

    /**
     * Повертає назву жанру за кодом. Якщо код не знайдено, повертає сам код.
     */
    public String getGenreName(String code) {
        String sql = "SELECT name FROM genres WHERE code = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            } else {
                logger.warn("Код жанру не знайдено: {}", code);
            }
        } catch (SQLException e) {
            logger.error("Помилка отримання назви жанру для коду {}", code, e);
        }
        return code; // fallback
    }

    /**
     * Повертає список всіх назв жанрів, відсортованих за назвою.
     */
    public List<String> getAllGenreNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM genres ORDER BY name";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) names.add(rs.getString("name"));
            logger.debug("Отримано {} жанрів", names.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання списку жанрів", e);
            throw new RuntimeException(e);
        }
        return names;
    }

    /**
     * Повертає карту всіх жанрів (код -> назва).
     */
    public Map<String, String> getAllGenres() {
        Map<String, String> map = new LinkedHashMap<>();
        String sql = "SELECT code, name FROM genres ORDER BY name";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("code"), rs.getString("name"));
            logger.debug("Отримано {} жанрів у вигляді карти", map.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання карти жанрів", e);
            throw new RuntimeException(e);
        }
        return map;
    }
}