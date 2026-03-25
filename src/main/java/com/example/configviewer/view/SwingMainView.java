package com.example.configviewer.view;

import com.example.configviewer.model.AppUiSettings;
import com.example.configviewer.model.ConfigRecord;
import com.example.configviewer.model.ParamDiffRecord;
import com.example.configviewer.model.ParamRecord;
import com.example.configviewer.presenter.MainViewListener;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class SwingMainView implements MainView {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AppUiSettings settings;
    private MainViewListener listener = new NoopMainViewListener();

    private JFrame frame;
    private DefaultListModel<ConfigRecord> configListModel;
    private JList<ConfigRecord> configList;

    private DefaultTableModel viewTableModel;
    private TableRowSorter<DefaultTableModel> viewSorter;
    private JTextField searchField;

    private JComboBox<ConfigRecord> compareA;
    private JComboBox<ConfigRecord> compareB;
    private DefaultTableModel compareTableModel;

    public SwingMainView(AppUiSettings settings) {
        this.settings = settings;
        buildUi();
    }

    @Override
    public void setListener(MainViewListener listener) {
        this.listener = listener == null ? new NoopMainViewListener() : listener;
    }

    @Override
    public void showWindow() {
        frame.setVisible(true);
    }

    @Override
    public void showConfigs(List<ConfigRecord> configs) {
        configListModel.clear();
        compareA.removeAllItems();
        compareB.removeAllItems();

        for (ConfigRecord config : configs) {
            configListModel.addElement(config);
            compareA.addItem(config);
            compareB.addItem(config);
        }

        if (!configs.isEmpty() && configList.getSelectedValue() == null) {
            configList.setSelectedIndex(0);
        }

        if (compareA.getItemCount() > 0) {
            compareA.setSelectedIndex(0);
        }
        if (compareB.getItemCount() > 1) {
            compareB.setSelectedIndex(1);
        } else if (compareB.getItemCount() == 1) {
            compareB.setSelectedIndex(0);
        }
    }

    @Override
    public void selectConfigById(int configId) {
        for (int i = 0; i < configListModel.size(); i++) {
            ConfigRecord config = configListModel.getElementAt(i);
            if (config.id() == configId) {
                configList.setSelectedIndex(i);
                compareA.setSelectedItem(config);
                return;
            }
        }
    }

    @Override
    public void showParameters(List<ParamRecord> params) {
        viewTableModel.setRowCount(0);
        for (ParamRecord param : params) {
            viewTableModel.addRow(new Object[]{
                    param.name(),
                    param.description(),
                    param.dataType(),
                    param.value(),
                    param.path(),
                    param.tenant(),
                    param.scope(),
                    param.location()
            });
        }
    }

    @Override
    public void showDiffs(List<ParamDiffRecord> diffs) {
        compareTableModel.setRowCount(0);

        for (ParamDiffRecord diff : diffs) {
            compareTableModel.addRow(new Object[]{
                    diff.status(),
                    diff.key(),
                    diff.name(),
                    diff.path(),
                    diff.dataTypeA(),
                    diff.valueA(),
                    diff.dataTypeB(),
                    diff.valueB(),
                    diff.description()
            });
        }
    }

    @Override
    public void showInfo(String message) {
        JOptionPane.showMessageDialog(frame, message, "Информация", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void showWarning(String message) {
        JOptionPane.showMessageDialog(frame, message, "Внимание", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void showError(String message, Throwable error) {
        JOptionPane.showMessageDialog(frame, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
        if (error != null) {
            error.printStackTrace();
        }
    }

    private void buildUi() {
        frame = new JFrame("Config JSON Viewer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(settings.windowWidth(), settings.windowHeight());
        frame.setLocationRelativeTo(null);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton importButton = new JButton("Импорт JSON");
        JButton refreshButton = new JButton("Обновить");

        importButton.addActionListener(e -> onImportClicked());
        refreshButton.addActionListener(e -> listener.onRefreshRequested());

        topPanel.add(importButton);
        topPanel.add(refreshButton);

        configListModel = new DefaultListModel<>();
        configList = new JList<>(configListModel);
        configList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        configList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            ConfigRecord selected = configList.getSelectedValue();
            if (selected != null) {
                listener.onConfigSelected(selected);
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
    }

    private JPanel createViewTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel searchPanel = new JPanel(new BorderLayout(8, 8));
        searchPanel.add(new JLabel("Поиск:"), BorderLayout.WEST);

        searchField = new JTextField();
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

        JTable table = new JTable(viewTableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(settings.tableRowHeight());

        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(320);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(330);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);
        table.getColumnModel().getColumn(5).setPreferredWidth(160);
        table.getColumnModel().getColumn(6).setPreferredWidth(280);
        table.getColumnModel().getColumn(7).setPreferredWidth(130);

        viewSorter = new TableRowSorter<>(viewTableModel);
        table.setRowSorter(viewSorter);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCompareTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        compareA = new JComboBox<>();
        compareB = new JComboBox<>();
        JButton compareButton = new JButton("Сравнить");

        compareButton.addActionListener(e -> listener.onCompareRequested(
                (ConfigRecord) compareA.getSelectedItem(),
                (ConfigRecord) compareB.getSelectedItem()
        ));

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
        table.setRowHeight(settings.tableRowHeight());

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

    private void onImportClicked() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(false);
        chooser.setDialogTitle("Выберите JSON-файл параметров");

        int result = chooser.showOpenDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path file = chooser.getSelectedFile().toPath();
        String defaultName = stripExtension(file.getFileName().toString()) + "_" + TIME_FMT.format(LocalDateTime.now());
        String configName = JOptionPane.showInputDialog(frame, "Введите имя конфигурации для базы:", defaultName);

        if (configName == null || configName.isBlank()) {
            return;
        }

        listener.onImportRequested(file, configName.trim());
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

    private String stripExtension(String fileName) {
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    private static final class NoopMainViewListener implements MainViewListener {
        @Override
        public void onImportRequested(Path file, String configName) {
        }

        @Override
        public void onRefreshRequested() {
        }

        @Override
        public void onConfigSelected(ConfigRecord config) {
        }

        @Override
        public void onCompareRequested(ConfigRecord configA, ConfigRecord configB) {
        }
    }
}
