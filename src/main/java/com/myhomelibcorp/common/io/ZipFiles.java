package com.myhomelibcorp.common.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * Допоміжний клас для роботи з ZIP-архівами.
 * Автоматично визначає кодування імен файлів всередині архіву (UTF-8, CP866, Windows-1251, ISO-8859-1).
 */
public final class ZipFiles {

    private static final List<Charset> ZIP_NAME_CHARSETS = List.of(
            StandardCharsets.UTF_8,
            Charset.forName("CP866"),
            Charset.forName("windows-1251"),
            StandardCharsets.ISO_8859_1
    );

    private ZipFiles() {
        // Приватний конструктор, щоб заборонити створення екземплярів
    }

    /**
     * Відкриває ZIP-файл з автоматичним визначенням кодування імен записів.
     * @param file шлях до ZIP-архіву
     * @return відкритий ZipFile
     * @throws IOException якщо не вдалося відкрити архів жодним із відомих кодувань
     */
    public static ZipFile open(Path file) throws IOException {
        IOException last = null;
        for (Charset charset : ZIP_NAME_CHARSETS) {
            try {
                return new ZipFile(file.toFile(), charset);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last == null ? new IOException("Не вдалося відкрити ZIP: " + file) : last;
    }
}