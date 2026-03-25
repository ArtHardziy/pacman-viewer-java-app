package com.example.configviewer;

import com.google.gson.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigViewerApp {
    private static final String DB_URL = "jdbc:sqlite:configurations.db";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JFrame frame;
    private DefaultListModel<ConfigRecord> configListModel;
    private JList<ConfigRecord> configList;

    private DefaultTableModel viewTableModel;
    private JTable viewTable;
    private TableRowSorter<DefaultTableModel> viewSorter;
    private JTextField searchField;

    private JComboBox<ConfigRecord> compareA;
    private JComboBox<ConfigRecord> compareB;
    private DefaultTableModel compareTableModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                ConfigViewerApp app = new ConfigViewerApp();
                app.initDb();
                app.createAndShow();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Ошибка запуска: " + e.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    private void createAndShow() {
        frame = new JFrame("Config JSON Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 820);
        frame.setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton importButton = new JButton("Импорт JSON");
        JButton refreshButton = new JButton("Обновить");

        importButton.addActionListener(e -> importJson());
        refreshButton.addActionListener(e -> refreshConfigs());

        topPanel.add(importButton);
        topPanel.add(refreshButton);

        configListModel = new DefaultListModel<>();
        configList = new JList<>(configListModel);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ConfigRecord selected = configList.getSelectedValue();
                if (selected != null) {
                    loadParamsForConfig(selected.id());
                }
            }
        });

        JScrollPane configScroll = new JScrollPane(configList);
        configScroll.setBorder(BorderFactory.createTitledBorder("База конфигураций"));

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Просмотр и поиск", createViewTab());
        tabs.addTab("Сравнение", createCompareTab());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, configScroll, tabs);
        split.setDividerLocation(350);

        frame.setLayout(new BorderLayout());
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);

        refreshConfigs();
        frame.setVisible(true);
    }

    private JPanel createViewTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.add(new JLabel("Поиск:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchPanel.add(searchField, BorderLayout.CENTER);

        String[] cols = {
                "Имя параметра",
                "Пояснение",
                "Тип",
                "Значение",
                "Путь",
                "Tenant",
                "Scope",
                "Location"
        };

        viewTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        viewTable = new JTable(viewTableModel);
        viewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        viewTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        viewTable.getColumnModel().getColumn(1).setPreferredWidth(320);
        viewTable.getColumnModel().getColumn(2).setPreferredWidth(110);
        viewTable.getColumnModel().getColumn(3).setPreferredWidth(330);
        viewTable.getColumnModel().getColumn(4).setPreferredWidth(260);
        viewTable.getColumnModel().getColumn(5).setPreferredWidth(160);
        viewTable.getColumnModel().getColumn(6).setPreferredWidth(280);
        viewTable.getColumnModel().getColumn(7).setPreferredWidth(130);

        viewSorter = new TableRowSorter<>(viewTableModel);
        viewTable.setRowSorter(viewSorter);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearch();
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(viewTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCompareTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        compareA = new JComboBox<>();
        compareB = new JComboBox<>();
        JButton compareButton = new JButton("Сравнить");

        compareButton.addActionListener(e -> compareConfigs());

        top.add(new JLabel("Конфигурация A:"));
        top.add(compareA);
        top.add(new JLabel("Конфигурация B:"));
        top.add(compareB);
        top.add(compareButton);

        String[] cols = {
                "Статус",
                "Ключ",
                "Имя параметра",
                "Путь",
                "Тип A",
                "Значение A",
                "Тип B",
                "Значение B",
                "Описание"
        };

        compareTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(compareTableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(1).setPreferredWidth(390);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(230);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);
        table.getColumnModel().getColumn(5).setPreferredWidth(260);
        table.getColumnModel().getColumn(6).setPreferredWidth(90);
        table.getColumnModel().getColumn(7).setPreferredWidth(260);
        table.getColumnModel().getColumn(8).setPreferredWidth(300);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void applySearch() {
        String text = searchField.getText();
        if (text == null || text.isBlank()) {
            viewSorter.setRowFilter(null);
            return;
        }

        String escaped = Pattern.quote(text.trim());
        viewSorter.setRowFilter(RowFilter.regexFilter("(?i)" + escaped));
    }

    private void importJson() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Выберите JSON-файл параметров");

        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        String defaultName = stripExtension(file.getFileName().toString()) + "_" + TIME_FMT.format(LocalDateTime.now());

        String configName = JOptionPane.showInputDialog(frame,
                "Введите имя конфигурации для базы:",
                defaultName);

        if (configName == null || configName.isBlank()) {
            return;
        }

        try {
            int configId = saveConfig(file, configName.trim());
            refreshConfigs();
            selectConfigById(configId);
            JOptionPane.showMessageDialog(frame,
                    "Импорт завершен: " + configName,
                    "Готово",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка импорта: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private int saveConfig(Path file, String configName) throws IOException, SQLException {
        String json = Files.readString(file);
        JsonArray root = JsonParser.parseString(json).getAsJsonArray();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                int configId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO configs(name, file_name, imported_at) VALUES(?, ?, datetime('now'))",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, configName);
                    ps.setString(2, file.getFileName().toString());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) {
                            throw new SQLException("Не удалось создать запись конфигурации");
                        }
                        configId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO parameters(config_id, name, description, data_type, value, path, tenant, scope, location, composite_key) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                    for (JsonElement element : root) {
                        if (!element.isJsonObject()) {
                            continue;
                        }
                        JsonObject item = element.getAsJsonObject();
                        JsonObject key = getObj(item, "key");
                        JsonObject parameter = getObj(item, "parameter");

                        String tenant = getString(key, "tenant");
                        String scope = getString(key, "scope");
                        String location = getString(key, "location");
                        String name = getString(parameter, "name");
                        String desc = getString(parameter, "description");
                        String dataType = getString(parameter, "dataType");

                        JsonArray bundles = getArr(parameter, "bundles");
                        if (bundles == null || bundles.isEmpty()) {
                            insertParam(ps, configId, name, desc, dataType, "", "", tenant, scope, location);
                            continue;
                        }

                        for (JsonElement bundleEl : bundles) {
                            if (!bundleEl.isJsonObject()) {
                                continue;
                            }
                            JsonObject bundle = bundleEl.getAsJsonObject();
                            String pathStr = pathToString(getArr(bundle, "path"));
                            JsonArray values = getArr(bundle, "values");

                            if (values == null || values.isEmpty()) {
                                insertParam(ps, configId, name, desc, dataType, "", pathStr, tenant, scope, location);
                                continue;
                            }

                            for (JsonElement v : values) {
                                String value = jsonPrimitiveToString(v);
                                insertParam(ps, configId, name, desc, dataType, value, pathStr, tenant, scope, location);
                            }
                        }
                    }
                }

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

    private void insertParam(PreparedStatement ps,
                             int configId,
                             String name,
                             String desc,
                             String dataType,
                             String value,
                             String path,
                             String tenant,
                             String scope,
                             String location) throws SQLException {
        String compositeKey = buildCompositeKey(tenant, scope, location, path, name);

        ps.setInt(1, configId);
        ps.setString(2, name);
        ps.setString(3, desc);
        ps.setString(4, dataType);
        ps.setString(5, value);
        ps.setString(6, path);
        ps.setString(7, tenant);
        ps.setString(8, scope);
        ps.setString(9, location);
        ps.setString(10, compositeKey);
        ps.addBatch();
        ps.executeBatch();
    }

    private void refreshConfigs() {
        configListModel.clear();
        compareA.removeAllItems();
        compareB.removeAllItems();

        List<ConfigRecord> configs = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, name, file_name, imported_at FROM configs ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ConfigRecord cfg = new ConfigRecord(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("file_name"),
                        rs.getString("imported_at")
                );
                configs.add(cfg);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка чтения конфигураций: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (ConfigRecord cfg : configs) {
            configListModel.addElement(cfg);
            compareA.addItem(cfg);
            compareB.addItem(cfg);
        }

        if (!configs.isEmpty() && configList.getSelectedValue() == null) {
            configList.setSelectedIndex(0);
        }
    }

    private void selectConfigById(int id) {
        for (int i = 0; i < configListModel.size(); i++) {
            ConfigRecord r = configListModel.getElementAt(i);
            if (r.id() == id) {
                configList.setSelectedIndex(i);
                compareA.setSelectedItem(r);
                break;
            }
        }

        if (compareB.getItemCount() > 1) {
            compareB.setSelectedIndex(0);
        }
    }

    private void loadParamsForConfig(int configId) {
        viewTableModel.setRowCount(0);

        String sql = "SELECT name, description, data_type, value, path, tenant, scope, location " +
                "FROM parameters WHERE config_id = ? ORDER BY name, path";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    viewTableModel.addRow(new Object[]{
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getString("data_type"),
                            rs.getString("value"),
                            rs.getString("path"),
                            rs.getString("tenant"),
                            rs.getString("scope"),
                            rs.getString("location")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка загрузки параметров: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void compareConfigs() {
        ConfigRecord a = (ConfigRecord) compareA.getSelectedItem();
        ConfigRecord b = (ConfigRecord) compareB.getSelectedItem();

        if (a == null || b == null) {
            JOptionPane.showMessageDialog(frame, "Выберите две конфигурации", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (a.id() == b.id()) {
            JOptionPane.showMessageDialog(frame, "Для сравнения нужны разные конфигурации", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Map<String, ParamRecord> mapA = readParamMap(a.id());
        Map<String, ParamRecord> mapB = readParamMap(b.id());

        compareTableModel.setRowCount(0);

        Set<String> keys = new TreeSet<>();
        keys.addAll(mapA.keySet());
        keys.addAll(mapB.keySet());

        for (String key : keys) {
            ParamRecord pa = mapA.get(key);
            ParamRecord pb = mapB.get(key);

            if (pa == null && pb != null) {
                compareTableModel.addRow(new Object[]{
                        "ADDED", key, pb.name, pb.path, "", "", pb.dataType, pb.value, pb.description
                });
            } else if (pa != null && pb == null) {
                compareTableModel.addRow(new Object[]{
                        "REMOVED", key, pa.name, pa.path, pa.dataType, pa.value, "", "", pa.description
                });
            } else if (pa != null && pb != null && !pa.isEquivalent(pb)) {
                compareTableModel.addRow(new Object[]{
                        "CHANGED", key, pa.name, pa.path, pa.dataType, pa.value, pb.dataType, pb.value,
                        chooseDescription(pa.description, pb.description)
                });
            }
        }

        if (compareTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(frame,
                    "Отличий не найдено",
                    "Сравнение",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private Map<String, ParamRecord> readParamMap(int configId) {
        Map<String, ParamRecord> map = new LinkedHashMap<>();

        String sql = "SELECT composite_key, name, description, data_type, value, path, tenant, scope, location " +
                "FROM parameters WHERE config_id = ? ORDER BY id";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String key = rs.getString("composite_key");
                    String value = rs.getString("value");

                    if (map.containsKey(key)) {
                        // В исходном JSON ключ может повторяться с несколькими values,
                        // поэтому склеиваем такие значения через '; '.
                        ParamRecord prev = map.get(key);
                        String mergedValue = mergeValues(prev.value, value);
                        map.put(key, new ParamRecord(
                                key, prev.name, prev.description, prev.dataType,
                                mergedValue, prev.path, prev.tenant, prev.scope, prev.location
                        ));
                    } else {
                        map.put(key, new ParamRecord(
                                key,
                                rs.getString("name"),
                                rs.getString("description"),
                                rs.getString("data_type"),
                                value,
                                rs.getString("path"),
                                rs.getString("tenant"),
                                rs.getString("scope"),
                                rs.getString("location")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame,
                    "Ошибка чтения для сравнения: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }

        return map;
    }

    private String mergeValues(String a, String b) {
        String left = a == null ? "" : a;
        String right = b == null ? "" : b;

        if (left.isBlank()) return right;
        if (right.isBlank()) return left;

        Set<String> unique = new LinkedHashSet<>();
        for (String s : left.split(";\\s*")) {
            if (!s.isBlank()) unique.add(s);
        }
        for (String s : right.split(";\\s*")) {
            if (!s.isBlank()) unique.add(s);
        }
        return String.join("; ", unique);
    }

    private String chooseDescription(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return "";
    }

    private void initDb() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
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

    private static JsonObject getObj(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return obj.getAsJsonObject(key);
    }

    private static JsonArray getArr(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull() || !obj.get(key).isJsonArray()) {
            return null;
        }
        return obj.getAsJsonArray(key);
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key) || obj.get(key).isJsonNull()) {
            return "";
        }
        return jsonPrimitiveToString(obj.get(key));
    }

    private static String pathToString(JsonArray pathArr) {
        if (pathArr == null || pathArr.isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (JsonElement el : pathArr) {
            String part = jsonPrimitiveToString(el).trim();
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return String.join("/", parts);
    }

    private static String jsonPrimitiveToString(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return "";
        }
        if (el.isJsonPrimitive()) {
            return el.getAsJsonPrimitive().getAsString();
        }
        return el.toString();
    }

    private static String buildCompositeKey(String tenant, String scope, String location, String path, String name) {
        return String.join("|",
                nullToEmpty(tenant),
                nullToEmpty(scope),
                nullToEmpty(location),
                nullToEmpty(path),
                nullToEmpty(name));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private record ConfigRecord(int id, String name, String fileName, String importedAt) {
        @Override
        public String toString() {
            return name + "  [" + importedAt + "]";
        }
    }

    private record ParamRecord(String key,
                               String name,
                               String description,
                               String dataType,
                               String value,
                               String path,
                               String tenant,
                               String scope,
                               String location) {
        boolean isEquivalent(ParamRecord other) {
            return Objects.equals(name, other.name)
                    && Objects.equals(description, other.description)
                    && Objects.equals(dataType, other.dataType)
                    && Objects.equals(value, other.value)
                    && Objects.equals(path, other.path)
                    && Objects.equals(tenant, other.tenant)
                    && Objects.equals(scope, other.scope)
                    && Objects.equals(location, other.location);
        }
    }
}
