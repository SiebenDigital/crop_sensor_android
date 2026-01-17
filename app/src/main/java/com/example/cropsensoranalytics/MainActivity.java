package com.example.cropsensoranalytics;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorDataManager.DataListener {

    private CircularProgressIndicator humidityProgress;
    private CircularProgressIndicator tempProgress;
    private CircularProgressIndicator sunlightProgress;
    private TextView humidityText;
    private TextView tempText;
    private TextView sunlightText;
    private TextView suggestionText;
    private MaterialCardView suggestionCard;
    private ImageView suggestionIcon;
    private AutoCompleteTextView cropSelector;
    
    private LineChart historyChart; // Humidity
    private LineChart tempHistoryChart;
    private LineChart sunlightHistoryChart;

    private final Handler demoHandler = new Handler(Looper.getMainLooper());
    private Runnable demoRunnable;
    private boolean isDemoRunning = true;

    // Demo Data for Charts
    private List<Entry> humidityEntries = new ArrayList<>();
    private List<Entry> tempEntries = new ArrayList<>();
    private List<Entry> sunlightEntries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int padding = (int) android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            v.setPadding(systemBars.left + padding, systemBars.top + padding, systemBars.right + padding, systemBars.bottom + padding);
            return insets;
        });

        initializeViews();
        setupCropSelector();
        setupCharts();
        
        // Register listener
        SensorDataManager.getInstance().setListener(this);

        // Start Demo Simulation
        startDemoSimulation();
    }

    private void initializeViews() {
        humidityProgress = findViewById(R.id.humidityProgress);
        tempProgress = findViewById(R.id.tempProgress);
        sunlightProgress = findViewById(R.id.sunlightProgress);
        
        humidityText = findViewById(R.id.humidityText);
        tempText = findViewById(R.id.tempText);
        sunlightText = findViewById(R.id.sunlightText);
        
        suggestionText = findViewById(R.id.suggestionText);
        suggestionCard = findViewById(R.id.suggestionCard);
        suggestionIcon = findViewById(R.id.suggestionIcon);
        cropSelector = findViewById(R.id.cropSelector);
        
        historyChart = findViewById(R.id.historyChart);
        tempHistoryChart = findViewById(R.id.tempHistoryChart);
        sunlightHistoryChart = findViewById(R.id.sunlightHistoryChart);
    }

    private void setupCropSelector() {
        String[] crops = getResources().getStringArray(R.array.crops_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, crops);
        cropSelector.setAdapter(adapter);
        
        if (crops.length > 0) {
            cropSelector.setText(crops[0], false);
        }
    }

    private void setupCharts() {
        setupSingleChart(historyChart);
        setupSingleChart(tempHistoryChart);
        setupSingleChart(sunlightHistoryChart);
    }
    
    private void setupSingleChart(LineChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        
        chart.getAxisRight().setEnabled(false);
    }

    @Override
    public void onDataUpdated(String cropName, float temp, float humidity, float sunlight, float pressure, String suggestion) {
        runOnUiThread(() -> {
            // Update Gauges
            humidityProgress.setProgress((int) humidity);
            humidityText.setText(String.format(Locale.getDefault(), "%d%%", (int) humidity));

            int tempProgressVal = (int) ((temp / 50.0f) * 100); 
            tempProgress.setProgress(Math.min(100, Math.max(0, tempProgressVal)));
            tempText.setText(String.format(Locale.getDefault(), "%.1f°C", temp));

            int sunProgressVal = (int) ((sunlight / 1000.0f) * 100);
            sunlightProgress.setProgress(Math.min(100, Math.max(0, sunProgressVal)));
            sunlightText.setText(String.format(Locale.getDefault(), "%.0f Lx", sunlight));

            // Update Suggestion
            suggestionText.setText(suggestion);
            updateSuggestionStyle(suggestion);

            // Update Charts
            updateChartData(historyChart, humidityEntries, humidity, "Humidity %", com.google.android.material.R.color.material_dynamic_primary50);
            updateChartData(tempHistoryChart, tempEntries, temp, "Temperature °C", com.google.android.material.R.color.design_default_color_error);
            updateChartData(sunlightHistoryChart, sunlightEntries, sunlight, "Sunlight Lx", com.google.android.material.R.color.material_dynamic_tertiary50);
        });
    }

    private void updateChartData(LineChart chart, List<Entry> entries, float value, String label, int colorResId) {
        float timeIndex = entries.size(); 
        entries.add(new Entry(timeIndex, value));

        if (entries.size() > 50) {
            entries.remove(0);
            for (int i = 0; i < entries.size(); i++) {
                entries.get(i).setX(i);
            }
        }

        LineDataSet set;
        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            set = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set.setValues(entries);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            set = new LineDataSet(entries, label);
            set.setDrawIcons(false);
            
            set.setColor(Color.BLUE); // Default fallback, custom logic can improve this
            if (label.contains("Temp")) set.setColor(Color.RED);
            if (label.contains("Sun")) set.setColor(Color.GRAY);
            
            set.setLineWidth(2f);
            set.setCircleRadius(3f);
            set.setDrawCircleHole(false);
            set.setValueTextSize(9f);
            set.setDrawFilled(true);
            set.setFormLineWidth(1f);
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            
            LineData data = new LineData(set);
            chart.setData(data);
        }
        
        chart.invalidate(); 
    }

    private void updateSuggestionStyle(String suggestion) {
        int color;
        String lower = suggestion.toLowerCase();
        
        if (lower.contains("optimal")) {
            color = Color.parseColor("#2E7D32"); // Dark Green
        } else if (lower.contains("wet") || lower.contains("high") || lower.contains("low")) {
            color = Color.parseColor("#C62828"); // Red (Warning)
        } else if (lower.contains("water")) {
            color = Color.parseColor("#1565C0"); // Blue (Action)
        } else if (lower.contains("sun")) {
            color = Color.parseColor("#EF6C00"); // Orange
        } else {
            color = Color.DKGRAY;
        }

        suggestionText.setTextColor(color);
        suggestionIcon.setColorFilter(color);
        suggestionIcon.setImageResource(R.drawable.ic_launcher_foreground);
    }

    private void startDemoSimulation() {
        demoRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDemoRunning) return;

                Random random = new Random();
                float temp = 20 + random.nextFloat() * 15; 
                float humidity = 20 + random.nextFloat() * 70; // Wider range to trigger suggestions
                float sunlight = 200 + random.nextFloat() * 600; 
                float pressure = 1000 + random.nextFloat() * 20; 

                String currentCrop = cropSelector.getText().toString();
                if (currentCrop.isEmpty()) currentCrop = "Unknown";

                SensorDataManager.getInstance().updateData(currentCrop, temp, humidity, sunlight, pressure);

                demoHandler.postDelayed(this, 2000);
            }
        };
        demoHandler.post(demoRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isDemoRunning = false;
        demoHandler.removeCallbacks(demoRunnable);
    }
}
