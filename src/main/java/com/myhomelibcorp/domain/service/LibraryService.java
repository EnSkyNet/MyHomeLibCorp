package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.common.io.ZipFiles;
import com.myhomelibcorp.domain.model.*;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.*;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import com.myhomelibcorp.infrastructure.importer.GenreListImporter;
import com.myhomelibcorp.infrastructure.importer.InpxImporter;
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

/**
 * Головний сервіс бібліотеки, який об'єднує всі операції:
 * пошук, імпорт, оновлення книг, робота з групами, налаштуваннями, рецензіями тощо.
 */
public class LibraryService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryService.class);

    private final DatabaseManager dbManager;
    private final SqliteBookRepository bookRepo;
    private final SqliteAuthorRepository authorRepo;
    private final SqliteGenreRepository genreRepo;
    private final SqliteGroupRepository groupRepo;
    private final SqliteSettingsRepository settingsRepo;
    private final SqliteReviewRepository reviewRepo;
    private final Fb2Importer fb2Importer;
    private final InpxImporter inpxImporter;
    private final GenreListImporter genreListImporter;
    private final BookContentService contentService;

    public LibraryService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        this.bookRepo = new SqliteBookRepository(dbManager);
        this.authorRepo = new SqliteAuthorRepository(dbManager);
        this.genreRepo = new SqliteGenreRepository(dbManager);
        this.groupRepo = new SqliteGroupRepository(dbManager);
        this.settingsRepo = new SqliteSettingsRepository(dbManager);
        this.reviewRepo = new SqliteReviewRepository(dbManager);
        this.fb2Importer = new Fb2Importer();
        this.inpxImporter = new InpxImporter();
        this.genreListImporter = new GenreListImporter(dbManager, genreRepo);
        this.contentService = new BookContentService();
    }

    // ==================== ПОШУК ТА СПИСКИ ====================

    /**
     * Пошук книг за текстовим запитом (назва, серія, ключові слова).
     */
    public List<Book> searchBooks(String query) {
        if (query == null || query.isBlank()) {
            return bookRepo.findAll();
        }
        return bookRepo.findByTitle(query);
    }

    /**
     * Список всіх авторів (рядки ПІБ).
     */
    public List<String> listAuthors() {
        return authorRepo.findAll().stream()
                .map(Author::displayFullName)
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());
    }

    /**
     * Список всіх серій книг.
     */
    public List<String> listSeries() {
        String sql = "SELECT DISTINCT series FROM books WHERE series IS NOT NULL AND series != '' ORDER BY series COLLATE NOCASE";
        try (var conn = dbManager.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString(1));
            return list;
        } catch (Exception e) {
            logger.error("Помилка отримання списку серій", e);
            return List.of();
        }
    }

    /**
     * Список всіх жанрів.
     */
    public List<String> listGenres() {
        return genreRepo.getAllGenreNames();
    }

    /**
     * Список всіх груп.
     */
    public List<String> listGroups() {
        return groupRepo.findAllNames();
    }

    // ==================== ОПЕРАЦІЇ З КНИГАМИ ====================

    /**
     * Оновлює метадані книги (назва, серія, автори, жанри, оцінка, прогрес тощо).
     */
    public void updateBook(Book book) {
        bookRepo.update(book);
        bookRepo.saveAuthorsForBook(book.id(), book.authors());
        List<String> genreCodes = book.genres().stream()
                .map(this::getGenreCodeByName)
                .collect(Collectors.toList());
        bookRepo.saveGenresForBook(book.id(), genreCodes);
        logger.info("Оновлено книгу: {}", book.title());
    }

    /**
     * Допоміжний метод: отримує код жанру за назвою.
     */
    private String getGenreCodeByName(String genreName) {
        Map<String, String> all = genreRepo.getAllGenres();
        for (Map.Entry<String, String> e : all.entrySet()) {
            if (e.getValue().equalsIgnoreCase(genreName)) return e.getKey();
        }
        return genreName;
    }

    /**
     * Встановлює оцінку книзі (від 0 до 5).
     */
    public void setRate(long bookId, int rate) {
        bookRepo.updateRate(bookId, rate);
        logger.info("Встановлено оцінку {} для книги id={}", rate, bookId);
    }

    /**
     * Встановлює прогрес читання (0-100%).
     */
    public void setProgress(long bookId, int progress) {
        bookRepo.updateProgress(bookId, progress);
        logger.info("Встановлено прогрес {}% для книги id={}", progress, bookId);
    }

    /**
     * Отримує рецензію на книгу.
     */
    public String getReview(long bookId) {
        return reviewRepo.getReview(bookId);
    }

    /**
     * Зберігає рецензію на книгу.
     */
    public void setReview(long bookId, String review) {
        reviewRepo.setReview(bookId, review);
        logger.info("Оновлено рецензію для книги id={}", bookId);
    }

    // ==================== ГРУПИ ====================

    public void addBookToGroup(long bookId, String groupName) {
        groupRepo.addBookToGroup(bookId, groupName);
    }

    public void removeBookFromGroup(long bookId, String groupName) {
        groupRepo.removeBookFromGroup(bookId, groupName);
    }

    public List<String> getGroupsForBook(long bookId) {
        return groupRepo.getGroupsForBook(bookId);
    }

    // ==================== НАЛАШТУВАННЯ ====================

    public Map<String, String> getSettings() {
        return settingsRepo.getAll();
    }

    public String getSetting(String key, String defaultValue) {
        return settingsRepo.get(key, defaultValue);
    }

    public void setSetting(String key, String value) {
        settingsRepo.set(key, value);
    }

    // ==================== ІМПОРТ ПАПКИ ====================

    /**
     * Імпортує всі FB2 та ZIP-файли з вказаної папки (рекурсивно).
     * @param folder шлях до папки
     * @param status функція для оновлення статусу в UI
     */
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
            List<? extends ZipEntry> entries = zip.stream()
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

    /**
     * Зберігає книгу, отриману з FB2, у базу даних.
     */
    private void saveBookFromFb2(Fb2Book fb2Book) {
        String unique = fb2Book.sourcePath().toString() + fb2Book.archiveEntry();
        long bookId = Math.abs(unique.hashCode());

        List<Author> authorsWithIds = new ArrayList<>();
        for (Author author : fb2Book.authors()) {
            long authId = author.id();
            if (authId == 0) {
                authId = Math.abs(author.displayFullName().hashCode());
            }
            authorsWithIds.add(new Author(authId, author.firstName(), author.middleName(), author.lastName()));
        }

        String folder;
        String fileName;
        if (fb2Book.archiveEntry().isBlank()) {
            folder = fb2Book.sourcePath().getParent() != null ? fb2Book.sourcePath().getParent().toString() : "";
            fileName = fb2Book.sourcePath().getFileName().toString();
        } else {
            folder = fb2Book.sourcePath().toString();
            fileName = fb2Book.archiveEntry();
        }

        Book book = new Book(
                bookId, fb2Book.title(), authorsWithIds, fb2Book.genres(),
                fb2Book.series(), fb2Book.sequenceNumber(), fb2Book.language(),
                fileName, folder, fb2Book.archiveEntry(), fb2Book.fileSize(),
                fb2Book.keywords(), fb2Book.annotation(),
                0, 0, LocalDateTime.now()
        );

        logger.debug("Збереження книги: {} (folder={}, fileName={}, archiveEntry={})",
                book.title(), book.folder(), book.fileName(), book.archiveEntry());

        bookRepo.save(book);
        for (Author author : authorsWithIds) {
            authorRepo.save(author);
        }
        bookRepo.saveAuthorsForBook(book.id(), authorsWithIds);

        List<String> genreCodes = fb2Book.genres();
        for (String code : genreCodes) {
            if (code != null && !code.isBlank()) {
                String existingName = genreRepo.getGenreName(code);
                if (existingName.equals(code)) {
                    genreRepo.saveGenre(code, code);
                }
            }
        }
        bookRepo.saveGenresForBook(book.id(), genreCodes);
    }

    // ==================== ІМПОРТ INPX ====================

    /**
     * Імпортує книги з INPX-файлу (архів з індексом).
     */
    public ImportResult importInpx(Path inpxFile, Consumer<String> status) throws Exception {
        status.accept("Імпорт INPX: " + inpxFile);
        List<Fb2Book> books = inpxImporter.parseInpFolder(Files.newInputStream(inpxFile), inpxFile);
        int saved = 0;
        for (Fb2Book fb : books) {
            saveBookFromFb2(fb);
            saved++;
        }
        logger.info("Імпортовано {} книг з INPX", saved);
        return new ImportResult(books, saved);
    }

    // ==================== ІМПОРТ ЖАНРІВ ====================

    public int importGenreList(Path file, String source) throws Exception {
        genreListImporter.importGenresFromFile(file);
        return 1;
    }

    // ==================== ЕКСПОРТ ТА ЧИТАННЯ ====================

    /**
     * Експортує книгу (копіює файл або витягує з архіву) у вказане місце.
     */
    public void exportBook(Book book, Path destination) throws Exception {
        Path sourcePath;
        if (book.hasArchiveEntry()) {
            sourcePath = Path.of(book.folder());
        } else {
            sourcePath = Path.of(book.folder(), book.fileName());
        }
        Files.copy(sourcePath, destination, StandardCopyOption.REPLACE_EXISTING);
        logger.info("Книгу експортовано до {}", destination);
    }

    /**
     * Читає книгу та повертає HTML-представлення для відображення у WebView.
     */
    public String readBookHtml(Book book) throws Exception {
        return contentService.readBookHtml(book);
    }
}