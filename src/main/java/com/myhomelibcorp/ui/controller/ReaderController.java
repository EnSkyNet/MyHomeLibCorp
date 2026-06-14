package com.myhomelibcorp.ui.controller;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.service.LibraryService;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контролер для вікна читання книги.
 * Відображає HTML-вміст книги у WebView.
 */
public class ReaderController {

    private static final Logger logger = LoggerFactory.getLogger(ReaderController.class);

    @FXML
    private WebView webView;

    /**
     * Встановлює книгу для читання та завантажує її вміст.
     * @param book книга для читання
     * @param service сервіс бібліотеки (для отримання HTML)
     */
    public void setBook(Book book, LibraryService service) {
        try {
            String html = service.readBookHtml(book);
            webView.getEngine().loadContent(html);
            logger.info("Відкрито книгу для читання: {}", book.title());
        } catch (Exception e) {
            logger.error("Помилка завантаження книги для читання", e);
            String errorHtml = String.format("""
                <html><body style="font-family: monospace; padding: 20px;">
                <h2>Не вдалося завантажити книгу</h2>
                <p><b>%s: %s</b></p>
                <hr/>
                <p>Деталі книги:<br/>
                Назва: %s<br/>
                Папка: %s<br/>
                Ім'я файлу: %s<br/>
                Запис в архіві: %s<br/>
                В архіві: %s</p>
                <pre>%s</pre>
                </body></html>
                """,
                    e.getClass().getSimpleName(),
                    escapeHtml(e.getMessage()),
                    escapeHtml(book.title()),
                    escapeHtml(book.folder()),
                    escapeHtml(book.fileName()),
                    escapeHtml(book.archiveEntry()),
                    book.hasArchiveEntry(),
                    escapeHtml(getStackTrace(e))
            );
            webView.getEngine().loadContent(errorHtml);
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String getStackTrace(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append(e.toString()).append("\n");
        }
        return sb.toString();
    }
}