package com.example.configviewer.config;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.util.Enumeration;

public final class UiScaleApplier {
    private UiScaleApplier() {
    }

    public static void applyUiScale(double uiScale) {
        if (uiScale <= 0 || Math.abs(uiScale - 1.0) < 0.0001) {
            return;
        }

        if (System.getProperty("sun.java2d.uiScale") == null || System.getProperty("sun.java2d.uiScale").isBlank()) {
            System.setProperty("sun.java2d.uiScale", Double.toString(uiScale));
        }
    }

    public static void applyGlobalFontScale(double fontScale) {
        if (fontScale <= 0 || Math.abs(fontScale - 1.0) < 0.0001) {
            return;
        }

        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        Enumeration<Object> keys = defaults.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = defaults.get(key);
            if (value instanceof FontUIResource font) {
                int scaledSize = Math.max(11, Math.round(font.getSize2D() * (float) fontScale));
                defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), scaledSize));
            }
        }
    }
}
