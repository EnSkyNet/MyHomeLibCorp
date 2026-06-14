package com.myhomelibcorp.ui.controller;

import com.myhomelibcorp.domain.model.Book;
import com.myhomelibcorp.domain.model.BookEdit;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Контролер для діалогу редагування метаданих книги.
 */
public class BookEditController {

    private static final Logger logger = LoggerFactory.getLogger(BookEditController.class);

    @FXML private TextField titleField;
    @FXML private TextField seriesField;
    @FXML private TextField sequenceNumberField;
    @FXML private ComboBox<String> languageBox;
    @FXML private Spinner<Integer> rateSpinner;
    @FXML private Slider progressSlider;
    @FXML private TextArea annotationArea;
    @FXML private TextField keywordsField;
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private Book book;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
        languageBox.setItems(FXCollections.observableArrayList("uk", "en", "ru", "pl", "de"));
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, 0);
        rateSpinner.setValueFactory(valueFactory);
    }

    public void setBook(Book book) {
        this.book = book;
        if (book != null) {
            titleField.setText(book.title());
            seriesField.setText(book.series());
            sequenceNumberField.setText(book.sequenceNumber() != null ? String.valueOf(book.sequenceNumber()) : "");
            languageBox.setValue(book.language());
            rateSpinner.getValueFactory().setValue(book.rate());
            progressSlider.setValue(book.progress());
            keywordsField.setText(book.keywords());
            annotationArea.setText(book.annotation());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public BookEdit getBookEdit() {
        int seqNum = 0;
        if (sequenceNumberField.getText() != null && !sequenceNumberField.getText().isBlank()) {
            try {
                seqNum = Integer.parseInt(sequenceNumberField.getText().trim());
            } catch (NumberFormatException ignored) {}
        }
        return new BookEdit(
                titleField.getText() != null ? titleField.getText().trim() : "",
                seriesField.getText() != null ? seriesField.getText().trim() : "",
                seqNum,
                languageBox.getValue() != null ? languageBox.getValue() : "",
                keywordsField.getText() != null ? keywordsField.getText().trim() : "",
                annotationArea.getText() != null ? annotationArea.getText().trim() : "",
                rateSpinner.getValue(),
                (int) progressSlider.getValue()
        );
    }

    @FXML
    private void handleSaveAction() {
        if (isInputValid()) {
            saveClicked = true;
            closeStage();
        }
    }

    @FXML
    private void handleCancelAction() {
        saveClicked = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }

    private boolean isInputValid() {
        String errorMessage = "";
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            errorMessage += "Назва не може бути порожньою!\n";
        }
        if (sequenceNumberField.getText() != null && !sequenceNumberField.getText().isBlank()) {
            try {
                Integer.parseInt(sequenceNumberField.getText().trim());
            } catch (NumberFormatException e) {
                errorMessage += "Номер у серії має бути цілим числом!\n";
            }
        }
        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Помилка валідації");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}