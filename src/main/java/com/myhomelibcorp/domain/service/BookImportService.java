package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервіс для асинхронного імпорту книг з INPX-файлів.
 */
public class BookImportService {

    private static final Logger logger = LoggerFactory.getLogger(BookImportService.class);
    private final InpxParser inpxParser;
    private final DatabaseManager databaseManager;

    public BookImportService() {
        this.inpxParser = new InpxParser();
        this.databaseManager = new DatabaseManager("library.db");
    }

    /**
     * Створює JavaFX Task для імпорту файлів INPX у фоновому потоці.
     * @param filesToImport список шляхів до файлів .inpx
     * @return Task, який можна запустити в окремому потоці
     */
    public Task<Integer> createImportTask(List<Path> filesToImport) {
        return new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                AtomicInteger totalImported = new AtomicInteger(0);
                int totalFiles = filesToImport.size();

                updateProgress(0, totalFiles);
                updateMessage("Запуск процесу імпорту...");

                for (int i = 0; i < totalFiles; i++) {
                    if (isCancelled()) {
                        updateMessage("Імпорт перервано користувачем.");
                        break;
                    }

                    Path filePath = filesToImport.get(i);
                    updateMessage("Обробка файлу [" + (i + 1) + " з " + totalFiles + "]: " + filePath.getFileName());

                    if (filePath.toString().toLowerCase().endsWith(".inpx")) {
                        try (InputStream is = new FileInputStream(filePath.toFile())) {
                            inpxParser.parseInpxStream(is, 5000, bookBatch -> {
                                totalImported.addAndGet(bookBatch.size());
                                databaseManager.saveBooksBatch(bookBatch);
                                updateMessage("Завантажено книг в індекс SQLite: " + totalImported.get());
                            });
                        }
                    } else {
                        totalImported.incrementAndGet();
                        Thread.sleep(20); // імітація обробки
                    }

                    updateProgress(i + 1, totalFiles);
                }

                String msg = "Імпорт завершено успішно! Додано об'єктів: " + totalImported.get();
                updateMessage(msg);
                logger.info(msg);
                return totalImported.get();
            }
        };
    }
}