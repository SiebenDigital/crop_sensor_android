package com.example.cropsensoranalytics;

import java.util.ArrayList;
import java.util.List;

public class SensorDataManager {
    private static SensorDataManager instance;
    private DataListener listener;

    private String cropName = "Unknown";
    private float temperature; // Celsius
    private float humidity;    // Percentage
    private float sunlight;    // Lux
    private float pressure;    // hPa

    public interface DataListener {
        void onDataUpdated(String cropName, float temp, float humidity, float sunlight, float pressure, String suggestion);
    }

    private SensorDataManager() {
        // Private constructor for Singleton
    }

    public static synchronized SensorDataManager getInstance() {
        if (instance == null) {
            instance = new SensorDataManager();
        }
        return instance;
    }

    public void setListener(DataListener listener) {
        this.listener = listener;
    }

    public void updateData(String cropName, float temp, float humidity, float sunlight, float pressure) {
        this.cropName = cropName;
        this.temperature = temp;
        this.humidity = humidity;
        this.sunlight = sunlight;
        this.pressure = pressure;

        String suggestion = generateSuggestion(temp, humidity, sunlight);

        if (listener != null) {
            listener.onDataUpdated(cropName, temp, humidity, sunlight, pressure, suggestion);
        }
    }

    private String generateSuggestion(float temp, float humidity, float sunlight) {
        if (humidity < 30) {
            return "Give more water to plant";
        } else if (humidity > 80) {
            return "Soil is too wet, reduce watering";
        } else if (temp > 35) {
            return "Temperature is too high, provide shade";
        } else if (temp < 10) {
            return "Temperature is too low";
        } else if (sunlight < 100) {
            return "Increase sun exposure";
        }
        return "Conditions are optimal";
    }

    // Getters if needed
    public String getCropName() { return cropName; }
    public float getTemperature() { return temperature; }
    public float getHumidity() { return humidity; }
    public float getSunlight() { return sunlight; }
    public float getPressure() { return pressure; }
}
