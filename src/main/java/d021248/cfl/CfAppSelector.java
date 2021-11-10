package d021248.cfl;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import d021248.cfl.cmd.Cf;
import d021248.cfl.cmd.Command;

class CfAppSelector extends JComponent {

    private static final String CRLF = System.getProperty("line.separator", "\n");

    private static final String TITLE = ".-=:#[ cfApplicationSelector ]#:=-.";

    // private final transient CfLogo logo = new CfLogo(this);
    private final CfLoggerUI loggerUI;

    public CfAppSelector(CfLoggerUI loggerUI) {
        this.loggerUI = loggerUI;
        initialize();
    }

    private JTable table = null;
    private BufferedImage image = null;

    private void initialize() {
        // ------------------------------------------------------------------
        // set Look & Feel
        // ------------------------------------------------------------------
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }

        // ------------------------------------------------------------------
        // add the List
        // ------------------------------------------------------------------
        table = new JTable() {
            public CfLogo logo = new CfLogo(this);

            @Override
            protected synchronized void paintComponent(Graphics g) {
                setTableSelection();
                var g2d = (Graphics2D) g.create();
                super.paintComponent(g2d);
                logo.paintLogo(g2d);
                g2d.dispose();
            }
        };

        populateTable();
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        var tableColumnModel = table.getColumnModel();
        tableColumnModel.getColumn(0).setPreferredWidth(160);
        tableColumnModel.getColumn(1).setPreferredWidth(48);
        tableColumnModel.getColumn(2).setPreferredWidth(48);
        tableColumnModel.getColumn(3).setPreferredWidth(48);
        tableColumnModel.getColumn(4).setPreferredWidth(48);
        tableColumnModel.getColumn(5).setPreferredWidth(192);
        tableColumnModel.getColumn(5).setPreferredWidth(48);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                var row = table.rowAtPoint(evt.getPoint());
                var col = table.columnAtPoint(evt.getPoint());

                if (row >= 0 && col >= 0) {
                    var tableModel = table.getModel();
                    var isLogged = (Boolean) tableModel.getValueAt(row, 6);
                    var appName = (String) tableModel.getValueAt(row, 0);

                    switch (col) {
                    case 6:
                        if (!isLogged) {
                            // TODO CfCommandLogger.unlogApplication(appName);
                            table.setValueAt(false, row, 6);
                        } else {
                            // TODO CfCommandLogger.logApplication(logger, appName);
                            table.setValueAt(true, row, 6);
                        }
                        break;
                    case 1:
                        doActionCommand(appName);
                        break;
                    case 0:
                        // CfEnvironment.getInstance(logger, parent.getParent(),
                        // appName);

                        getEnvironment(appName);

                        // CfEnvironment.getInstance(parent, appName);
                        break;
                    default:
                        break;
                    }
                    evt.consume();
                    setTableSelection();
                }
            }
        });

        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        var tablePane = new JScrollPane((new JPanel(new GridLayout(1, 2))).add(table));
        tablePane.setBorder(BorderFactory.createEtchedBorder());

        // ------------------------------------------------------------------
        // add the buttons
        // ------------------------------------------------------------------
        var buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        var logAllButton = new JButton("log all");
        logAllButton.addActionListener(e -> logApplicationList());
        buttonPanel.add(logAllButton);

        var unlogAllButton = new JButton("unlog all");
        unlogAllButton.addActionListener(e -> unlogApplicationList());
        buttonPanel.add(unlogAllButton);

        // ------------------------------------------------------------------
        // add the frame itself
        // ------------------------------------------------------------------

        var parent = loggerUI.textArea;
        var optionPane = new JOptionPane();
        optionPane.setBorder(BorderFactory.createEtchedBorder());
        // pane.setLocation(250, 250);
        optionPane.setLayout(new BorderLayout());
        optionPane.add(tablePane, BorderLayout.CENTER);
        optionPane.add(buttonPanel, BorderLayout.SOUTH);

        var dialog = optionPane.createDialog(parent, "blablabla");
        dialog.setModal(false);
        if (image != null) {
            dialog.setIconImage(image);
        }
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                if (parent != null) {
                    parent.setEnabled(true);
                }
            }

            public void windowClosing(WindowEvent e) {
                if (parent != null) {
                    parent.setEnabled(true);
                }
            }
        });
        dialog.setPreferredSize(new Dimension(512, 480));
        dialog.setResizable(true);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void populateTable() {
        table.setModel(getTableModel());
        setTableSelection();
    }

    private void setTableSelection() {
        var tableModel = table.getModel();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 6))) {
                table.addRowSelectionInterval(i, i);
            } else {
                table.removeRowSelectionInterval(i, i);
            }
        }
    }

    private TableModel getTableModel() {
        String[] columnNames = { "name", "state", "instances", "memory", "disk", "urls", "logged" };
        var appList = Cf.apps();
        Object[][] data = new Object[appList.size()][columnNames.length];
        for (int i = 0; i < appList.size(); i++) {
            var app = appList.get(i);
            var isLogged = Command.activeList().stream().anyMatch(c -> c.cmd().contains(app.name));
            data[i][6] = isLogged;
            data[i][0] = app.name;
            data[i][1] = app.state;
            data[i][2] = app.instances;
            data[i][3] = app.memory;
            data[i][4] = app.disk;
            data[i][5] = app.urls;
        }

        return new DefaultTableModel(data, columnNames) {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return getValueAt(0, columnIndex).getClass();
            }
        };
    }

    private void logApplicationList() {
        Cf.logs(loggerUI::logger);
        populateTable();
    }

    private void unlogApplicationList() {
        Cf.stopLogs();
        populateTable();
    }

    private void doActionCommand(String appName) {
        var radioButtons = new JRadioButton[5];
        var buttonGroup = new ButtonGroup();

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

        for (var i = 0; i < radioButtons.length; i++) {
            buttonGroup.add(radioButtons[i]);
        }
        radioButtons[0].setSelected(true);

        var showButton = new JButton("execute");

        var panel = new JPanel();
        var label = new JLabel("Actions for " + appName);

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(label);

        for (var i = 0; i < radioButtons.length; i++) {
            panel.add(radioButtons[i]);
        }

        var optionPane = new JOptionPane();
        optionPane.setBorder(BorderFactory.createEtchedBorder());
        optionPane.setLayout(new BorderLayout());
        optionPane.add(panel, BorderLayout.CENTER);
        optionPane.add(showButton, BorderLayout.SOUTH);

        var dialog = optionPane.createDialog(table, appName);
        dialog.setModal(true);

        showButton.addActionListener(e -> {
            Cf.run(loggerUI::logger, buttonGroup.getSelection().getActionCommand());
            dialog.dispose();
            populateTable();
        });

        if (image != null) {
            dialog.setIconImage(image);
        }
        // dialog.setPreferredSize(new Dimension(512, 480));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setVisible(true);
    }

    private void getEnvironment(String appName) {
        try {
            var env = Cf.env(appName);
            var path = Files.write(Paths.get("./tmp.txt"), env.getBytes(), StandardOpenOption.CREATE);
            var desktop = Desktop.getDesktop();
            desktop.edit(path.toFile());
            path.toFile().deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
