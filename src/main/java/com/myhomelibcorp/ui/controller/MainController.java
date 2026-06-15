package com.myhomelibcorp.ui.controller;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.BookEdit;
import com.myhomelibcorp.domain.model.SearchCriteria;
import com.myhomelibcorp.domain.service.LibraryService;
import com.myhomelibcorp.domain.service.OnlineLibraryService;
import com.myhomelibcorp.infrastructure.database.DatabaseManager;
import com.myhomelibcorp.infrastructure.settings.SettingsStore;
import com.myhomelibcorp.ui.viewmodel.LibraryViewModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.stage.Modality;
import com.myhomelibcorp.ui.controller.SettingsController;

/**
 * Головний контролер програми.
 */
public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    // ==================== JavaFX компоненти (з MainView.fxml) ====================
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleCol, authorCol, seriesCol, langCol, fileNameCol, genreCol, sizeCol;
    @FXML private TableColumn<Book, Integer> seqNumberCol, rateCol, progressCol;
    @FXML private Label detailTitleLabel, detailAuthorLabel, detailSeriesLabel, detailGenresLabel;
    @FXML private Label detailLanguageLabel, detailFileLabel, detailSizeLabel, detailRateLabel, detailProgressLabel;
    @FXML private TextArea detailKeywordsArea, detailAnnotationArea, detailReviewArea;
    @FXML private ListView<String> authorsList, seriesList, genresList, groupsList, downloadsList;
    @FXML private TextField searchField;
    @FXML private Label statusLeftLabel, statusRightLabel;

    // ==================== Залежності ====================
    private LibraryService libraryService;
    private LibraryViewModel viewModel;
    private DatabaseManager dbManager;
    private SettingsStore settingsStore;
    private OnlineLibraryService onlineLibraryService;
    private String selectedGroup = null;
    private Path currentDbPath = Path.of("library.db");

    public void setDependencies(DatabaseManager dbManager, LibraryService service, LibraryViewModel vm, SettingsStore store) {
        this.dbManager = dbManager;
        this.libraryService = service;
        this.viewModel = vm;
        this.settingsStore = store;
        this.onlineLibraryService = new OnlineLibraryService();
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

        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, book) -> showBookDetails(book));
        searchField.textProperty().addListener((obs, old, query) -> refreshBooks());

        authorsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        seriesList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        genresList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> { if (val != null) searchField.setText(val); });
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            selectedGroup = val;
            refreshBooks();
            if (val != null && !val.isBlank()) {
                statusLeftLabel.setText("Фільтр: група '" + val + "'");
            } else {
                statusLeftLabel.setText("Готово");
            }
        });

        setupDragAndDrop();
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
        String query = searchField.getText();
        List<Book> books;
        if (selectedGroup != null && !selectedGroup.isBlank()) {
            books = libraryService.getBooksInGroup(selectedGroup);
            if (query != null && !query.isBlank()) {
                String lowerQuery = query.toLowerCase();
                books = books.stream()
                        .filter(b -> b.title().toLowerCase().contains(lowerQuery) ||
                                b.authorsText().toLowerCase().contains(lowerQuery) ||
                                (b.series() != null && b.series().toLowerCase().contains(lowerQuery)))
                        .collect(Collectors.toList());
            }
        } else {
            books = libraryService.searchBooks(query);
        }
        viewModel.getBooksList().setAll(books);
        updateStatus();
    }

    private void refreshLeftLists() {
        authorsList.setItems(FXCollections.observableArrayList(libraryService.listAuthors()));
        seriesList.setItems(FXCollections.observableArrayList(libraryService.listSeries()));
        genresList.setItems(FXCollections.observableArrayList(libraryService.listGenres()));
        groupsList.setItems(FXCollections.observableArrayList(libraryService.listGroups()));
        downloadsList.setItems(FXCollections.observableArrayList());
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

    private static String formatSize(long size) {
        if (size <= 0) return "";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }

    private void setupDragAndDrop() {
        bookTable.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(String.valueOf(row.getItem().id()));
                    db.setContent(content);
                    event.consume();
                }
            });
            return row;
        });

        groupsList.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        groupsList.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                long bookId = Long.parseLong(db.getString());
                String targetGroup = groupsList.getSelectionModel().getSelectedItem();
                if (targetGroup != null) {
                    libraryService.addBookToGroup(bookId, targetGroup);
                    statusLeftLabel.setText("Книгу додано до групи: " + targetGroup);
                    refreshLeftLists();
                    refreshBooks();
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });
    }

    // ==================== Обробники меню ====================
    @FXML private void handleNewCollection() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Створити нову колекцію");
        chooser.setInitialFileName("collection.db");
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                DatabaseManager newDb = new DatabaseManager(file.toPath());
                newDb.getConnection().close();
                currentDbPath = file.toPath();
                settingsStore.setString("lastOpenedCollection", currentDbPath.toString());
                refreshAll();
                statusLeftLabel.setText("Створено нову колекцію: " + file.getName());
            } catch (Exception e) {
                statusLeftLabel.setText("Помилка створення колекції: " + e.getMessage());
                logger.error("Помилка створення колекції", e);
            }
        }
    }

    @FXML private void handleOpenCollection() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Відкрити колекцію");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Бази даних SQLite", "*.db"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            currentDbPath = file.toPath();
            settingsStore.setString("lastOpenedCollection", currentDbPath.toString());
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
            task.setOnSucceeded(e -> {
                refreshAll();
                statusLeftLabel.setText("Імпорт завершено з " + folder.getName());
            });
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
            task.setOnSucceeded(e -> {
                refreshAll();
                statusLeftLabel.setText("Імпорт INPX завершено");
            });
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
                logger.error("Помилка імпорту жанрів", ex);
            }
        }
    }

    @FXML private void handleImportExportUserData() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Імпорт/експорт даних користувача");
        alert.setContentText("Функцію буде реалізовано в наступних версіях.");
        alert.showAndWait();
    }

    @FXML private void handleExportBook() {
        Book book = getSelectedBook();
        if (book == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть книгу для експорту.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(book.fileName());
        File dest = chooser.showSaveDialog(null);
        if (dest != null) {
            try {
                libraryService.exportBook(book, dest.toPath());
                statusLeftLabel.setText("Експортовано до " + dest.getName());
            } catch (Exception e) {
                statusLeftLabel.setText("Помилка експорту: " + e.getMessage());
                logger.error("Помилка експорту", e);
            }
        }
    }

    @FXML private void handleExportToDevice() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Експорт на пристрій");
        alert.setContentText("Функцію буде реалізовано пізніше.");
        alert.showAndWait();
    }

    @FXML private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myhomelibcorp/ui/view/SettingsView.fxml"));
            Parent root = loader.load();
            SettingsController controller = loader.getController();
            controller.setSettingsStore(settingsStore);
            Stage stage = new Stage();
            stage.setTitle("Налаштування");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(bookTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            refreshAll();
        } catch (Exception e) {
            logger.error("Помилка відкриття налаштувань", e);
            statusLeftLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleExit() {
        Platform.exit();
    }

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
            statusLeftLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleOpenBookExternal() {
        Book book = getSelectedBook();
        if (book == null) return;
        try {
            Path filePath = book.hasArchiveEntry() ? Path.of(book.folder()) : Path.of(book.folder(), book.fileName());
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
            statusLeftLabel.setText("Помилка: " + e.getMessage());
            logger.error("Помилка відкриття зовнішньою програмою", e);
        }
    }

    @FXML private void handleDownloadBook() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Завантаження книги");
        alert.setContentText("Функцію буде реалізовано в розділі 'Онлайн'.");
        alert.showAndWait();
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
                        LocalDateTime.now()
                );
                libraryService.updateBook(updated);
                refreshAll();
                statusLeftLabel.setText("Книгу оновлено");
            }
        } catch (Exception e) {
            logger.error("Помилка редагування книги", e);
            statusLeftLabel.setText("Помилка: " + e.getMessage());
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

    @FXML private void handleEditCover() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Змінити обкладинку");
        alert.setContentText("Функцію буде реалізовано в наступних версіях.");
        alert.showAndWait();
    }

    @FXML private void handleAddToFavorites() {
        addToGroup("Favorites");
    }

    @FXML private void handleAddToGroup() {
        Book book = getSelectedBook();
        if (book == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть книгу.").showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Додати до групи");
        dialog.setHeaderText("Введіть назву групи:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            libraryService.addBookToGroup(book.id(), name);
            refreshLeftLists();
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
        dialog.setHeaderText("Виберіть групу:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(group -> {
            libraryService.removeBookFromGroup(book.id(), group);
            refreshLeftLists();
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

    @FXML private void handleDeleteBook() {
        Book book = getSelectedBook();
        if (book == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть книгу для видалення.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Підтвердження видалення");
        confirm.setHeaderText("Видалення книги \"" + book.title() + "\"");
        confirm.setContentText("Книга буде видалена з бази даних. Файл залишиться на диску. Продовжити?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                libraryService.deleteBook(book.id());
                refreshAll();
                statusLeftLabel.setText("Книгу видалено");
            } catch (Exception e) {
                statusLeftLabel.setText("Помилка видалення: " + e.getMessage());
                logger.error("Помилка видалення книги", e);
            }
        }
    }

    @FXML private void handleToggleNavigationTree() {
        statusLeftLabel.setText("Функцію буде реалізовано пізніше.");
    }

    @FXML private void handleRefresh() {
        refreshAll();
        statusLeftLabel.setText("Дані оновлено");
    }

    @FXML private void handleViewMode() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Режим перегляду");
        alert.setContentText("Функцію буде реалізовано пізніше.");
        alert.showAndWait();
    }

    @FXML private void handleSorting() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Сортування");
        alert.setContentText("Функцію буде реалізовано пізніше.");
        alert.showAndWait();
    }

    @FXML private void handleFilters() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Фільтри");
        alert.setContentText("Функцію буде реалізовано пізніше.");
        alert.showAndWait();
    }

    @FXML private void handleAdvancedSearch() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/myhomelibcorp/ui/view/AdvancedSearchView.fxml"));
            Parent root = loader.load();
            AdvancedSearchController controller = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Розширений пошук");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(bookTable.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();
            if (controller.isSearchTriggered()) {
                AdvancedSearchController.SearchParams params = controller.getSearchParams();
                SearchCriteria criteria = new SearchCriteria(
                        params.title(), params.author(), params.genre(), params.series(), params.lang(),
                        params.minRate(), null, null, null, null, null,
                        "", "", "", "", ""
                );
                List<Book> results = libraryService.searchBooks(params.title());
                viewModel.getBooksList().setAll(results);
                updateStatus();
                statusLeftLabel.setText("Знайдено книг: " + results.size());
            }
        } catch (Exception e) {
            logger.error("Помилка відкриття розширеного пошуку", e);
            statusLeftLabel.setText("Помилка: " + e.getMessage());
        }
    }

    @FXML private void handleSavedSearches() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Збережені пошуки");
        alert.setContentText("Функцію буде реалізовано пізніше.");
        alert.showAndWait();
    }

    @FXML private void handleNewGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Нова група");
        dialog.setHeaderText("Введіть назву нової групи:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.isBlank()) {
                libraryService.createGroup(name);
                refreshLeftLists();
                statusLeftLabel.setText("Створено групу: " + name);
            }
        });
    }

    @FXML private void handleRenameGroup() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть групу для перейменування.").showAndWait();
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selected);
        dialog.setTitle("Перейменувати групу");
        dialog.setHeaderText("Введіть нову назву групи:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            if (!newName.isBlank() && !newName.equals(selected)) {
                libraryService.renameGroup(selected, newName);
                refreshLeftLists();
                statusLeftLabel.setText("Групу перейменовано на: " + newName);
            }
        });
    }

    @FXML private void handleDeleteGroup() {
        String selected = groupsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.WARNING, "Виберіть групу для видалення.").showAndWait();
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Видалення групи");
        confirm.setHeaderText("Видалення групи \"" + selected + "\"");
        confirm.setContentText("Група буде видалена, але книги залишаться в бібліотеці. Продовжити?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            libraryService.deleteGroup(selected);
            refreshLeftLists();
            statusLeftLabel.setText("Групу видалено: " + selected);
        }
    }

    @FXML private void handleHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Довідка");
        alert.setHeaderText("MyHomeLibCorp - Довідка");
        alert.setContentText("""
                Основні можливості програми:
                • Відкриття та створення колекцій книг
                • Імпорт книг з FB2, ZIP, INPX
                • Пошук за назвою, автором, серією
                • Групування книг (Favorites, To read, власні групи)
                • Оцінка книг, прогрес читання, рецензії
                • Експорт книг
                • Онлайн-бібліотеки (Флібуста)
                """);
        alert.showAndWait();
    }

    @FXML private void handleAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Про програму");
        alert.setHeaderText("MyHomeLibCorp");
        alert.setContentText("Версія 1.0\nJava порт з підтримкою MVVM\nЗасновано на оригінальному MyHomeLib (Delphi)\n\n© 2025 MyHomeLibCorp");
        alert.showAndWait();
    }

    private void addToGroup(String groupName) {
        Book book = getSelectedBook();
        if (book != null) {
            libraryService.addBookToGroup(book.id(), groupName);
            statusLeftLabel.setText("Додано до групи: " + groupName);
            refreshLeftLists();
        }
    }

    // ==================== Додаткові методи для меню ====================

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
}