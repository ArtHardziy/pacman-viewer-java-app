package com.example.configviewer.config;

import com.example.configviewer.model.AppUiSettings;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class UiSettingsLoader {
    private static final String SETTINGS_FILE = "config-viewer.properties";

    private UiSettingsLoader() {
    }

    public static AppUiSettings load() {
        Properties props = new Properties();
        Path settingsPath = Path.of(System.getProperty("user.dir"), SETTINGS_FILE);

        if (Files.exists(settingsPath)) {
            try (Reader reader = Files.newBufferedReader(settingsPath)) {
                props.load(reader);
            } catch (IOException ignored) {
                // Invalid settings file should not break application startup.
            }
        }

        double uiScale = readDoubleSetting("cfgview.uiScale", "CFGVIEW_UI_SCALE", "ui.scale", props, 1.0, 3.0, 1.0);
        double fontScale = readDoubleSetting("cfgview.fontScale", "CFGVIEW_FONT_SCALE", "font.scale", props, 1.0, 3.0, 1.0);
        int rowHeight = readIntSetting("cfgview.tableRowHeight", "CFGVIEW_TABLE_ROW_HEIGHT", "table.rowHeight", props, 18, 80, 24);
        int width = readIntSetting("cfgview.windowWidth", "CFGVIEW_WINDOW_WIDTH", "window.width", props, 1000, 5000, 1400);
        int height = readIntSetting("cfgview.windowHeight", "CFGVIEW_WINDOW_HEIGHT", "window.height", props, 700, 4000, 820);

        return new AppUiSettings(uiScale, fontScale, rowHeight, width, height);
    }

    private static double readDoubleSetting(String sysPropKey,
                                            String envKey,
                                            String fileKey,
                                            Properties props,
                                            double min,
                                            double max,
                                            double defaultValue) {
        String raw = firstNonBlank(System.getProperty(sysPropKey), System.getenv(envKey), props.getProperty(fileKey));
        if (raw == null) {
            return defaultValue;
        }

        try {
            double parsed = Double.parseDouble(raw.trim().replace(',', '.'));
            if (parsed < min || parsed > max) {
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static int readIntSetting(String sysPropKey,
                                      String envKey,
                                      String fileKey,
                                      Properties props,
                                      int min,
                                      int max,
                                      int defaultValue) {
        String raw = firstNonBlank(System.getProperty(sysPropKey), System.getenv(envKey), props.getProperty(fileKey));
        if (raw == null) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(raw.trim());
            if (parsed < min || parsed > max) {
                return defaultValue;
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
