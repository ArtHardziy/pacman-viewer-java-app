package com.example.configviewer.presenter;

import com.example.configviewer.model.ConfigRecord;
import com.example.configviewer.model.ParamDiffRecord;
import com.example.configviewer.model.ParamRecord;
import com.example.configviewer.service.CompareService;
import com.example.configviewer.service.ConfigService;
import com.example.configviewer.view.MainView;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class MainPresenter implements MainViewListener {
    private final MainView view;
    private final ConfigService configService;
    private final CompareService compareService;

    public MainPresenter(MainView view, ConfigService configService, CompareService compareService) {
        this.view = view;
        this.configService = configService;
        this.compareService = compareService;
    }

    public void init() {
        view.setListener(this);

        try {
            configService.initStorage();
            refreshConfigs();
        } catch (Exception ex) {
            view.showError("Ошибка инициализации приложения: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onImportRequested(Path file, String configName) {
        try {
            int configId = configService.importConfig(file, configName);
            refreshConfigs();
            view.selectConfigById(configId);
            view.showInfo("Импорт завершен: " + configName);
        } catch (Exception ex) {
            view.showError("Ошибка импорта: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onRefreshRequested() {
        try {
            refreshConfigs();
        } catch (Exception ex) {
            view.showError("Ошибка обновления списка: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onConfigSelected(ConfigRecord config) {
        if (config == null) {
            return;
        }

        try {
            List<ParamRecord> params = configService.getParameters(config.id());
            view.showParameters(params);
        } catch (SQLException ex) {
            view.showError("Ошибка загрузки параметров: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void onCompareRequested(ConfigRecord configA, ConfigRecord configB) {
        if (configA == null || configB == null) {
            view.showWarning("Выберите две конфигурации");
            return;
        }

        if (configA.id() == configB.id()) {
            view.showWarning("Для сравнения нужны разные конфигурации");
            return;
        }

        try {
            List<ParamRecord> paramsA = configService.getParameters(configA.id());
            List<ParamRecord> paramsB = configService.getParameters(configB.id());
            List<ParamDiffRecord> diffs = compareService.compare(paramsA, paramsB);

            view.showDiffs(diffs);
            if (diffs.isEmpty()) {
                view.showInfo("Отличий не найдено");
            }
        } catch (SQLException ex) {
            view.showError("Ошибка сравнения: " + ex.getMessage(), ex);
        }
    }

    private void refreshConfigs() throws SQLException {
        List<ConfigRecord> configs = configService.getConfigs();
        view.showConfigs(configs);
    }
}
