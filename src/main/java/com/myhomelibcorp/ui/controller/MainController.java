package com.myhomelibcorp.ui.controller;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.BookEdit;
import com.myhomelibcorp.domain.service.LibraryService;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.ui.viewmodel.LibraryViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Головний контролер програми. Обробляє дії користувача та оновлює інтерфейс.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // ==================== JavaFX компоненти ====================
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleCol, authorCol, seriesCol, langCol, fileNameCol, genreCol, sizeCol;
    @FXML private TableColumn<Book, Integer> seqNumberCol, rateCol, progressCol;
    @FXML private Label detailTitleLabel, detailAuthorLabel, detailSeriesLabel, detailGenresLabel;
    @FXML private Label detailLanguageLabel, detailFileLabel, detailSizeLabel, detailRateLabel, detailProgressLabel;
    @FXML private TextArea detailKeywordsArea, detailAnnotationArea, detailReviewArea;
    @FXML private ListView<String> authorsList, seriesList, genresList, groupsList;
    @FXML private TextField searchField;
    @FXML private Label statusLeftLabel, statusRightLabel;

    // ==================== Залежності ====================
    private LibraryService libraryService;
    private LibraryViewModel viewModel;
    private DatabaseManager dbManager;
    private Path currentDbPath = Path.of("library.db");

    /**
     * Встановлює залежності після завантаження FXML.
     */
    public void setDependencies(DatabaseManager dbManager, LibraryService service, LibraryViewModel vm) {
        this.dbManager = dbManager;
        this.libraryService = service;
        this.viewModel = vm;
        this.bookTable.setItems(viewModel.getBooksList());
        refreshAll();
    }

    @FXML
    public void initialize() {
        // Налаштування колонок таблиці
        titleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().title()));
        authorCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().authorsText()));
        seriesCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().series()));
        seqNumberCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(
                cell.getValue().sequenceNumber() == null ? 0 : cell.getValue().sequenceNumber()).asObject());
        genreCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().genresText()));
        langCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().language()));
        rateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().rate()).asObject());
        progressCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleIntegerProperty(cell.getValue().progress()).asObject());
        fileNameCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().fileName()));
        sizeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(formatSize(cell.getValue().fileSize())));

        // Слухач вибору книги
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, book) -> showBookDetails(book));

        // Слухач пошуку
        searchField.textProperty().addListener((obs, old, query) -> refreshBooks());

        // Клік по лівій панелі встановлює текст пошуку
        authorsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        seriesList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        genresList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
    }

    // ==================== Оновлення даних ====================
    private void showBookDetails(Book book) {
        if (book == null) return;
        detailTitleLabel.setText(book.title());
        detailAuthorLabel.setText(book.authorsText());
        detailSeriesLabel.setText((book.series() != null ? book.series() : "") +
                (book.sequenceNumber() != null ? " #" + book.sequenceNumber() : ""));
        detailGenresLabel.setText(book.genresText());
        detailLanguageLabel.setText(book.language());
        detailFileLabel.setText(book.fileName());
        detailSizeLabel.setText(formatSize(book.fileSize()));
        detailRateLabel.setText(String.valueOf(book.rate()));
        detailProgressLabel.setText(book.progress() + "%");
        detailKeywordsArea.setText(book.keywords());
        detailAnnotationArea.setText(book.annotation());
        detailReviewArea.setText(libraryService.getReview(book.id()));
    }

    private void refreshBooks() {
        List<Book> books = libraryService.searchBooks(searchField.getText());
        viewModel.getBooksList().setAll(books);
        updateStatus();
    }

    private void refreshLeftLists() {
        authorsList.setItems(FXCollections.observableArrayList(libraryService.listAuthors()));
        seriesList.setItems(FXCollections.observableArrayList(libraryService.listSeries()));
        genresList.setItems(FXCollections.observableArrayList(libraryService.listGenres()));
        groupsList.setItems(FXCollections.observableArrayList(libraryService.listGroups()));
    }

    private void refreshAll() {
        refreshBooks();
        refreshLeftLists();
        updateStatus();
    }

    private void updateStatus() {
        statusRightLabel.setText("Всього книг: " + viewModel.getBooksList().size());
    }

    private Book getSelectedBook() {
        return bookTable.getSelectionModel().getSelectedItem();
    }

    // ==================== Дії користувача ====================
    @FXML private void handleOpenCollection() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Відкрити або створити колекцію");
        chooser.setInitialFileName("library.db");
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            currentDbPath = file.toPath();
            refreshAll();
            statusLeftLabel.setText("Відкрито колекцію: " + file.getName());
        }
    }

    @FXML private void handleShowCollections() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Зареєстровані колекції");
        alert.setHeaderText("Поточна колекція");
        alert.setContentText("База даних: " + currentDbPath.toAbsolutePath());
        alert.showAndWait();
    }

    @FXML private void handleImportFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Виберіть папку з FB2/ZIP файлами");
        File folder = chooser.showDialog(null);
        if (folder != null && folder.isDirectory()) {
            statusLeftLabel.setText("Імпорт з папки: " + folder.getName());
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    libraryService.importFolder(folder.toPath(), msg -> Platform.runLater(() -> statusLeftLabel.setText(msg)));
                    return null;
                }
            };
            task.setOnSucceeded(e -> { refreshAll(); statusLeftLabel.setText("Імпорт завершено з " + folder.getName()); });
            task.setOnFailed(e -> statusLeftLabel.setText("Помилка імпорту: " + task.getException().getMessage()));
            new Thread(task).start();
        }
    }

    @FXML private void handleImportInpx() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("INPX files", "*.inpx"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            statusLeftLabel.setText("Імпорт INPX: " + file.getName());
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    libraryService.importInpx(file.toPath(), msg -> Platform.runLater(() -> statusLeftLabel.setText(msg)));
                    return null;
                }
            };
            task.setOnSucceeded(e -> { refreshAll(); statusLeftLabel.setText("Імпорт INPX завершено"); });
            task.setOnFailed(e -> statusLeftLabel.setText("Помилка імпорту INPX: " + task.getException().getMessage()));
            new Thread(task).start();
        }
    }

    @FXML private void handleImportGenres() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Список жанрів", "*.glst", "*.txt"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                libraryService.importGenreList(file.toPath(), "fb2");
                refreshAll();
                statusLeftLabel.setText("Жанри імпортовано з " + file.getName());
            } catch (Exception ex) {
                statusLeftLabel.setText("Помилка імпорту жанрів: " + ex.getMessage());
            }
        }
    }

    @FXML private void handleRefresh() { refreshAll(); }

    @FXML private void handleReadBook() {
        Book book = getSelectedBook();
        if (book == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myhomelibcorp/ui/view/ReaderView.fxml"));
            Parent root = loader.load();
            ReaderController controller = loader.getController();
            controller.setBook(book, libraryService);
            Stage stage = new Stage();
            stage.setTitle("Читання: " + book.title());
            stage.setScene(new Scene(root, 800, 600));
            stage.show();
        } catch (Exception e) {
            logger.error("Не вдалося відкрити читалку", e);
            statusLeftLabel.setText("Помилка відкриття читалки: " + e.getMessage());
        }
    }

    @FXML private void handleAdvancedSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myhomelibcorp/ui/view/AdvancedSearchView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Розширений пошук");
            stage.setScene(new Scene(root, 550, 500));
            stage.showAndWait();

            AdvancedSearchController controller = loader.getController();
            if (controller.isSearchTriggered()) {
                AdvancedSearchController.SearchParams params = controller.getSearchParams();
                performAdvancedSearch(params);
            }
        } catch (Exception e) {
            logger.error("Помилка відкриття розширеного пошуку", e);
            statusLeftLabel.setText("Помилка відкриття розширеного пошуку: " + e.getMessage());
        }
    }

    /**
     * Виконує розширений пошук на основі параметрів з форми.
     */
    private void performAdvancedSearch(AdvancedSearchController.SearchParams params) {
        // Створюємо критерії пошуку
        SearchCriteria criteria = new SearchCriteria(
                params.title(),
                params.author(),
                params.genre(),
                params.series(),
                params.lang(),
                params.minRate(), null, null, null, null, null,
                "", "", "", "", ""
        );

        // Викликаємо пошук через LibraryService (або через SearchService)
        List<Book> results = libraryService.searchBooks(criteria.title()); // тимчасово, бо LibraryService поки не підтримує всі критерії
        // Покращена версія: якщо у LibraryService є метод findBooks(SearchCriteria), використовуйте його:
        // List<Book> results = libraryService.findBooks(criteria);

        viewModel.getBooksList().setAll(results);
        updateStatus();
        statusLeftLabel.setText("Знайдено книг: " + results.size());
    }

    @FXML private void handleOpenBookExternal() {
        Book book = getSelectedBook();
        if (book == null) return;
        try {
            Path filePath;
            if (!book.hasArchiveEntry()) {
                filePath = Path.of(book.folder(), book.fileName());
            } else {
                filePath = Path.of(book.folder());
            }
            if (Files.exists(filePath)) {
                Desktop.getDesktop().open(filePath.toFile());
                statusLeftLabel.setText("Відкрито: " + filePath);
            } else {
                statusLeftLabel.setText("Файл не знайдено: " + filePath);
                Path altPath = Path.of(book.fileName());
                if (Files.exists(altPath)) {
                    Desktop.getDesktop().open(altPath.toFile());
                    statusLeftLabel.setText("Відкрито: " + altPath);
                }
            }
        } catch (Exception e) {
            statusLeftLabel.setText("Помилка відкриття: " + e.getMessage());
        }
    }

    @FXML private void handleExportBook() {
        Book book = getSelectedBook();
        if (book == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(book.fileName());
        File dest = chooser.showSaveDialog(null);
        if (dest != null) {
            try {
                libraryService.exportBook(book, dest.toPath());
                statusLeftLabel.setText("Експортовано до " + dest.getName());
            } catch (Exception e) {
                statusLeftLabel.setText("Помилка експорту: " + e.getMessage());
            }
        }
    }

    @FXML private void handleEditBook() {
        Book book = getSelectedBook();
        if (book == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myhomelibcorp/ui/view/BookEditView.fxml"));
            DialogPane pane = loader.load();
            BookEditController controller = loader.getController();
            controller.setBook(book);
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(pane);
            dialog.setTitle("Редагування книги");
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                BookEdit edit = controller.getBookEdit();
                Book updated = new Book(
                        book.id(), edit.title(), book.authors(), book.genres(),
                        edit.series(), edit.sequenceNumber(), edit.language(),
                        book.fileName(), book.folder(), book.archiveEntry(), book.fileSize(),
                        edit.keywords(), edit.annotation(), edit.rate(), edit.progress(),
                        java.time.LocalDateTime.now()
                );
                libraryService.updateBook(updated);
                refreshAll();
                statusLeftLabel.setText("Книгу оновлено");
            }
        } catch (Exception e) {
            logger.error("Помилка редагування книги", e);
        }
    }

    @FXML private void handleEditReview() {
        Book book = getSelectedBook();
        if (book == null) return;
        TextInputDialog dialog = new TextInputDialog(libraryService.getReview(book.id()));
        dialog.setTitle("Редагувати рецензію");
        dialog.setHeaderText("Рецензія на " + book.title());
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(review -> {
            libraryService.setReview(book.id(), review);
            showBookDetails(book);
            statusLeftLabel.setText("Рецензію оновлено");
        });
    }

    @FXML private void handleAddToFavorites() {
        addToGroup("Favorites");
    }

    @FXML private void handleAddToGroup() {
        Book book = getSelectedBook();
        if (book == null) return;
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Додати до групи");
        dialog.setHeaderText("Введіть назву групи:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            libraryService.addBookToGroup(book.id(), name);
            statusLeftLabel.setText("Додано до групи: " + name);
        });
    }

    @FXML private void handleRemoveFromGroup() {
        Book book = getSelectedBook();
        if (book == null) return;
        List<String> groups = libraryService.getGroupsForBook(book.id());
        if (groups.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Книга не належить до жодної групи.").showAndWait();
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(groups.get(0), groups);
        dialog.setTitle("Видалити з групи");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(group -> {
            libraryService.removeBookFromGroup(book.id(), group);
            statusLeftLabel.setText("Видалено з групи: " + group);
        });
    }

    @FXML private void handleSetRate() {
        Book book = getSelectedBook();
        if (book == null) return;
        Spinner<Integer> spinner = new Spinner<>(0, 5, book.rate());
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Встановити оцінку");
        alert.getDialogPane().setContent(spinner);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryService.setRate(book.id(), spinner.getValue());
            refreshAll();
            statusLeftLabel.setText("Оцінка встановлена: " + spinner.getValue());
        }
    }

    @FXML private void handleSetProgress() {
        Book book = getSelectedBook();
        if (book == null) return;
        Slider slider = new Slider(0, 100, book.progress());
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Встановити прогрес");
        alert.getDialogPane().setContent(slider);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryService.setProgress(book.id(), (int) slider.getValue());
            refreshAll();
            statusLeftLabel.setText("Прогрес встановлено: " + (int) slider.getValue() + "%");
        }
    }

    @FXML private void handleShowStatistics() {
        long total = viewModel.getBooksList().size();
        long authors = libraryService.listAuthors().size();
        long genres = libraryService.listGenres().size();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Статистика");
        alert.setHeaderText("Статистика колекції");
        alert.setContentText("Книг: " + total + "\nАвторів: " + authors + "\nЖанрів: " + genres);
        alert.showAndWait();
    }

    @FXML private void handleShowSettings() {
        Map<String, String> settings = libraryService.getSettings();
        StringBuilder sb = new StringBuilder();
        settings.forEach((k, v) -> sb.append(k).append(" = ").append(v).append("\n"));
        TextArea textArea = new TextArea(sb.toString());
        textArea.setEditable(false);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Налаштування");
        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    @FXML private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Про програму");
        alert.setHeaderText("MyHomeLibCorp");
        alert.setContentText("Версія 1.0\nJava порт з підтримкою MVVM\nЗасновано на оригінальному MyHomeLib (Delphi)");
        alert.showAndWait();
    }

    @FXML private void handleExit() {
        Platform.exit();
    }

    // ==================== Допоміжні методи ====================
    private void addToGroup(String groupName) {
        Book book = getSelectedBook();
        if (book != null) {
            libraryService.addBookToGroup(book.id(), groupName);
            statusLeftLabel.setText("Додано до групи: " + groupName);
        }
    }

    private static String formatSize(long size) {
        if (size <= 0) return "";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    @FXML private void handleSavedSearches() {
        // TODO: реалізувати діалог зі збереженими пошуками
        // Поки що просто показуємо повідомлення
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Збережені пошуки");
        alert.setHeaderText(null);
        alert.setContentText("Функцію буде реалізовано в наступних версіях.");
        alert.showAndWait();
    }
}