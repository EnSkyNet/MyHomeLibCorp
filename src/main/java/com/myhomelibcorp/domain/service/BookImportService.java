package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import javafx.concurrent.Task;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BookImportService {
    private final InpxParser inpxParser;
    private final DatabaseManager databaseManager;
    public BookImportService() {
        this.inpxParser = new InpxParser();
        this.databaseManager = new DatabaseManager("library.db");
    }
    public Task<Integer> createImportTask(List<Path> filesToImport) {
        return new Task<>() {
            @Override
            protected Integer call() throws Exception {
                AtomicInteger totalImported = new AtomicInteger(0);
                int total = filesToImport.size();
                updateProgress(0, total);
                updateMessage("Запуск імпорту...");
                for (int i = 0; i < total; i++) {
                    if (isCancelled()) { updateMessage("Імпорт скасовано"); break; }
                    Path file = filesToImport.get(i);
                    updateMessage("Обробка [" + (i+1) + "/" + total + "]: " + file.getFileName());
                    if (file.toString().toLowerCase().endsWith(".inpx")) {
                        try (InputStream is = new FileInputStream(file.toFile())) {
                            inpxParser.parseInpxStream(is, 5000, batch -> {
                                totalImported.addAndGet(batch.size());
                                databaseManager.saveBooksBatch(batch);
                                updateMessage("Завантажено книг: " + totalImported.get());
                            });
                        }
                    } else {
                        totalImported.incrementAndGet();
                        Thread.sleep(20);
                    }
                    updateProgress(i+1, total);
                }
                updateMessage("Імпорт завершено. Додано: " + totalImported.get());
                return totalImported.get();
            }
        };
    }
}