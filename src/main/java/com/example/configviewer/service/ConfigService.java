package com.example.configviewer.service;

import com.example.configviewer.model.ConfigRecord;
import com.example.configviewer.model.ParamRecord;
import com.example.configviewer.parser.ConfigJsonParser;
import com.example.configviewer.repository.ConfigRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;

public class ConfigService {
    private final ConfigRepository repository;
    private final ConfigJsonParser parser;

    public ConfigService(ConfigRepository repository, ConfigJsonParser parser) {
        this.repository = repository;
        this.parser = parser;
    }

    public void initStorage() throws SQLException {
        repository.initDb();
    }

    public int importConfig(Path file, String configName) throws IOException, SQLException {
        List<ParamRecord> params = parser.parse(file);
        return repository.saveImportedConfig(configName, file.getFileName().toString(), params);
    }

    public List<ConfigRecord> getConfigs() throws SQLException {
        return repository.getConfigs();
    }

    public List<ParamRecord> getParameters(int configId) throws SQLException {
        return repository.getParametersForConfig(configId);
    }
}
