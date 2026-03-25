package com.example.configviewer.presenter;

import com.example.configviewer.model.ConfigRecord;

import java.nio.file.Path;

public interface MainViewListener {
    void onImportRequested(Path file, String configName);

    void onRefreshRequested();

    void onConfigSelected(ConfigRecord config);

    void onCompareRequested(ConfigRecord configA, ConfigRecord configB);
}
