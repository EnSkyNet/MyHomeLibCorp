package com.myhomelibcorp.infrastructure.importer;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Fb2Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.stream.*;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class Fb2Importer {
    private static final Logger logger = LoggerFactory.getLogger(Fb2Importer.class);
    public Fb2Book parseFb2(InputStream inputStream, Path sourcePath, String archiveEntry, long fileSize) {
        String title = "Без назви";
        List<Author> authors = new ArrayList<>();
        List<String> genres = new ArrayList<>();
        String series = "";
        Integer seqNumber = 0;
        String language = "ru";
        String keywords = "";
        StringBuilder annotation = new StringBuilder();
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLStreamReader reader = factory.createXMLStreamReader(inputStream);
            String current = "", fName = "", mName = "", lName = "";
            boolean inTitle = false, inAnnotation = false;
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT:
                        current = reader.getLocalName();
                        if ("title-info".equals(current)) inTitle = true;
                        else if ("annotation".equals(current) && inTitle) inAnnotation = true;
                        else if ("author".equals(current) && inTitle) { fName = mName = lName = ""; }
                        else if ("sequence".equals(current) && inTitle) {
                            String nameAttr = reader.getAttributeValue(null, "name");
                            if (nameAttr != null && !nameAttr.isBlank()) series = nameAttr.trim();
                            String numAttr = reader.getAttributeValue(null, "number");
                            if (numAttr != null) try { seqNumber = Integer.parseInt(numAttr.trim()); } catch (NumberFormatException e) { seqNumber = 0; }
                        }
                        break;
                    case XMLStreamConstants.CHARACTERS:
                        String text = reader.getText();
                        if (text == null || text.isBlank()) break;
                        if (inTitle) {
                            if (inAnnotation) annotation.append(text);
                            else {
                                String trim = text.trim();
                                if (trim.isEmpty()) break;
                                switch (current) {
                                    case "book-title": title = trim; break;
                                    case "first-name": fName = trim; break;
                                    case "middle-name": mName = trim; break;
                                    case "last-name": lName = trim; break;
                                    case "genre": if (!genres.contains(trim)) genres.add(trim); break;
                                    case "lang": language = trim.toLowerCase(); break;
                                    case "keywords": keywords = trim; break;
                                }
                            }
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        String end = reader.getLocalName();
                        if ("title-info".equals(end)) inTitle = false;
                        else if ("annotation".equals(end)) inAnnotation = false;
                        else if ("author".equals(end) && inTitle && (!lName.isEmpty() || !fName.isEmpty())) authors.add(new Author(0, fName, mName, lName));
                        if (!inAnnotation) current = "";
                        break;
                }
                if (!inTitle && event == XMLStreamConstants.END_ELEMENT && "title-info".equals(reader.getLocalName())) break;
            }
            reader.close();
        } catch (Exception e) {
            logger.warn("Помилка парсингу FB2", e);
            if (archiveEntry != null && !archiveEntry.isEmpty()) title = archiveEntry;
            else if (sourcePath != null) title = sourcePath.getFileName().toString();
            title = title.replace(".fb2", "");
        }
        if (title.isBlank()) title = (archiveEntry != null && !archiveEntry.isEmpty()) ? archiveEntry : sourcePath.getFileName().toString();
        return new Fb2Book(title, authors, genres, series, seqNumber, language, sourcePath, archiveEntry, fileSize, keywords, annotation.toString().replaceAll("\\s+", " ").trim());
    }
}