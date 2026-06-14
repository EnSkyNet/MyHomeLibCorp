package com.myhomelibcorp.infrastructure.database.sqlite;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite реалізація репозиторію книг.
 * Виконує всі CRUD операції, а також роботу зі зв'язками авторів та жанрів.
 */
public class SqliteBookRepository {

    private static final Logger logger = LoggerFactory.getLogger(SqliteBookRepository.class);
    private final DatabaseManager databaseManager;

    public SqliteBookRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Повертає всі книги, впорядковані за назвою (без урахування регістру).
     */
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                books.add(mapResultSetToBook(rs));
            }
            logger.debug("Знайдено {} книг", books.size());
        } catch (SQLException e) {
            logger.error("Помилка виконання findAll()", e);
            throw new RuntimeException("Не вдалося отримати список книг", e);
        }
        return books;
    }

    /**
     * Знаходить книгу за її унікальним ідентифікатором.
     * @return Optional з книгою, або пустий Optional, якщо не знайдено.
     */
    public Optional<Book> findById(long id) {
        String sql = "SELECT * FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка пошуку книги за id {}", id, e);
            throw new RuntimeException("Помилка пошуку книги за id: " + id, e);
        }
        return Optional.empty();
    }

    /**
     * Зберігає нову книгу або оновлює існуючу (upsert).
     */
    public void save(Book book) {
        String sql = """
            INSERT OR REPLACE INTO books (id, title, series, sequence_number, file_name, folder, archive_entry, language, file_size, keywords, annotation, rate, progress, date_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, book.id());
            pstmt.setString(2, book.title());
            pstmt.setString(3, book.series());
            pstmt.setInt(4, book.sequenceNumber() != null ? book.sequenceNumber() : 0);
            pstmt.setString(5, book.fileName());
            pstmt.setString(6, book.folder());
            pstmt.setString(7, book.archiveEntry());
            pstmt.setString(8, book.language());
            pstmt.setLong(9, book.fileSize());
            pstmt.setString(10, book.keywords());
            pstmt.setString(11, book.annotation());
            pstmt.setInt(12, book.rate());
            pstmt.setInt(13, book.progress());
            pstmt.setString(14, book.updateDate() != null ? book.updateDate().toString() : LocalDateTime.now().toString());
            pstmt.executeUpdate();
            logger.debug("Збережено книгу: id={}, title={}", book.id(), book.title());
        } catch (SQLException e) {
            logger.error("Помилка збереження книги {}", book.title(), e);
            throw new RuntimeException("Не вдалося зберегти книгу: " + book.title(), e);
        }
    }

    /**
     * Оновлює існуючу книгу (без зміни незмінних полів).
     */
    public void update(Book book) {
        String sql = """
            UPDATE books SET
                title = ?, series = ?, sequence_number = ?, language = ?,
                keywords = ?, annotation = ?, rate = ?, progress = ?, date_time = ?
            WHERE id = ?
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.title());
            ps.setString(2, book.series());
            ps.setInt(3, book.sequenceNumber() != null ? book.sequenceNumber() : 0);
            ps.setString(4, book.language());
            ps.setString(5, book.keywords());
            ps.setString(6, book.annotation());
            ps.setInt(7, book.rate());
            ps.setInt(8, book.progress());
            ps.setString(9, book.updateDate().toString());
            ps.setLong(10, book.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Помилка оновлення книги id {}", book.id(), e);
            throw new RuntimeException("Не вдалося оновити книгу: " + book.id(), e);
        }
    }

    /**
     * Видаляє книгу за її ідентифікатором.
     */
    public void deleteById(long id) {
        String sql = "DELETE FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            logger.debug("Видалено книгу id={}", id);
        } catch (SQLException e) {
            logger.error("Помилка видалення книги id {}", id, e);
            throw new RuntimeException("Не вдалося видалити книгу: " + id, e);
        }
    }

    /**
     * Пошук книг за назвою (частковий збіг, нечутливий до регістру).
     */
    public List<Book> findByTitle(String titlePattern) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? ORDER BY title COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + titlePattern + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка пошуку за назвою: {}", titlePattern, e);
            throw new RuntimeException("Помилка пошуку за назвою: " + titlePattern, e);
        }
        return books;
    }

    /**
     * Пошук книг за ім'ям автора (частковий збіг по full_name або last_name).
     */
    public List<Book> findByAuthor(String authorName) {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.* FROM books b
            JOIN book_authors ba ON b.id = ba.book_id
            JOIN authors a ON a.id = ba.author_id
            WHERE a.full_name LIKE ? OR a.last_name LIKE ?
            ORDER BY b.title COLLATE NOCASE;
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + authorName + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(mapResultSetToBook(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Помилка пошуку за автором: {}", authorName, e);
            throw new RuntimeException("Помилка пошуку за автором: " + authorName, e);
        }
        return books;
    }

    /**
     * Оновлює лише оцінку книги.
     */
    public void updateRate(long bookId, int rate) {
        String sql = "UPDATE books SET rate = ?, date_time = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rate);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setLong(3, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Помилка оновлення оцінки книги id {}", bookId, e);
            throw new RuntimeException("Помилка оновлення оцінки для книги: " + bookId, e);
        }
    }

    /**
     * Оновлює прогрес читання книги.
     */
    public void updateProgress(long bookId, int progress) {
        String sql = "UPDATE books SET progress = ?, date_time = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, progress);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setLong(3, bookId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Помилка оновлення прогресу книги id {}", bookId, e);
            throw new RuntimeException("Помилка оновлення прогресу для книги: " + bookId, e);
        }
    }

    // --- Методи для роботи з авторами та жанрами ---

    public void saveAuthorsForBook(long bookId, List<Author> authors) {
        String deleteSql = "DELETE FROM book_authors WHERE book_id = ?";
        String insertSql = "INSERT OR IGNORE INTO book_authors (book_id, author_id) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setLong(1, bookId);
                delStmt.executeUpdate();
            }
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                for (Author author : authors) {
                    insStmt.setLong(1, bookId);
                    insStmt.setLong(2, author.id());
                    insStmt.addBatch();
                }
                insStmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Помилка збереження авторів для книги id {}", bookId, e);
            throw new RuntimeException("Не вдалося зберегти авторів для книги: " + bookId, e);
        }
    }

    public void saveGenresForBook(long bookId, List<String> genreCodes) {
        String deleteSql = "DELETE FROM book_genres WHERE book_id = ?";
        String insertSql = "INSERT OR IGNORE INTO book_genres (book_id, genre_code) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(deleteSql)) {
                delStmt.setLong(1, bookId);
                delStmt.executeUpdate();
            }
            try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                for (String code : genreCodes) {
                    if (code == null || code.isBlank()) continue;
                    insStmt.setLong(1, bookId);
                    insStmt.setString(2, code);
                    insStmt.addBatch();
                }
                insStmt.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error("Помилка збереження жанрів для книги id {}", bookId, e);
            throw new RuntimeException("Не вдалося зберегти жанри для книги: " + bookId, e);
        }
    }

    public List<Author> findAuthorsForBook(long bookId) {
        List<Author> authors = new ArrayList<>();
        String sql = """
            SELECT a.id, a.full_name, a.first_name, a.middle_name, a.last_name
            FROM authors a
            JOIN book_authors ba ON a.id = ba.author_id
            WHERE ba.book_id = ?
        """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
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
        } catch (SQLException e) {
            logger.error("Помилка отримання авторів для книги id {}", bookId, e);
            throw new RuntimeException("Помилка отримання авторів для книги: " + bookId, e);
        }
        return authors;
    }

    public List<String> findGenreCodesForBook(long bookId) {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT genre_code FROM book_genres WHERE book_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                codes.add(rs.getString("genre_code"));
            }
        } catch (SQLException e) {
            logger.error("Помилка отримання жанрів для книги id {}", bookId, e);
            throw new RuntimeException("Помилка отримання жанрів для книги: " + bookId, e);
        }
        return codes;
    }

    // Приватний допоміжний метод
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        long bookId = rs.getLong("id");
        List<Author> authorsList = findAuthorsForBook(bookId);
        List<String> genreCodes = findGenreCodesForBook(bookId);
        // Тимчасове перетворення кодів в назви (в майбутньому буде використовувати GenreRepository)
        List<String> genreNames = new ArrayList<>();
        for (String code : genreCodes) {
            genreNames.add(code);
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
    public int getBookCount() {
        String sql = "SELECT COUNT(*) FROM books;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Помилка отримання кількості книг", e);
        }
        return 0;
    }
    /**
     * Зберігає жанр, якщо його ще немає в таблиці genres.
     */
    public void saveGenreIfNotExists(String code, String name) {
        String sql = "INSERT OR IGNORE INTO genres (code, name) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Помилка збереження жанру {} -> {}", code, name, e);
        }
    }

    /**
     * Примусово оновлює FTS-індекс для конкретної книги.
     * Використовується після додавання/зміни авторів або жанрів.
     */
    public void refreshFtsForBook(long bookId) {
        String authorsSql = """
            SELECT lower(group_concat(full_name, ' ')) as authors
            FROM authors
            JOIN book_authors ON authors.id = book_authors.author_id
            WHERE book_authors.book_id = ?
        """;
        String bookSql = "SELECT title, series, keywords, annotation FROM books WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement bookStmt = conn.prepareStatement(bookSql);
             PreparedStatement authorsStmt = conn.prepareStatement(authorsSql)) {

            // Отримуємо дані книги
            bookStmt.setLong(1, bookId);
            ResultSet bookRs = bookStmt.executeQuery();
            if (!bookRs.next()) return;

            String title = bookRs.getString("title");
            String series = bookRs.getString("series");
            String keywords = bookRs.getString("keywords");
            String annotation = bookRs.getString("annotation");

            // Отримуємо об'єднаних авторів
            authorsStmt.setLong(1, bookId);
            ResultSet authorsRs = authorsStmt.executeQuery();
            String authors = authorsRs.next() ? authorsRs.getString("authors") : "";

            // Оновлюємо FTS
            String updateFtsSql = """
                UPDATE books_fts SET
                    title = lower(?),
                    authors = ?,
                    series = lower(?),
                    keywords = lower(?),
                    annotation = lower(?)
                WHERE rowid = ?
            """;
            try (PreparedStatement updateStmt = conn.prepareStatement(updateFtsSql)) {
                updateStmt.setString(1, title);
                updateStmt.setString(2, authors);
                updateStmt.setString(3, series);
                updateStmt.setString(4, keywords);
                updateStmt.setString(5, annotation);
                updateStmt.setLong(6, bookId);
                updateStmt.executeUpdate();
                logger.debug("Оновлено FTS для книги id={}", bookId);
            }
        } catch (SQLException e) {
            logger.error("Помилка оновлення FTS для книги id={}", bookId, e);
        }
    }
}