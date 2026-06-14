package com.myhomelibcorp.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Контролер для вибору збережених пресетів пошуку.
 */
public class SearchPresetsController {

    private static final Logger logger = LoggerFactory.getLogger(SearchPresetsController.class);

    @FXML private ListView<String> presetsListView;
    @FXML private Button btnApply;
    @FXML private Button btnDelete;
    @FXML private Button btnCancel;

    private String selectedPreset = null;
    private boolean applyClicked = false;
    private ObservableList<String> presetsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        presetsListView.setItems(presetsList);

        presetsListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean hasSelection = (newValue != null);
            btnApply.setDisable(!hasSelection);
            btnDelete.setDisable(!hasSelection);
            if (hasSelection) {
                selectedPreset = newValue;
            }
        });
    }

    public void loadPresets(List<String> userPresets) {
        if (userPresets != null) {
            presetsList.setAll(userPresets);
        }
    }

    public String getSelectedPreset() {
        return selectedPreset;
    }

    public boolean isApplyClicked() {
        return applyClicked;
    }

    @FXML
    private void handleApplyPreset() {
        if (selectedPreset != null) {
            applyClicked = true;
            closeStage();
        }
    }

    @FXML
    private void handleDeletePreset() {
        if (selectedPreset != null) {
            presetsList.remove(selectedPreset);
            logger.info("Видалено пресет пошуку: {}", selectedPreset);
            // Тут можна додати виклик до сервісу для збереження змін на диску
        }
    }

    @FXML
    private void handleCancel() {
        applyClicked = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnCancel.getScene().getWindow();
        stage.close();
    }
}