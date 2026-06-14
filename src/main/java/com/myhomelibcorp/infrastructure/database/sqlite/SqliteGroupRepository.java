package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторій для роботи з групами книг (Favorites, To read тощо).
 */
public class SqliteGroupRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteGroupRepository.class);
    private final DatabaseManager dbManager;

    public SqliteGroupRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Повертає всі назви груп, відсортовані за назвою.
     */
    public List<String> findAllNames() {
        List<String> names = new ArrayList<>();
        String sql = "SELECT name FROM groups ORDER BY name";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) names.add(rs.getString("name"));
            logger.debug("Знайдено {} груп", names.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання списку груп", e);
            throw new RuntimeException(e);
        }
        return names;
    }

    /**
     * Знаходить ID групи за назвою, або створює нову групу, якщо такої ще немає.
     */
    public long findOrCreateGroup(String name) {
        String select = "SELECT id FROM groups WHERE name = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            // ignore, will try insert
        }

        String insert = "INSERT INTO groups (name) VALUES (?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                long id = keys.getLong(1);
                logger.debug("Створено нову групу: {} (id={})", name, id);
                return id;
            }
        } catch (SQLException e) {
            logger.error("Помилка створення групи {}", name, e);
            throw new RuntimeException(e);
        }
        return -1;
    }

    /**
     * Додає книгу до групи (створює групу, якщо її не існувало).
     */
    public void addBookToGroup(long bookId, String groupName) {
        long groupId = findOrCreateGroup(groupName);
        String sql = "INSERT OR IGNORE INTO book_groups (book_id, group_id) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setLong(2, groupId);
            ps.executeUpdate();
            logger.debug("Книгу {} додано до групи {}", bookId, groupName);
        } catch (SQLException e) {
            logger.error("Помилка додавання книги {} до групи {}", bookId, groupName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Видаляє книгу з групи.
     */
    public void removeBookFromGroup(long bookId, String groupName) {
        String sql = "DELETE FROM book_groups WHERE book_id = ? AND group_id = (SELECT id FROM groups WHERE name = ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ps.setString(2, groupName);
            ps.executeUpdate();
            logger.debug("Книгу {} видалено з групи {}", bookId, groupName);
        } catch (SQLException e) {
            logger.error("Помилка видалення книги {} з групи {}", bookId, groupName, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Повертає список назв груп, до яких належить книга.
     */
    public List<String> getGroupsForBook(long bookId) {
        List<String> groups = new ArrayList<>();
        String sql = "SELECT g.name FROM groups g JOIN book_groups bg ON g.id = bg.group_id WHERE bg.book_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) groups.add(rs.getString("name"));
            logger.debug("Для книги {} знайдено {} груп", bookId, groups.size());
        } catch (SQLException e) {
            logger.error("Помилка отримання груп для книги {}", bookId, e);
            throw new RuntimeException(e);
        }
        return groups;
    }
}