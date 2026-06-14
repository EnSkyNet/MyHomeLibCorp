package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Fb2Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Високопродуктивний парсер FB2 метаданих на базі StAX.
 * Читає тільки заголовок (title-info), ігноруючи тіло книги для економії пам'яті.
 */
public final class Fb2Importer {

    private static final Logger logger = LoggerFactory.getLogger(Fb2Importer.class);

    public Fb2Book parseFb2(InputStream inputStream, Path sourcePath, String archiveEntry, long fileSize) {
        String title = "Без назви";
        List<Author> authors = new ArrayList<>();
        List<String> genres = new ArrayList<>();
        String series = "";
        Integer sequenceNumber = 0;
        String language = "ru";
        String keywords = "";
        StringBuilder annotationBuilder = new StringBuilder();

        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            // Захист від XXE та entity expansion
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);

            String currentElement = "";
            String fName = "";
            String mName = "";
            String lName = "";

            boolean inTitleInfo = false;
            boolean inAnnotation = false;

            while (reader.hasNext()) {
                int event = reader.next();

                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        currentElement = reader.getLocalName();

                        if ("title-info".equals(currentElement)) {
                            inTitleInfo = true;
                        } else if ("annotation".equals(currentElement) && inTitleInfo) {
                            inAnnotation = true;
                        } else if ("author".equals(currentElement) && inTitleInfo) {
                            fName = "";
                            mName = "";
                            lName = "";
                        } else if ("sequence".equals(currentElement) && inTitleInfo) {
                            String nameAttr = reader.getAttributeValue(null, "name");
                            if (nameAttr != null && !nameAttr.isBlank()) {
                                series = nameAttr.trim();
                            }
                            String numberAttr = reader.getAttributeValue(null, "number");
                            if (numberAttr != null) {
                                try {
                                    sequenceNumber = Integer.parseInt(numberAttr.trim());
                                } catch (NumberFormatException e) {
                                    sequenceNumber = 0;
                                }
                            }
                        }
                        break;

                    case XMLStreamConstants.CHARACTERS:
                        String text = reader.getText();
                        if (text == null || text.isEmpty()) break;

                        if (inTitleInfo) {
                            if (inAnnotation) {
                                annotationBuilder.append(text);
                            } else {
                                String trimmedText = text.trim();
                                if (trimmedText.isEmpty()) break;

                                switch (currentElement) {
                                    case "book-title":
                                        title = trimmedText;
                                        break;
                                    case "first-name":
                                        fName = trimmedText;
                                        break;
                                    case "middle-name":
                                        mName = trimmedText;
                                        break;
                                    case "last-name":
                                        lName = trimmedText;
                                        break;
                                    case "genre":
                                        if (!genres.contains(trimmedText)) {
                                            genres.add(trimmedText);
                                        }
                                        break;
                                    case "lang":
                                        language = trimmedText.toLowerCase();
                                        break;
                                    case "keywords":
                                        keywords = trimmedText;
                                        break;
                                }
                            }
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        String endElement = reader.getLocalName();

                        if ("title-info".equals(endElement)) {
                            inTitleInfo = false;
                            // Достроковий вихід, оскільки тіло книги нам не потрібне
                            break;
                        } else if ("annotation".equals(endElement)) {
                            inAnnotation = false;
                        } else if ("author".equals(endElement) && inTitleInfo) {
                            if (!lName.isEmpty() || !fName.isEmpty()) {
                                authors.add(new Author(0, fName, mName, lName));
                            }
                        }

                        if (!inAnnotation) {
                            currentElement = "";
                        }
                        break;
                }

                // Якщо вийшли з title-info – припиняємо парсинг
                if (!inTitleInfo && event == XMLStreamConstants.END_ELEMENT && "title-info".equals(reader.getLocalName())) {
                    break;
                }
            }
            reader.close();
        } catch (Exception e) {
            logger.warn("Помилка парсингу FB2, використовуємо ім'я файлу", e);
            if (archiveEntry != null && !archiveEntry.isEmpty()) {
                title = archiveEntry;
                if (title.contains("/")) {
                    title = title.substring(title.lastIndexOf('/') + 1);
                }
            } else if (sourcePath != null) {
                title = sourcePath.getFileName().toString();
            }
            title = title.replace(".fb2", "");
        }

        if (title == null || title.isBlank()) {
            title = (archiveEntry != null && !archiveEntry.isEmpty()) ? archiveEntry : sourcePath.getFileName().toString();
        }

        return new Fb2Book(
                title,
                authors,
                genres,
                series,
                sequenceNumber,
                language,
                sourcePath,
                archiveEntry,
                fileSize,
                keywords,
                annotationBuilder.toString().replaceAll("\\s+", " ").trim()
        );
    }
}