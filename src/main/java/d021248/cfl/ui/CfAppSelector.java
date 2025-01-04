package d021248.cfl.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import d021248.cfl.cmd.Cf;
import d021248.cfl.cmd.Command;
import d021248.cfl.cmd.cf.App;

public class CfAppSelector extends JComponent {

    private static final String TITLE = ".-=:#[ app selector ]#:=-.";
    private static final String LOGO = "D021248.jpg";

    private final CfLoggerUI loggerUI;
    private BufferedImage image;
    private JTable cfTable;
    private List<App> appList;

    public CfAppSelector(CfLoggerUI loggerUI) {
        this.loggerUI = loggerUI;
        appList = Cf.apps();
        initialize();
    }

    private void initialize() {
        setLookAndFeel();
        loadLogo();
        createTable();
        createDialog();
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void loadLogo() {
        try {
            image = ImageIO.read(getClass().getResource(LOGO));
        } catch (Exception ignored) {
        }
    }

    private void createTable() {
        cfTable = createDecoratedTable();
        cfTable.setModel(getTableModel());
        populateTable();

        cfTable.addMouseListener(new TableMouseListener());
    }

    private JTable createDecoratedTable() {
        return new JTable() {
            private final CfLogo logo = new CfLogo(this);

            @Override
            protected void paintComponent(Graphics g) {
                logo.paintLogo((Graphics2D) g.create());
                super.paintComponent(g);
            }
        };
    }

    private void populateTable() {
        cfTable.setModel(getTableModel());
        setTableSelection(cfTable.getModel());

        cfTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        cfTable.setFillsViewportHeight(true);
        cfTable.setRowSelectionAllowed(true);
        cfTable.setColumnSelectionAllowed(false);

        TableColumnModel columnModel = cfTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(160);
        columnModel.getColumn(1).setPreferredWidth(48);
        columnModel.getColumn(2).setPreferredWidth(48);
        columnModel.getColumn(3).setPreferredWidth(192);
    }

    private DefaultTableModel getTableModel() {

        String[] columnNames = { "name", "state", "processes", "urls", "logged" };
        Object[][] data = new Object[appList.size()][columnNames.length];

        for (int i = 0; i < appList.size(); i++) {
            var app = appList.get(i);
            boolean isLogged = Command.activeList().stream().anyMatch(c -> c.cmd().contains(app.name()));
            data[i][4] = isLogged;
            data[i][0] = app.name();
            data[i][1] = app.state();
            data[i][2] = app.processes();
            data[i][3] = app.routes();
        }

        return new DefaultTableModel(data, columnNames) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return getValueAt(0, columnIndex).getClass();
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4;
            }
        };
    }

    private void setTableSelection(TableModel tableModel) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean isLogged = (boolean) tableModel.getValueAt(i, 4);
            cfTable.setRowSelectionInterval(i, i);
        }
    }

    private void createDialog() {
        JOptionPane optionPane = new JOptionPane();
        JScrollPane tablePane = new JScrollPane(cfTable);
        JPanel buttonPanel = createButtonPanel();

        optionPane.setLayout(new BorderLayout());
        optionPane.add(tablePane, BorderLayout.CENTER);
        optionPane.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = optionPane.createDialog(loggerUI.textArea, TITLE);
        dialog.setModal(false);
        if (image != null) {
            dialog.setIconImage(image);
        }

        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (loggerUI.textArea != null) {
                    loggerUI.textArea.setEnabled(true);
                }
            }

            @Override
            public void windowClosing(WindowEvent e) {
                if (loggerUI.textArea != null) {
                    loggerUI.textArea.setEnabled(true);
                }
            }
        });

        dialog.setPreferredSize(new Dimension(512, 480));
        dialog.setResizable(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        JButton logAllButton = new JButton("log all");
        logAllButton.addActionListener(e -> logAllApps());
        buttonPanel.add(logAllButton);

        JButton unlogAllButton = new JButton("unlog all");
        unlogAllButton.addActionListener(e -> unlogAllApps());
        buttonPanel.add(unlogAllButton);

        return buttonPanel;
    }

    private void logAllApps() {
        // Cf.logs(loggerUI::logger);
        appList.forEach(app -> Cf.logs(app.name(), loggerUI::logger));
        populateTable();
    }

    private void unlogAllApps() {
        // Cf.stopLogs();
        appList.forEach(app -> Cf.stopLogs(app.name()));
        populateTable();
    }

    private class TableMouseListener extends java.awt.event.MouseAdapter {
        @Override
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            int row = cfTable.rowAtPoint(evt.getPoint());
            int col = cfTable.columnAtPoint(evt.getPoint());

            if (row >= 0 && col >= 0) {
                handleTableClick(row, col);
                evt.consume();
            }
        }
    }

    private void handleTableClick(int row, int col) {
        TableModel model = cfTable.getModel();
        String appName = (String) model.getValueAt(row, 0);
        boolean isLogged = (boolean) model.getValueAt(row, 4);

        switch (col) {
            case 4:
                toggleLogging(row, appName, isLogged);
                break;
            case 1:
                doActionCommand(appName);
                break;
            case 0:
                getEnvironment(appName);
                break;
        }
        setTableSelection(model);
    }

    private void toggleLogging(int row, String appName, boolean isLogged) {
        if (!isLogged) {
            Cf.stopLogs(appName);
            cfTable.setValueAt(false, row, 4);
        } else {
            Cf.logs(appName, loggerUI::logger);
            cfTable.setValueAt(true, row, 4);
        }
    }

    private void doActionCommand(String appName) {
        var radioButtons = new JRadioButton[5];

        radioButtons[0] = new JRadioButton("debug");
        radioButtons[0].setActionCommand("cf ssh -L 8000:127.0.0.1:8000 " + appName);
        radioButtons[1] = new JRadioButton("start");
        radioButtons[1].setActionCommand("cf start " + appName);
        radioButtons[2] = new JRadioButton("stop");
        radioButtons[2].setActionCommand("cf stop " + appName);
        radioButtons[3] = new JRadioButton("restart");
        radioButtons[3].setActionCommand("cf restart " + appName);
        radioButtons[4] = new JRadioButton("restage");
        radioButtons[4].setActionCommand("cf restage " + appName);

        ButtonGroup buttonGroup = new ButtonGroup();
        for (JRadioButton rb : radioButtons) {
            buttonGroup.add(rb);
        }
        radioButtons[0].setSelected(true);

        JButton executeButton = new JButton("execute");

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(new JLabel("Actions for " + appName));
        for (JRadioButton rb : radioButtons) {
            panel.add(rb);
        }

        JOptionPane optionPane = new JOptionPane();
        optionPane.setLayout(new BorderLayout());
        optionPane.add(panel, BorderLayout.CENTER);
        optionPane.add(executeButton, BorderLayout.SOUTH);

        JDialog dialog = optionPane.createDialog(cfTable, appName);
        dialog.setModal(true);

        executeButton.addActionListener(e -> {
            String selectedCommand = buttonGroup.getSelection().getActionCommand();
            Cf.run(loggerUI::logger, selectedCommand);
            dialog.dispose();
            populateTable();
        });

        if (image != null) {
            dialog.setIconImage(image);
        }

        dialog.setResizable(false);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void getEnvironment(String appName) {
        try {
            String env = Cf.env(appName);
            Files.write(Paths.get("./tmp.txt"), env.getBytes(), StandardOpenOption.CREATE);
            Desktop.getDesktop().edit(Paths.get("./tmp.txt").toFile());
            Paths.get("./tmp.txt").toFile().deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}