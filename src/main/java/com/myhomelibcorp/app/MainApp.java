package com.myhomelibcorp.app;

import com.myhomelibcorp.domain.model.Author;
import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.service.LibraryService;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteGenreRepository;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import com.myhomelibcorp.infrastructure.importer.GenreListImporter;
import com.myhomelibcorp.infrastructure.settings.JsonSettingsStore;
import com.myhomelibcorp.ui.controller.MainController;
import com.myhomelibcorp.ui.viewmodel.LibraryViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public class MainApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    @Override
    public void start(Stage primaryStage) throws Exception {
        String fxmlResourcePath = "com/myhomelibcorp/ui/view/MainView.fxml";
        URL fxmlLocation = getClass().getClassLoader().getResource(fxmlResourcePath);
        if (fxmlLocation == null) fxmlLocation = getClass().getResource("/" + fxmlResourcePath);
        if (fxmlLocation == null) {
            File directFile = new File("src/main/resources/" + fxmlResourcePath);
            if (!directFile.exists()) directFile = new File("MyHomeLibCorp/src/main/resources/" + fxmlResourcePath);
            if (directFile.exists()) fxmlLocation = directFile.toURI().toURL();
        }
        if (fxmlLocation == null) throw new java.io.FileNotFoundException("FXML файл не знайдено: " + fxmlResourcePath);

        JsonSettingsStore settingsStore = new JsonSettingsStore();
        String lastDbPath = settingsStore.getString("lastOpenedCollection", "library.db");
        DatabaseManager dbManager = new DatabaseManager(lastDbPath);
        SqliteGenreRepository genreRepository = new SqliteGenreRepository(dbManager);

        // Примусовий імпорт жанрів (якщо файл існує, завжди імпортуємо)
        URL genreFileUrl = getClass().getClassLoader().getResource("genres_fb2.txt");
        if (genreFileUrl != null) {
            try {
                Path genreFilePath = Path.of(genreFileUrl.toURI());
                GenreListImporter genreImporter = new GenreListImporter(dbManager, genreRepository);
                genreImporter.importGenresFromFile(genreFilePath);
                logger.info("Жанри імпортовано");
            } catch (Exception e) {
                logger.error("Помилка імпорту жанрів", e);
            }
        } else {
            logger.warn("Файл genres_fb2.txt не знайдено");
        }

        LibraryService libraryService = new LibraryService(dbManager, settingsStore);
        SqliteBookRepository bookRepository = new SqliteBookRepository(dbManager);
        Fb2Importer fb2Importer = new Fb2Importer();
        LibraryViewModel viewModel = new LibraryViewModel(bookRepository, fb2Importer);

        // Додаємо тестову книгу, якщо база порожня
        if (bookRepository.getBookCount() == 0) {
            Author testAuthor = new Author(1L, "Тестовий", "", "Автор");
            List<Author> authors = List.of(testAuthor);
            // Використовуємо коди, які точно є в файлі жанрів
            List<String> genreCodes = List.of("sf", "sf_fantasy");
            Book testBook = new Book(1L, "Тестова книга", authors, genreCodes, "Тестова серія", 1, "uk", "test.fb2", "", "", 1024, "", "Тестова книга", 0, 0, LocalDateTime.now());
            bookRepository.save(testBook);
            bookRepository.saveAuthorsForBook(testBook.id(), authors);
            bookRepository.saveGenresForBook(testBook.id(), genreCodes);
            bookRepository.refreshFtsForBook(testBook.id());
            logger.info("Додано тестову книгу");
        }

        // Завантажуємо книги
        viewModel.loadAllBooks();

        // Оновлюємо FTS-індекс для всіх книг (на випадок, якщо жанри змінилися)
        bookRepository.refreshAllFts();

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setDependencies(dbManager, libraryService, viewModel, settingsStore);
        primaryStage.setTitle("MyHomeLibCorp - Java порт [800 000 книг]");
        primaryStage.setScene(new Scene(root, 1150, 720));
        primaryStage.show();
    }

    public static void main(String[] args) { launch(args); }
}