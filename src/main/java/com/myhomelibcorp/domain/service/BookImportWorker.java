package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteAuthorRepository;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteGenreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Фоновий робітник для імпорту книг з черги.
 * Обробляє партії книг у транзакціях.
 */
public class BookImportWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(BookImportWorker.class);

    private final BlockingQueue<List<Book>> queue;
    private final DatabaseManager databaseManager;
    private final SqliteBookRepository bookRepository;
    private final SqliteAuthorRepository authorRepository;
    private final SqliteGenreRepository genreRepository;
    private final CountDownLatch latch;
    private volatile boolean running = true;

    public BookImportWorker(BlockingQueue<List<Book>> queue,
                            DatabaseManager databaseManager,
                            SqliteBookRepository bookRepository,
                            SqliteAuthorRepository authorRepository,
                            SqliteGenreRepository genreRepository,
                            CountDownLatch latch) {
        this.queue = queue;
        this.databaseManager = databaseManager;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.genreRepository = genreRepository;
        this.latch = latch;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        try {
            while (running || !queue.isEmpty()) {
                List<Book> batch = queue.poll(100, TimeUnit.MILLISECONDS);
                if (batch != null && !batch.isEmpty()) {
                    processBatchWithTransaction(batch);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Потік імпорту перервано");
        } finally {
            latch.countDown();
            logger.info("Робітник імпорту завершив роботу");
        }
    }

    private void processBatchWithTransaction(List<Book> batch) {
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            conn.setAutoCommit(false);

            for (Book book : batch) {
                bookRepository.save(book);
                if (book.authors() != null) {
                    for (Author author : book.authors()) {
                        authorRepository.save(author);
                    }
                }
                if (book.genres() != null) {
                    for (String genre : book.genres()) {
                        genreRepository.saveGenre(genre, genre); // тимчасово код = назва
                    }
                }
            }

            conn.commit();
            logger.debug("Збережено партію з {} книг", batch.size());
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.error("Відкат транзакції через помилку", e);
                } catch (SQLException ex) {
                    logger.error("Помилка при відкаті", ex);
                }
            }
            throw new RuntimeException("Помилка збереження партії книг", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Помилка закриття з'єднання", e);
                }
            }
        }
    }
}