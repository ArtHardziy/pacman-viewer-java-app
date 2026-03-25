package com.example.configviewer.model;

public record AppUiSettings(double uiScale,
                            double fontScale,
                            int tableRowHeight,
                            int windowWidth,
                            int windowHeight) {
    public static AppUiSettings defaults() {
        return new AppUiSettings(1.0, 1.0, 24, 1400, 820);
    }
}
