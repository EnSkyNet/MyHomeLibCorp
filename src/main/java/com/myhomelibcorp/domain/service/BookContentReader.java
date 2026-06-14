package com.myhomelibcorp.domain.service;

import com.myhomelibcorp.domain.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Клас для читання сирого тексту книги (без форматування).
 * Використовується в тих місцях, де потрібен чистий текст.
 */
public final class BookContentReader {

    private static final Logger logger = LoggerFactory.getLogger(BookContentReader.class);

    /**
     * Читає книгу та повертає її текстовий вміст (без HTML).
     * @param book книга
     * @return текстовий рядок
     */
    public String readBookText(Book book) throws IOException {
        if (book == null) {
            throw new IllegalArgumentException("Об'єкт книги не може бути null");
        }

        byte[] rawBytes = readBookBytes(book);
        if (rawBytes == null || rawBytes.length == 0) {
            return "Файл порожній або не знайдений";
        }

        return new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Читає сирі байти книги (з файлової системи або з архіву).
     */
    public byte[] readBookBytes(Book book) throws IOException {
        if (book.hasArchiveEntry()) {
            Path zipFilePath = Paths.get(book.folder());
            if (!Files.exists(zipFilePath)) {
                throw new IOException("Файл архіву не знайдено за шляхом: " + zipFilePath);
            }

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFilePath.toFile())))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(book.archiveEntry())) {
                        byte[] data = readAllBytesFromStream(zis);
                        zis.closeEntry();
                        return data;
                    }
                    zis.closeEntry();
                }
            }
            throw new IOException("Запис '" + book.archiveEntry() + "' відсутній всередині архіву " + book.folder());
        } else {
            Path plainFilePath = Paths.get(book.folder(), book.fileName());
            if (!Files.exists(plainFilePath)) {
                plainFilePath = Paths.get(book.fileName());
                if (!Files.exists(plainFilePath)) {
                    throw new IOException("Файл книги не знайдено за шляхом: " + book.folder() + " / " + book.fileName());
                }
            }

            try (InputStream is = new BufferedInputStream(new FileInputStream(plainFilePath.toFile()))) {
                return readAllBytesFromStream(is);
            }
        }
    }

    private byte[] readAllBytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }
}