package com.myhomelibcorp.app;

import com.myhomelibcorp.domain.service.LibraryService;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.database.sqlite.SqliteBookRepository;
import com.myhomelibcorp.infrastructure.importer.Fb2Importer;
import com.myhomelibcorp.ui.controller.MainController;
import com.myhomelibcorp.ui.viewmodel.LibraryViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        String fxmlResourcePath = "com/myhomelibcorp/ui/view/MainView.fxml";
        URL fxmlLocation = getClass().getClassLoader().getResource(fxmlResourcePath);
        if (fxmlLocation == null) {
            fxmlLocation = getClass().getResource("/" + fxmlResourcePath);
        }
        if (fxmlLocation == null) {
            File directFile = new File("src/main/resources/" + fxmlResourcePath);
            if (!directFile.exists()) {
                directFile = new File("MyHomeLibCorp/src/main/resources/" + fxmlResourcePath);
            }
            if (directFile.exists()) {
                fxmlLocation = directFile.toURI().toURL();
            }
        }
        if (fxmlLocation == null) {
            throw new java.io.FileNotFoundException("FXML file not found: " + fxmlResourcePath);
        }

        DatabaseManager dbManager = new DatabaseManager("library.db");
        LibraryService libraryService = new LibraryService(dbManager);
        SqliteBookRepository bookRepository = new SqliteBookRepository(dbManager);
        Fb2Importer fb2Importer = new Fb2Importer();
        LibraryViewModel viewModel = new LibraryViewModel(bookRepository, fb2Importer);

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();
        MainController controller = loader.getController();
        controller.setDependencies(dbManager, libraryService, viewModel);

        primaryStage.setTitle("MyHomeLibCorp - Java Port [800 000 books]");
        primaryStage.setScene(new Scene(root, 1150, 720));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}