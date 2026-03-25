package com.example.configviewer.view;

import com.example.configviewer.model.ConfigRecord;
import com.example.configviewer.model.ParamDiffRecord;
import com.example.configviewer.model.ParamRecord;
import com.example.configviewer.presenter.MainViewListener;

import java.util.List;

public interface MainView {
    void setListener(MainViewListener listener);

    void showWindow();

    void showConfigs(List<ConfigRecord> configs);

    void selectConfigById(int configId);

    void showParameters(List<ParamRecord> params);

    void showDiffs(List<ParamDiffRecord> diffs);

    void showInfo(String message);

    void showWarning(String message);

    void showError(String message, Throwable error);
}
