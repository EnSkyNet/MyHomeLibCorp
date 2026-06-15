package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import javafx.concurrent.Task;
import java.io.File;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

public class BookImportTask extends Task<Void> {
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
        updateMessage("Підготовка до імпорту...");
        int total = filesToImport.size();
        boolean prevAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            for (int i = 0; i < total; i++) {
                if (isCancelled()) { connection.rollback(); updateMessage("Скасовано"); return null; }
                File file = filesToImport.get(i);
                updateMessage("Обробка: " + file.getName());
                Book mock = new Book(System.nanoTime(), file.getName().replace(".fb2",""),
                        List.of(new Author(0L,"Невідомий Автор","","")), List.of("sf"),
                        "Локальна серія",0, file.getName(), file.getParent()!=null?file.getParent():"",
                        file.getName(),"ru", file.length(),"","Автоматично імпортовано",0,0, LocalDateTime.now());
                bookRepository.save(mock);
                updateProgress(i+1, total);
            }
            connection.commit();
        } catch (Exception e) { connection.rollback(); throw e; }
        finally { connection.setAutoCommit(prevAutoCommit); }
        updateMessage("Імпорт завершено. Оброблено: " + total);
        return null;
    }
}