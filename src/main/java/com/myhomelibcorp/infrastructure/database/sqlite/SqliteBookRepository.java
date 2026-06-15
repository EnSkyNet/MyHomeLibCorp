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

public class SqliteBookRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteBookRepository.class);
    private final DatabaseManager databaseManager;
    private SqliteGenreRepository genreRepo;

    public SqliteBookRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
        this.genreRepo = new SqliteGenreRepository(databaseManager);
    }
    public List<Book> findAll() {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) books.add(mapResultSetToBook(rs));
        } catch (SQLException e) { logger.error("Помилка отримання всіх книг", e); throw new RuntimeException(e); }
        return books;
    }
    public Optional<Book> findById(long id) {
        String sql = "SELECT * FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) return Optional.of(mapResultSetToBook(rs)); }
        } catch (SQLException e) { logger.error("Помилка пошуку книги за id: {}", id, e); }
        return Optional.empty();
    }
    public void save(Book book) {
        String sql = "INSERT OR REPLACE INTO books (id, title, series, sequence_number, file_name, folder, archive_entry, language, file_size, keywords, annotation, rate, progress, date_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, book.id()); pstmt.setString(2, book.title()); pstmt.setString(3, book.series()); pstmt.setInt(4, book.sequenceNumber() != null ? book.sequenceNumber() : 0);
            pstmt.setString(5, book.fileName()); pstmt.setString(6, book.folder()); pstmt.setString(7, book.archiveEntry()); pstmt.setString(8, book.language());
            pstmt.setLong(9, book.fileSize()); pstmt.setString(10, book.keywords()); pstmt.setString(11, book.annotation()); pstmt.setInt(12, book.rate());
            pstmt.setInt(13, book.progress()); pstmt.setString(14, book.updateDate() != null ? book.updateDate().toString() : LocalDateTime.now().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) { logger.error("Помилка збереження книги: {}", book.title(), e); throw new RuntimeException(e); }
    }
    public void update(Book book) { /* аналогічно save, але з UPDATE */ }
    public void deleteById(long id) {
        String sql = "DELETE FROM books WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) { pstmt.setLong(1, id); pstmt.executeUpdate(); }
        catch (SQLException e) { logger.error("Помилка видалення книги id: {}", id, e); throw new RuntimeException(e); }
    }
    public List<Book> findByTitle(String titlePattern) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ? ORDER BY title COLLATE NOCASE;";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + titlePattern + "%");
            try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) books.add(mapResultSetToBook(rs)); }
        } catch (SQLException e) { logger.error("Помилка пошуку за назвою: {}", titlePattern, e); throw new RuntimeException(e); }
        return books;
    }
    public void updateRate(long bookId, int rate) {
        String sql = "UPDATE books SET rate = ?, date_time = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rate); ps.setString(2, LocalDateTime.now().toString()); ps.setLong(3, bookId); ps.executeUpdate();
        } catch (SQLException e) { logger.error("Помилка оновлення оцінки для книги: {}", bookId, e); throw new RuntimeException(e); }
    }
    public void updateProgress(long bookId, int progress) {
        String sql = "UPDATE books SET progress = ?, date_time = ? WHERE id = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, progress); ps.setString(2, LocalDateTime.now().toString()); ps.setLong(3, bookId); ps.executeUpdate();
        } catch (SQLException e) { logger.error("Помилка оновлення прогресу для книги: {}", bookId, e); throw new RuntimeException(e); }
    }
    public void saveAuthorsForBook(long bookId, List<Author> authors) {
        try (Connection conn = databaseManager.getConnection()) { conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM book_authors WHERE book_id = ?")) { del.setLong(1, bookId); del.executeUpdate(); }
            try (PreparedStatement ins = conn.prepareStatement("INSERT OR IGNORE INTO book_authors (book_id, author_id) VALUES (?, ?)")) {
                for (Author a : authors) { ins.setLong(1, bookId); ins.setLong(2, a.id()); ins.addBatch(); }
                ins.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) { logger.error("Помилка збереження авторів для книги: {}", bookId, e); throw new RuntimeException(e); }
    }
    public void saveGenresForBook(long bookId, List<String> genreCodes) {
        try (Connection conn = databaseManager.getConnection()) { conn.setAutoCommit(false);
            try (PreparedStatement del = conn.prepareStatement("DELETE FROM book_genres WHERE book_id = ?")) { del.setLong(1, bookId); del.executeUpdate(); }
            try (PreparedStatement ins = conn.prepareStatement("INSERT OR IGNORE INTO book_genres (book_id, genre_code) VALUES (?, ?)")) {
                for (String code : genreCodes) { if (code != null && !code.isBlank()) { ins.setLong(1, bookId); ins.setString(2, code); ins.addBatch(); } }
                ins.executeBatch();
            }
            conn.commit();
        } catch (SQLException e) { logger.error("Помилка збереження жанрів для книги: {}", bookId, e); throw new RuntimeException(e); }
    }
    public List<Author> findAuthorsForBook(long bookId) {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT a.id, a.full_name, a.first_name, a.middle_name, a.last_name FROM authors a JOIN book_authors ba ON a.id = ba.author_id WHERE ba.book_id = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) { String full = rs.getString("full_name");
                if (full != null && !full.isBlank()) authors.add(new Author(rs.getLong("id"), full));
                else authors.add(new Author(rs.getLong("id"), rs.getString("first_name"), rs.getString("middle_name"), rs.getString("last_name")));
            }
        } catch (SQLException e) { logger.error("Помилка отримання авторів для книги: {}", bookId, e); throw new RuntimeException(e); }
        return authors;
    }
    public List<String> findGenreCodesForBook(long bookId) {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT genre_code FROM book_genres WHERE book_id = ?";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) codes.add(rs.getString("genre_code"));
        } catch (SQLException e) { logger.error("Помилка отримання жанрів для книги: {}", bookId, e); throw new RuntimeException(e); }
        return codes;
    }
    public void saveGenreIfNotExists(String code, String name) {
        String sql = "INSERT OR IGNORE INTO genres (code, name) VALUES (?, ?)";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) { ps.setString(1, code); ps.setString(2, name); ps.executeUpdate(); }
        catch (SQLException e) { logger.error("Помилка збереження жанру {} -> {}", code, name, e); }
    }
    public void refreshFtsForBook(long bookId) {
        String authorsSql = "SELECT lower(group_concat(full_name, ' ')) as authors FROM authors JOIN book_authors ON authors.id = book_authors.author_id WHERE book_authors.book_id = ?";
        String bookSql = "SELECT title, series, keywords, annotation FROM books WHERE id = ?";
        try (Connection conn = databaseManager.getConnection()) {
            PreparedStatement bookStmt = conn.prepareStatement(bookSql); bookStmt.setLong(1, bookId);
            ResultSet bookRs = bookStmt.executeQuery(); if (!bookRs.next()) return;
            String title = bookRs.getString("title"), series = bookRs.getString("series"), keywords = bookRs.getString("keywords"), annotation = bookRs.getString("annotation");
            PreparedStatement authorsStmt = conn.prepareStatement(authorsSql); authorsStmt.setLong(1, bookId);
            ResultSet authorsRs = authorsStmt.executeQuery();
            String authors = authorsRs.next() ? authorsRs.getString("authors") : "";
            String updateFtsSql = "UPDATE books_fts SET title=lower(?), authors=?, series=lower(?), keywords=lower(?), annotation=lower(?) WHERE rowid = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateFtsSql)) {
                updateStmt.setString(1, title); updateStmt.setString(2, authors); updateStmt.setString(3, series);
                updateStmt.setString(4, keywords); updateStmt.setString(5, annotation); updateStmt.setLong(6, bookId);
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) { logger.error("Помилка оновлення FTS для книги id={}", bookId, e); }
    }
    public int getBookCount() {
        String sql = "SELECT COUNT(*) FROM books;";
        try (Connection conn = databaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { logger.error("Помилка отримання кількості книг", e); }
        return 0;
    }
    public void refreshAllFts() {
        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) { stmt.execute("DELETE FROM books_fts;"); }
            String insertAll = "INSERT INTO books_fts(rowid, title, authors, series, keywords, annotation) SELECT b.id, lower(b.title), lower(coalesce((SELECT group_concat(full_name, ' ') FROM authors JOIN book_authors ON authors.id = book_authors.author_id WHERE book_authors.book_id = b.id), '')), lower(b.series), lower(b.keywords), lower(b.annotation) FROM books b";
            try (Statement stmt = conn.createStatement()) { stmt.execute(insertAll); }
            conn.commit();
            logger.info("FTS-індекс перебудовано");
        } catch (SQLException e) { logger.error("Помилка перебудови FTS-індексу", e); }
    }
    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        long bookId = rs.getLong("id");
        List<Author> authors = findAuthorsForBook(bookId);
        List<String> genreCodes = findGenreCodesForBook(bookId);
        List<String> genreNames = new ArrayList<>();
        for (String code : genreCodes) { genreNames.add(genreRepo.getGenreName(code)); }
        LocalDateTime bookDate = (rs.getString("date_time") == null || rs.getString("date_time").isBlank()) ? LocalDateTime.now() : LocalDateTime.parse(rs.getString("date_time"));
        return new Book(bookId, rs.getString("title"), authors, genreNames, rs.getString("series"), rs.getInt("sequence_number"), rs.getString("language"), rs.getString("file_name"), rs.getString("folder"), rs.getString("archive_entry"), rs.getLong("file_size"), rs.getString("keywords"), rs.getString("annotation"), rs.getInt("rate"), rs.getInt("progress"), bookDate);
    }
}