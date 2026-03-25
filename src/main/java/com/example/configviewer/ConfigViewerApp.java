package com.example.configviewer;

import com.example.configviewer.config.UiScaleApplier;
import com.example.configviewer.config.UiSettingsLoader;
import com.example.configviewer.model.AppUiSettings;
import com.example.configviewer.parser.ConfigJsonParser;
import com.example.configviewer.presenter.MainPresenter;
import com.example.configviewer.repository.ConfigRepository;
import com.example.configviewer.service.CompareService;
import com.example.configviewer.service.ConfigService;
import com.example.configviewer.view.MainView;
import com.example.configviewer.view.SwingMainView;

import javax.swing.SwingUtilities;

public class ConfigViewerApp {
    private static final String DB_URL = "jdbc:sqlite:configurations.db";

    public static void main(String[] args) {
        AppUiSettings settings = UiSettingsLoader.load();
        UiScaleApplier.applyUiScale(settings.uiScale());
        UiScaleApplier.applyGlobalFontScale(settings.fontScale());

        SwingUtilities.invokeLater(() -> {
            ConfigRepository repository = new ConfigRepository(DB_URL);
            ConfigJsonParser parser = new ConfigJsonParser();
            ConfigService configService = new ConfigService(repository, parser);
            CompareService compareService = new CompareService();

            MainView view = new SwingMainView(settings);
            MainPresenter presenter = new MainPresenter(view, configService, compareService);

            presenter.init();
            view.showWindow();
        });
    }
}
