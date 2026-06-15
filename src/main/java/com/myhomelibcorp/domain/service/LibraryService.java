package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.common.io.ZipFiles;
import com.myhomelibcorp.domain.model.*;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.*;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import com.myhomelibcorp.infrastructure.importer.GenreListImporter;
import com.myhomelibcorp.infrastructure.importer.InpxImporter;
import com.myhomelibcorp.infrastructure.settings.SettingsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibraryService {
    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);
    private final DatabaseManager dbManager;
    private final SqliteBookRepository bookRepo;
    private final SqliteAuthorRepository authorRepo;
    private final SqliteGenreRepository genreRepo;
    private final SqliteGroupRepository groupRepo;
    private final SqliteReviewRepository reviewRepo;
    private final SearchRepository searchRepo;
    private final Fb2Importer fb2Importer;
    private final InpxImporter inpxImporter;
    private final GenreListImporter genreListImporter;
    private final BookContentService contentService;
    private final SettingsStore settingsStore;

    public LibraryService(DatabaseManager dbManager, SettingsStore settingsStore) {
        this.dbManager = dbManager;
        this.settingsStore = settingsStore;
        this.bookRepo = new SqliteBookRepository(dbManager);
        this.authorRepo = new SqliteAuthorRepository(dbManager);
        this.genreRepo = new SqliteGenreRepository(dbManager);
        this.groupRepo = new SqliteGroupRepository(dbManager);
        this.reviewRepo = new SqliteReviewRepository(dbManager);
        this.searchRepo = new SearchRepository(dbManager);
        this.fb2Importer = new Fb2Importer();
        this.inpxImporter = new InpxImporter();
        this.genreListImporter = new GenreListImporter(dbManager, genreRepo);
        this.contentService = new BookContentService();
    }

    // ==================== ПОШУК ТА СПИСКИ ====================

    /**
     * Повнотекстовий пошук книг (FTS5). Регістронезалежний.
     */
    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return bookRepo.findAll();
        }
        SearchCriteria criteria = new SearchCriteria(query, "", "", "");
        return searchRepo.searchBooks(criteria);
    }

    public List<String> listAuthors() {
        return authorRepo.findAll().stream()
                .map(Author::displayFullName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    public List<String> listSeries() {
        try (var conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT DISTINCT series FROM books WHERE series IS NOT NULL AND series != '' ORDER BY series COLLATE NOCASE")) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        } catch (Exception e) {
            logger.error("Помилка отримання списку серій", e);
            return List.of();
        }
    }

    public List<String> listGenres() { return genreRepo.getAllGenreNames(); }
    public List<String> listGroups() { return groupRepo.findAllNames(); }

    // ==================== ОПЕРАЦІЇ З КНИГАМИ ====================
    public void updateBook(Book book) {
        bookRepo.update(book);
        bookRepo.saveAuthorsForBook(book.id(), book.authors());
        bookRepo.saveGenresForBook(book.id(), book.genres());
    }

    public void setRate(long bookId, int rate) { bookRepo.updateRate(bookId, rate); }
    public void setProgress(long bookId, int progress) { bookRepo.updateProgress(bookId, progress); }
    public String getReview(long bookId) { return reviewRepo.getReview(bookId); }
    public void setReview(long bookId, String review) { reviewRepo.setReview(bookId, review); }

    // ==================== ГРУПИ ====================
    public void addBookToGroup(long bookId, String groupName) { groupRepo.addBookToGroup(bookId, groupName); }
    public void removeBookFromGroup(long bookId, String groupName) { groupRepo.removeBookFromGroup(bookId, groupName); }
    public List<String> getGroupsForBook(long bookId) { return groupRepo.getGroupsForBook(bookId); }
    public List<Book> getBooksInGroup(String groupName) {
        List<Long> ids = groupRepo.getBookIdsInGroup(groupName);
        List<Book> books = new ArrayList<>();
        for (long id : ids) bookRepo.findById(id).ifPresent(books::add);
        return books;
    }
    public void createGroup(String groupName) { groupRepo.findOrCreateGroup(groupName); }
    public void renameGroup(String oldName, String newName) { groupRepo.renameGroup(oldName, newName); }
    public void deleteGroup(String groupName) { groupRepo.deleteGroup(groupName); }
    public void deleteBook(long bookId) { bookRepo.deleteById(bookId); }

    // ==================== НАЛАШТУВАННЯ ====================
    public Map<String, String> getSettings() { return settingsStore.getAll(); }
    public String getSetting(String key, String defaultValue) { return settingsStore.getString(key, defaultValue); }
    public void setSetting(String key, String value) { settingsStore.setString(key, value); }

    // ==================== ІМПОРТ ПАПКИ ====================
    public void importFolder(Path folder, Consumer<String> status) {
        if (!Files.isDirectory(folder)) {
            status.accept("Не є папкою: " + folder);
            return;
        }
        try {
            List<Path> files = Files.walk(folder)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".fb2") || name.endsWith(".fbd") || name.endsWith(".zip");
                    })
                    .collect(Collectors.toList());
            int total = files.size();
            int processed = 0;
            for (Path file : files) {
                processed++;
                status.accept("[" + processed + "/" + total + "] " + file.getFileName());
                if (file.toString().toLowerCase().endsWith(".zip")) {
                    processZipFile(file, status);
                } else {
                    processFb2File(file);
                }
            }
            status.accept("Імпорт завершено. Оброблено файлів: " + processed);
        } catch (IOException e) {
            logger.error("Помилка сканування папки", e);
            throw new RuntimeException("Помилка сканування папки", e);
        }
    }

    private void processFb2File(Path file) {
        try (InputStream is = Files.newInputStream(file)) {
            long size = Files.size(file);
            Fb2Book fb2Book = fb2Importer.parseFb2(is, file, "", size);
            saveBookFromFb2(fb2Book);
        } catch (Exception e) {
            logger.error("Помилка обробки FB2: {}", file, e);
        }
    }

    private void processZipFile(Path zipFile, Consumer<String> status) {
        try (ZipFile zip = ZipFiles.open(zipFile)) {
            List<ZipEntry> entries = zip.stream()
                    .filter(entry -> !entry.isDirectory())
                    .filter(entry -> {
                        String name = entry.getName().toLowerCase();
                        return name.endsWith(".fb2") || name.endsWith(".fbd");
                    })
                    .collect(Collectors.toList());
            if (entries.isEmpty()) {
                status.accept("У ZIP немає FB2 файлів: " + zipFile.getFileName());
                return;
            }
            status.accept("Обробка " + entries.size() + " книг з " + zipFile.getFileName());
            for (ZipEntry entry : entries) {
                try (InputStream is = zip.getInputStream(entry)) {
                    long size = entry.getSize();
                    Fb2Book fb2Book = fb2Importer.parseFb2(is, zipFile, entry.getName(), size);
                    saveBookFromFb2(fb2Book);
                } catch (Exception e) {
                    logger.error("Помилка у ZIP-записі: {}", entry.getName(), e);
                    status.accept("Помилка в " + entry.getName());
                }
            }
        } catch (IOException e) {
            logger.error("Не вдалося відкрити ZIP: {}", zipFile, e);
            status.accept("Не вдалося відкрити ZIP: " + zipFile.getFileName());
        }
    }

    private void saveBookFromFb2(Fb2Book fb2Book) {
        long bookId = Math.abs((fb2Book.sourcePath().toString() + fb2Book.archiveEntry()).hashCode());
        List<Author> authorsWithIds = new ArrayList<>();
        for (Author author : fb2Book.authors()) {
            long authId = author.id() == 0 ? Math.abs(author.displayFullName().hashCode()) : author.id();
            authorsWithIds.add(new Author(authId, author.firstName(), author.middleName(), author.lastName()));
        }
        String folder, fileName;
        if (fb2Book.archiveEntry().isBlank()) {
            folder = fb2Book.sourcePath().getParent() != null ? fb2Book.sourcePath().getParent().toString() : "";
            fileName = fb2Book.sourcePath().getFileName().toString();
        } else {
            folder = fb2Book.sourcePath().toString();
            fileName = fb2Book.archiveEntry();
        }
        Book book = new Book(bookId, fb2Book.title(), authorsWithIds, fb2Book.genres(),
                fb2Book.series(), fb2Book.sequenceNumber(), fb2Book.language(),
                fileName, folder, fb2Book.archiveEntry(), fb2Book.fileSize(),
                fb2Book.keywords(), fb2Book.annotation(), 0, 0, LocalDateTime.now());
        bookRepo.save(book);
        for (Author author : authorsWithIds) authorRepo.save(author);
        bookRepo.saveAuthorsForBook(book.id(), authorsWithIds);
        for (String code : fb2Book.genres()) {
            if (code != null && !code.isBlank()) {
                String existing = genreRepo.getGenreName(code);
                if (existing.equals(code)) genreRepo.saveGenre(code, code);
            }
        }
        bookRepo.saveGenresForBook(book.id(), fb2Book.genres());
    }

    // ==================== ІМПОРТ INPX ====================
    public ImportResult importInpx(Path inpxFile, Consumer<String> status) throws Exception {
        status.accept("Імпорт INPX: " + inpxFile);
        List<Fb2Book> books = inpxImporter.parseInpFolder(Files.newInputStream(inpxFile), inpxFile);
        int saved = 0;
        for (Fb2Book fb : books) {
            saveBookFromFb2(fb);
            saved++;
        }
        return new ImportResult(books, saved);
    }

    // ==================== ІМПОРТ ЖАНРІВ ====================
    public int importGenreList(Path file, String source) throws Exception {
        genreListImporter.importGenresFromFile(file);
        bookRepo.refreshAllFts();
        return 1;
    }

    // ==================== ЕКСПОРТ ====================
    public void exportBook(Book book, Path destination) throws Exception {
        Path source = book.hasArchiveEntry() ? Path.of(book.folder()) : Path.of(book.folder(), book.fileName());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }

    // ==================== ЧИТАННЯ ====================
    public String readBookHtml(Book book) throws Exception {
        return contentService.readBookHtml(book);
    }

    // ==================== ДОДАТКОВІ МЕТОДИ ====================
    public void rebuildFtsIndex() { bookRepo.refreshAllFts(); }
}