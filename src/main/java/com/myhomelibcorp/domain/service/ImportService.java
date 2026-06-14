package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.Fb2Book;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Сервіс для імпорту окремих книг (FB2, FBD) з файлової системи.
 */
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final Fb2Importer fb2Importer;
    private final SqliteBookRepository bookRepository;

    public ImportService(Fb2Importer fb2Importer, SqliteBookRepository bookRepository) {
        this.fb2Importer = fb2Importer;
        this.bookRepository = bookRepository;
    }

    /**
     * Імпортує книгу з локального файлу (FB2).
     * @param filePath шлях до файлу .fb2
     * @return збережена книга
     */
    public Book importBookFromPath(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalArgumentException("Файл для імпорту не існує або шлях порожній");
        }

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            long fileSize = Files.size(filePath);
            return importBook(inputStream, filePath, "", fileSize);
        } catch (Exception e) {
            logger.error("Помилка читання файлу для імпорту: {}", filePath.getFileName(), e);
            throw new RuntimeException("Помилка читання файлу: " + filePath.getFileName(), e);
        }
    }

    /**
     * Універсальний метод імпорту з InputStream (для FB2 з архіву або звичайного файлу).
     * @param inputStream потік даних FB2
     * @param sourcePath шлях до джерела (файл або ZIP)
     * @param archiveEntry ім'я запису всередині архіву (порожнє, якщо не архів)
     * @param fileSize розмір файлу
     * @return збережена книга
     */
    public Book importBook(InputStream inputStream, Path sourcePath, String archiveEntry, long fileSize) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Потік даних InputStream не може бути null");
        }

        Fb2Book fb2Book = fb2Importer.parseFb2(inputStream, sourcePath, archiveEntry, fileSize);
        long generatedId = System.nanoTime();

        Book book = new Book(
                generatedId,
                fb2Book.title(),
                fb2Book.authors(),
                fb2Book.genres(),
                fb2Book.series(),
                fb2Book.sequenceNumber(),
                (fb2Book.archiveEntry() != null && !fb2Book.archiveEntry().isEmpty())
                        ? fb2Book.archiveEntry()
                        : fb2Book.sourcePath().getFileName().toString(),
                fb2Book.sourcePath().getParent() != null ? fb2Book.sourcePath().getParent().toString() : "",
                fb2Book.archiveEntry() != null ? fb2Book.archiveEntry() : "",
                fb2Book.language(),
                fb2Book.fileSize(),
                fb2Book.keywords(),
                fb2Book.annotation(),
                0,
                0,
                LocalDateTime.now()
        );

        bookRepository.save(book);
        logger.info("Імпортовано книгу: {}", book.title());
        return book;
    }

    /**
     * Заглушка для імпорту жанрів (буде реалізовано пізніше).
     */
    public void importGenres(Path path, String encoding) {
        logger.info("Запуск імпорту жанрів з файлу: {} (Кодування: {})", path, encoding);
        // Тут буде реальна логіка
    }
}