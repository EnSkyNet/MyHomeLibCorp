package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.common.io.ZipFiles;
import com.myhomelibcorp.domain.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Сервіс для читання вмісту книги та перетворення його в HTML для відображення у WebView.
 */
public class BookContentService {

    private static final Logger logger = LoggerFactory.getLogger(BookContentService.class);

    public String readBookHtml(Book book) throws Exception {
        byte[] data = readBookBytes(book);
        if (data == null || data.length == 0) {
            return errorHtml("Вміст книги порожній", book);
        }

        Document doc = parseXml(data);
        String title = getElementText(doc, "book-title", book.title());
        List<String> paragraphs = getBodyParagraphs(doc);
        if (paragraphs.isEmpty()) {
            paragraphs.add("У цій книзі не знайдено тексту для читання.");
        }

        StringBuilder html = new StringBuilder();
        html.append("""
            <html><head><meta charset="UTF-8"></head>
            <body style="font-family: Georgia, 'Times New Roman', serif; background: #f4f1ea; color: #1f2933; line-height: 1.58; padding: 34px; max-width: 900px; margin: 0 auto;">
            <h1 style="font-family: Segoe UI, Arial; font-size: 30px;">""")
                .append(escapeHtml(title)).append("</h1>");
        html.append("<p><b>").append(escapeHtml(book.authorsText())).append("</b></p>");
        for (String p : paragraphs) {
            html.append("<p>").append(escapeHtml(p)).append("</p>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    private byte[] readBookBytes(Book book) throws Exception {
        logger.debug("Читання книги: {}", book.title());
        logger.debug("Папка: {}", book.folder());
        logger.debug("Ім'я файлу: {}", book.fileName());
        logger.debug("Запис в архіві: {}", book.archiveEntry());

        if (book.hasArchiveEntry()) {
            return readFromZip(book);
        } else {
            return readFromFileSystem(book);
        }
    }

    private byte[] readFromFileSystem(Book book) throws Exception {
        Path filePath = Path.of(book.folder(), book.fileName());
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            logger.debug("Читання з ФС: {}", filePath);
            return Files.readAllBytes(filePath);
        }
        filePath = Path.of(book.fileName());
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            logger.debug("Читання з ФС (запасний варіант): {}", filePath);
            return Files.readAllBytes(filePath);
        }
        throw new Exception("Файл не знайдено: " + book.folder() + "/" + book.fileName() + " або " + book.fileName());
    }

    private byte[] readFromZip(Book book) throws Exception {
        Path zipPath = Path.of(book.folder());
        if (!Files.exists(zipPath) || !Files.isRegularFile(zipPath)) {
            zipPath = Path.of(book.fileName());
            if (!Files.exists(zipPath)) {
                throw new Exception("ZIP-файл не знайдено: " + book.folder() + " або " + book.fileName());
            }
        }
        try (ZipFile zip = ZipFiles.open(zipPath)) {
            ZipEntry entry = zip.getEntry(book.archiveEntry());
            if (entry == null) {
                entry = zip.stream()
                        .filter(e -> !e.isDirectory() && e.getName().toLowerCase().endsWith(".fb2"))
                        .findFirst()
                        .orElse(null);
                if (entry == null) throw new Exception("У ZIP не знайдено жодного FB2 запису: " + zipPath);
            }
            try (InputStream is = zip.getInputStream(entry)) {
                return is.readAllBytes();
            }
        }
    }

    private Document parseXml(byte[] data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new ByteArrayInputStream(data)));
    }

    private String getElementText(Document doc, String tag, String fallback) {
        NodeList nodes = doc.getElementsByTagNameNS("*", tag);
        if (nodes.getLength() == 0) nodes = doc.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return fallback;
        String text = nodes.item(0).getTextContent();
        return (text == null || text.isBlank()) ? fallback : text.trim();
    }

    private List<String> getBodyParagraphs(Document doc) {
        List<String> paras = new ArrayList<>();
        NodeList bodies = doc.getElementsByTagNameNS("*", "body");
        if (bodies.getLength() == 0) bodies = doc.getElementsByTagName("body");
        for (int i = 0; i < bodies.getLength(); i++) {
            Element body = (Element) bodies.item(i);
            NodeList pNodes = body.getElementsByTagNameNS("*", "p");
            if (pNodes.getLength() == 0) pNodes = body.getElementsByTagName("p");
            for (int j = 0; j < pNodes.getLength(); j++) {
                String text = pNodes.item(j).getTextContent();
                if (text != null && !text.isBlank()) {
                    paras.add(text.replaceAll("\\s+", " ").trim());
                }
            }
        }
        return paras;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String errorHtml(String error, Book book) {
        return String.format("""
            <html><body style="font-family: monospace; padding: 20px;">
            <h2>Не вдалося завантажити книгу</h2>
            <p><b>%s</b></p>
            <hr/>
            <p>Деталі книги:<br/>
            Назва: %s<br/>
            Папка: %s<br/>
            Ім'я файлу: %s<br/>
            Запис в архіві: %s<br/>
            В архіві: %s</p>
            </body></html>
            """,
                escapeHtml(error),
                escapeHtml(book.title()),
                escapeHtml(book.folder()),
                escapeHtml(book.fileName()),
                escapeHtml(book.archiveEntry()),
                book.hasArchiveEntry()
        );
    }
}