package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Репозиторій для зберігання налаштувань програми (ключ-значення).
 */
public class SqliteSettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteSettingsRepository.class);
    private final DatabaseManager dbManager;

    public SqliteSettingsRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Повертає значення налаштування за ключем.
     * @param key ключ
     * @param defaultValue значення за замовчуванням, якщо ключ не знайдено
     * @return значення або defaultValue
     */
    public String get(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            logger.error("Помилка отримання налаштування для ключа {}", key, e);
            throw new RuntimeException(e);
        }
        return defaultValue;
    }

    /**
     * Зберігає налаштування (ключ-значення). Якщо ключ існує, значення оновлюється.
     */
    public void set(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
            logger.debug("Збережено налаштування: {} = {}", key, value);
        } catch (SQLException e) {
            logger.error("Помилка збереження налаштування {} = {}", key, value, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Повертає всі налаштування у вигляді карти.
     */
    public Map<String, String> getAll() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT key, value FROM settings";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) map.put(rs.getString("key"), rs.getString("value"));
            logger.debug("Отримано {} налаштувань", map.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання всіх налаштувань", e);
            throw new RuntimeException(e);
        }
        return map;
    }
}