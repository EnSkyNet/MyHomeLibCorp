package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JavaFX Task для фонового імпорту книг з можливістю відображення прогресу.
 */
public class BookImportTask extends Task<Void> {

    private static final Logger logger = LoggerFactory.getLogger(BookImportTask.class);
    private final List<File> filesToImport;
    private final SqliteBookRepository bookRepository;
    private final Connection connection;

    public BookImportTask(List<File> filesToImport, SqliteBookRepository bookRepository, Connection connection) {
        this.filesToImport = filesToImport;
        this.bookRepository = bookRepository;
        this.connection = connection;
    }

    @Override
    protected Void call() throws Exception {
        updateMessage("Підготовка до імпорту книг...");
        int totalFiles = filesToImport.size();

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try {
            for (int i = 0; i < totalFiles; i++) {
                if (isCancelled()) {
                    connection.rollback();
                    updateMessage("Імпорт скасовано користувачем.");
                    return null;
                }

                File file = filesToImport.get(i);
                updateMessage("Обробка файлу: " + file.getName());

                // Тимчасовий мок-об'єкт, в реальності тут парсинг FB2
                Book mockBook = new Book(
                        System.nanoTime(),
                        file.getName().replace(".fb2", ""),
                        List.of(new Author(0L, "Невідомий Автор", "", "")),
                        List.of("sf"),
                        "Локальна серія",
                        0,
                        file.getName(),
                        file.getParent() != null ? file.getParent() : "",
                        file.getName(),
                        "ru",
                        file.length(),
                        "",
                        "Автоматично імпортована книга через Task.",
                        0,
                        0,
                        LocalDateTime.now()
                );

                bookRepository.save(mockBook);

                updateProgress(i + 1, totalFiles);
            }

            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            logger.error("Помилка під час імпорту", e);
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }

        updateMessage("Імпорт успішно завершено! Оброблено файлів: " + totalFiles);
        return null;
    }
}