package com.energy.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class EnergyController {

    // UI Elements
    @FXML private Label communityPoolLabel;
    @FXML private Label gridPortionLabel;
    @FXML private Button refreshButton;
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourComboBox;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourComboBox;
    @FXML private Button showDataButton;
    @FXML private Label communityProducedLabel;
    @FXML private Label communityUsedLabel;
    @FXML private Label gridUsedLabel;
    @FXML private VBox historicalDataBox;

    // Constants
    private static final String BASE_API_URL = "http://localhost:8080";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Initialize hour dropdowns (00:00 to 23:00)
        initializeHourComboBoxes();

        // Set default values matching the image
        setDefaultDateTimeValues();

        // Set button actions
        setupButtonActions();

        // Initially fetch current data
//        fetchCurrentData();
    }

    private void initializeHourComboBoxes() {
        for (int i = 0; i < 24; i++) {
            String hour = String.format("%02d:00", i);
            startHourComboBox.getItems().add(hour);
            endHourComboBox.getItems().add(hour);
        }
    }

    private void setDefaultDateTimeValues() {
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now());

        // Set default hours to 14:00
        startHourComboBox.setValue("14:00");
        endHourComboBox.setValue("15:00");
    }

    private void setupButtonActions() {
        refreshButton.setOnAction(event -> fetchCurrentData());
        showDataButton.setOnAction(event -> fetchHistoricalData());
    }

    private void fetchCurrentData() {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_API_URL + "/energy/current");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    String response = readResponse(conn);
                    System.out.println("Response: " + response);
                    CurrentPercentage currentPercentage = new Gson().fromJson(response, CurrentPercentage.class);

                    updateUI(() -> {
                        communityPoolLabel.setText(String.format("%.2f%% used", currentPercentage.communityDepleted));
                        gridPortionLabel.setText(String.format("%.2f%%", currentPercentage.gridPortion));
                    });
                }
                conn.disconnect();
            } catch (IOException e) {
                handleError("Error fetching current data", e);
            }
        }).start();
    }

    private void fetchHistoricalData() {
        String startDateTime = buildDateTimeString(startDatePicker.getValue(), startHourComboBox.getValue());
        String endDateTime = buildDateTimeString(endDatePicker.getValue(), endHourComboBox.getValue());

        new Thread(() -> {
            try {
                String urlString = String.format("%s/energy/historical?start=%s&end=%s",
                        BASE_API_URL, startDateTime, endDateTime);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
                conn.setRequestMethod("GET");

                System.out.println("Request URL: " + urlString);

                if (conn.getResponseCode() == 200) {
                    String response = readResponse(conn);
                    HistoricalData historicalData = new Gson().fromJson(response, HistoricalData.class);

                    updateUI(() -> {
                        communityProducedLabel.setText(String.format("%.3f kWh", historicalData.communityProduced));
                        communityUsedLabel.setText(String.format("%.3f kWh", historicalData.communityUsed));
                        gridUsedLabel.setText(String.format("%.3f kWh", historicalData.gridUsed));
                        historicalDataBox.setVisible(true);
                    });
                }
                conn.disconnect();
            } catch (IOException e) {
                handleError("Error fetching historical data", e);
            }
        }).start();
    }

    private String buildDateTimeString(LocalDate date, String hour) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-dd-MM")) + "T" + hour + ":00";
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }

    private void updateUI(Runnable update) {
        javafx.application.Platform.runLater(update);
    }

    private void handleError(String message, Exception e) {
        e.printStackTrace();
        updateUI(() -> {
            communityPoolLabel.setText(message);
            gridPortionLabel.setText(message);
            communityProducedLabel.setText(message);
            communityUsedLabel.setText(message);
            gridUsedLabel.setText(message);
        });
    }
}
