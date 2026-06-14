package com.myhomelibcorp.infrastructure.database;

import com.myhomelibcorp.domain.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Менеджер підключення до бази даних SQLite.
 * Використовує FTS5 з токенізатором unicode61 для регістронезалежного пошуку.
 * Автоматично створює всі таблиці, індекси та тригери.
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private final String dbUrl;
    private Connection activeConnection;

    public DatabaseManager(Path dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();
        initializeDatabase();
    }

    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Оптимізація SQLite
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=OFF;");
            stmt.execute("PRAGMA cache_size=-64000;");
            stmt.execute("PRAGMA foreign_keys=ON;");

            // ==================== ОСНОВНІ ТАБЛИЦІ ====================

            // Книги
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY,
                    title TEXT NOT NULL,
                    series TEXT,
                    sequence_number INTEGER,
                    file_name TEXT,
                    folder TEXT,
                    archive_entry TEXT,
                    language TEXT,
                    file_size INTEGER,
                    keywords TEXT,
                    annotation TEXT,
                    rate INTEGER DEFAULT 0,
                    progress INTEGER DEFAULT 0,
                    date_time TEXT,
                    review TEXT
                );
            """);

            // Автори
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS authors (
                    id INTEGER PRIMARY KEY,
                    full_name TEXT NOT NULL,
                    first_name TEXT,
                    middle_name TEXT,
                    last_name TEXT,
                    field3 TEXT,
                    field4 TEXT
                );
            """);

            // Зв'язок книга-автор
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS book_authors (
                    book_id INTEGER,
                    author_id INTEGER,
                    PRIMARY KEY (book_id, author_id),
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
                    FOREIGN KEY (author_id) REFERENCES authors(id) ON DELETE CASCADE
                );
            """);

            // Жанри
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS genres (
                    code TEXT PRIMARY KEY,
                    name TEXT NOT NULL
                );
            """);

            // Зв'язок книга-жанр
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS book_genres (
                    book_id INTEGER,
                    genre_code TEXT,
                    PRIMARY KEY (book_id, genre_code),
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
                    FOREIGN KEY (genre_code) REFERENCES genres(code) ON DELETE CASCADE
                );
            """);

            // Групи
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS groups (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);

            // Зв'язок книга-група
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS book_groups (
                    book_id INTEGER,
                    group_id INTEGER,
                    PRIMARY KEY (book_id, group_id),
                    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
                    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
                );
            """);

            // Налаштування
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """);

            // Міграції для зворотної сумісності зі старою схемою
            try { stmt.execute("ALTER TABLE books ADD COLUMN review TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE authors ADD COLUMN field3 TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE authors ADD COLUMN field4 TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE authors ADD COLUMN first_name TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE authors ADD COLUMN middle_name TEXT;"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE authors ADD COLUMN last_name TEXT;"); } catch (SQLException ignored) {}

            // Міграція жанрів (стара версія)
            try {
                stmt.execute("ALTER TABLE genres RENAME TO genres_old;");
                stmt.execute("CREATE TABLE IF NOT EXISTS genres (code TEXT PRIMARY KEY, name TEXT NOT NULL);");
                stmt.execute("INSERT OR IGNORE INTO genres (code, name) SELECT name, name FROM genres_old;");
                stmt.execute("DROP TABLE genres_old;");
            } catch (SQLException ignored) {}

            // ==================== FTS5 ТА ТРИГЕРИ ====================

            // Видаляємо стару FTS-таблицю, щоб створити з правильною конфігурацією
            try { stmt.execute("DROP TABLE IF EXISTS books_fts;"); } catch (SQLException ignored) {}

            // Віртуальна таблиця для повнотекстового пошуку з підтримкою Unicode
            stmt.execute("""
                CREATE VIRTUAL TABLE books_fts USING fts5(
                    title, authors, series, keywords, annotation,
                    tokenize='unicode61'
                );
            """);

            // Тригер на вставку книги (зберігає всі текстові поля в нижньому регістрі)
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS books_ai AFTER INSERT ON books BEGIN
                    INSERT INTO books_fts(rowid, title, authors, series, keywords, annotation)
                    VALUES (
                        new.id,
                        lower(new.title),
                        lower(coalesce((
                            SELECT group_concat(full_name, ' ') FROM authors 
                            JOIN book_authors ON authors.id = book_authors.author_id 
                            WHERE book_authors.book_id = new.id
                        ), '')),
                        lower(new.series),
                        lower(new.keywords),
                        lower(new.annotation)
                    );
                END;
            """);

            // Тригер на оновлення книги
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS books_au AFTER UPDATE ON books BEGIN
                    UPDATE books_fts SET
                        title = lower(new.title),
                        authors = lower(coalesce((
                            SELECT group_concat(full_name, ' ') FROM authors 
                            JOIN book_authors ON authors.id = book_authors.author_id 
                            WHERE book_authors.book_id = new.id
                        ), '')),
                        series = lower(new.series),
                        keywords = lower(new.keywords),
                        annotation = lower(new.annotation)
                    WHERE rowid = old.id;
                END;
            """);

            // Тригер на видалення книги
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS books_ad AFTER DELETE ON books BEGIN
                    DELETE FROM books_fts WHERE rowid = old.id;
                END;
            """);

            // Тригер при додаванні зв'язку книга-автор
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS book_authors_ai AFTER INSERT ON book_authors BEGIN
                    UPDATE books_fts SET authors = lower(coalesce((
                        SELECT group_concat(full_name, ' ') FROM authors 
                        JOIN book_authors ON authors.id = book_authors.author_id 
                        WHERE book_authors.book_id = new.book_id
                    ), '')) WHERE rowid = new.book_id;
                END;
            """);

            // Тригер при видаленні зв'язку книга-автор
            stmt.execute("""
                CREATE TRIGGER IF NOT EXISTS book_authors_ad AFTER DELETE ON book_authors BEGIN
                    UPDATE books_fts SET authors = lower(coalesce((
                        SELECT group_concat(full_name, ' ') FROM authors 
                        JOIN book_authors ON authors.id = book_authors.author_id 
                        WHERE book_authors.book_id = old.book_id
                    ), '')) WHERE rowid = old.book_id;
                END;
            """);

            // ==================== ІНДЕКСИ ====================
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_title ON books(title);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_series ON books(series);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_books_language ON books(language);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_authors_full_name ON authors(full_name);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_book_authors_book_id ON book_authors(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_book_authors_author_id ON book_authors(author_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_book_genres_book_id ON book_genres(book_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_genres_code ON genres(code);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_genres_name ON genres(name);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_groups_name ON groups(name);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_book_groups_book_id ON book_groups(book_id);");

            // ==================== ПОЧАТКОВІ ДАНІ ====================
            seedDefaultGroups(stmt);
            seedDefaultSettings(stmt);

            logger.info("Базу даних ініціалізовано: {}", dbUrl);
        } catch (SQLException e) {
            logger.error("Помилка ініціалізації бази даних", e);
        }
    }

    private void seedDefaultGroups(Statement stmt) {
        try {
            stmt.execute("INSERT OR IGNORE INTO groups (name) VALUES ('Favorites');");
            stmt.execute("INSERT OR IGNORE INTO groups (name) VALUES ('To read');");
        } catch (SQLException ignored) {}
    }

    private void seedDefaultSettings(Statement stmt) {
        try {
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('view.hideDeleted', 'true');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('view.showLocalOnly', 'false');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('import.readFb2', 'true');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('import.readZip', 'true');");
            stmt.execute("INSERT OR IGNORE INTO settings (key, value) VALUES ('import.readInpx', 'true');");
        } catch (SQLException ignored) {}
    }

    public void open() throws SQLException {
        if (activeConnection == null || activeConnection.isClosed()) {
            activeConnection = getConnection();
        }
    }

    public void close() throws SQLException {
        if (activeConnection != null && !activeConnection.isClosed()) {
            activeConnection.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void saveBooksBatch(List<Book> books) {
        logger.info("Пакетне збереження {} книг (ще не реалізовано)", books.size());
    }

    public int getBookCount() {
        String sql = "SELECT COUNT(*) FROM books;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.error("Помилка отримання кількості книг", e);
        }
        return 0;
    }
}