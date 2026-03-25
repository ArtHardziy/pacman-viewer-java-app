package com.example.configviewer.repository;

import com.example.configviewer.model.ConfigRecord;
import com.example.configviewer.model.ParamRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigRepository {
    private final String dbUrl;

    public ConfigRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public void initDb() throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS configs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        file_name TEXT,
                        imported_at TEXT NOT NULL
                    )
                    """);

            st.execute("""
                    CREATE TABLE IF NOT EXISTS parameters (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        config_id INTEGER NOT NULL,
                        name TEXT,
                        description TEXT,
                        data_type TEXT,
                        value TEXT,
                        path TEXT,
                        tenant TEXT,
                        scope TEXT,
                        location TEXT,
                        composite_key TEXT,
                        FOREIGN KEY(config_id) REFERENCES configs(id)
                    )
                    """);

            st.execute("CREATE INDEX IF NOT EXISTS idx_parameters_config ON parameters(config_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_parameters_key ON parameters(composite_key)");
        }
    }

    public int saveImportedConfig(String configName, String fileName, List<ParamRecord> params) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            try {
                int configId = insertConfig(conn, configName, fileName);
                insertParameters(conn, configId, params);
                conn.commit();
                return configId;
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<ConfigRecord> getConfigs() throws SQLException {
        List<ConfigRecord> configs = new ArrayList<>();
        String sql = "SELECT id, name, file_name, imported_at FROM configs ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                configs.add(new ConfigRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("file_name"),
                        rs.getString("imported_at")
                ));
            }
        }

        return configs;
    }

    public List<ParamRecord> getParametersForConfig(int configId) throws SQLException {
        List<ParamRecord> parameters = new ArrayList<>();
        String sql = "SELECT composite_key, name, description, data_type, value, path, tenant, scope, location " +
                "FROM parameters WHERE config_id = ? ORDER BY name, path, id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    parameters.add(new ParamRecord(
                            rs.getString("composite_key"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("data_type"),
                            rs.getString("value"),
                            rs.getString("path"),
                            rs.getString("tenant"),
                            rs.getString("scope"),
                            rs.getString("location")
                    ));
                }
            }
        }

        return parameters;
    }

    private int insertConfig(Connection conn, String configName, String fileName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO configs(name, file_name, imported_at) VALUES(?, ?, datetime('now'))",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, configName);
            ps.setString(2, fileName);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Не удалось создать запись конфигурации");
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertParameters(Connection conn, int configId, List<ParamRecord> params) throws SQLException {
        String sql = "INSERT INTO parameters(config_id, name, description, data_type, value, path, tenant, scope, location, composite_key) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (ParamRecord param : params) {
                ps.setInt(1, configId);
                ps.setString(2, param.name());
                ps.setString(3, param.description());
                ps.setString(4, param.dataType());
                ps.setString(5, param.value());
                ps.setString(6, param.path());
                ps.setString(7, param.tenant());
                ps.setString(8, param.scope());
                ps.setString(9, param.location());
                ps.setString(10, param.compositeKey());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
