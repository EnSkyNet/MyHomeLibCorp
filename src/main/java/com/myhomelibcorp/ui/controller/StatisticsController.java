package com.myhomelibcorp.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.Map;

/**
 * Контролер для вікна статистики.
 */
public class StatisticsController {

    @FXML private PieChart languagePieChart;
    @FXML private BarChart<String, Number> authorsBarChart;
    @FXML private Button btnClose;

    /**
     * Завантажує дані статистики у графіки.
     * @param languageData мапа мова -> кількість книг
     * @param authorData мапа автор -> кількість книг
     */
    public void setStatisticsData(Map<String, Integer> languageData, Map<String, Integer> authorData) {
        // Кругова діаграма мов
        if (languagePieChart != null && languageData != null) {
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            languageData.forEach((lang, count) -> pieChartData.add(new PieChart.Data(lang + " (" + count + ")", count)));
            languagePieChart.setData(pieChartData);
        }

        // Стовпчикова діаграма авторів
        if (authorsBarChart != null && authorData != null) {
            authorsBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            authorData.forEach((author, count) -> series.getData().add(new XYChart.Data<>(author, count)));
            authorsBarChart.getData().add(series);
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) btnClose.getScene().getWindow();
        stage.close();
    }
}