package d021248.cfl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
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

import d021248.cfl.cmd.Command;

class CfApplicationSelector {

	private final static String CRLF = System.getProperty("line.separator", "\n");

	private final static String LOGO = "D021248.jpg";

	private final static String TITLE = ".-=:#[ cfApplicationSelector]#:=-.";

	public static CfApplicationSelector getInstance(Logger logger, Component parent) {
		return new CfApplicationSelector(logger, parent, TITLE);
	}

	private CfApplicationSelector(Logger logger, Component parent, String title) {
		this.logger = logger;
		initialize(parent, title);
	}

	private final Logger logger;
	private JTable table = null;
	private BufferedImage image = null;

	@SuppressWarnings("serial")
	private void initialize(Component parent, String title) {

		if (parent != null) {
			// parent.setEnabled(false);
		}

		// ------------------------------------------------------------------
		// set Look & Feel
		// ------------------------------------------------------------------
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
		}

		// ------------------------------------------------------------------
		// load Logo
		// ------------------------------------------------------------------
		try {
			image = ImageIO.read(this.getClass().getResource(LOGO));
		} catch (Exception e) {
		}

		// ------------------------------------------------------------------
		// add the List
		// ------------------------------------------------------------------
		table = new JTable() {

			public CfLogo logo = new CfLogo(this.getClass().getResource(LOGO).toString(), this);

			@Override
			synchronized protected void paintComponent(Graphics g) {
				setTableSelection();
				Graphics2D g2d = (Graphics2D) g.create();
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
		TableColumnModel colModel = table.getColumnModel();
		colModel.getColumn(0).setPreferredWidth(160);
		colModel.getColumn(1).setPreferredWidth(48);
		colModel.getColumn(2).setPreferredWidth(48);
		colModel.getColumn(3).setPreferredWidth(48);
		colModel.getColumn(4).setPreferredWidth(48);
		colModel.getColumn(5).setPreferredWidth(192);
		colModel.getColumn(5).setPreferredWidth(48);
		table.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt) {

				int row = table.rowAtPoint(evt.getPoint());
				int col = table.columnAtPoint(evt.getPoint());

				if (row >= 0 && col >= 0) {
					TableModel tm = table.getModel();
					Boolean isLogged = (Boolean) tm.getValueAt(row, 6);
					String appName = (String) tm.getValueAt(row, 0);

					switch (col) {
					case 6:
						if (!isLogged) {
							CfCommandLogger.unlogApplication(appName);
							table.setValueAt(false, row, 6);
						} else {
							CfCommandLogger.logApplication(logger, appName);
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
		JScrollPane tablePane = new JScrollPane((new JPanel(new GridLayout(1, 2))).add(table));
		tablePane.setBorder(BorderFactory.createEtchedBorder());

		// ------------------------------------------------------------------
		// add the buttons
		// ------------------------------------------------------------------
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonPanel.setBorder(BorderFactory.createEtchedBorder());

		JButton logAllButton = new JButton("log all");
		logAllButton.addActionListener(e -> logApplicationList());
		buttonPanel.add(logAllButton);

		JButton unlogAllButton = new JButton("unlog all");
		unlogAllButton.addActionListener(e -> unlogApplicationList());
		buttonPanel.add(unlogAllButton);

		// ------------------------------------------------------------------
		// add the frame itself
		// ------------------------------------------------------------------

		JOptionPane pane = new JOptionPane();
		pane.setBorder(BorderFactory.createEtchedBorder());
		// pane.setLocation(250, 250);
		pane.setLayout(new BorderLayout());
		pane.add(tablePane, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.SOUTH);

		JDialog dialog = pane.createDialog(parent, title);
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
		TableModel model = table.getModel();
		for (int i = 0; i < model.getRowCount(); i++) {
			if ((Boolean) model.getValueAt(i, 6) == true) {
				table.addRowSelectionInterval(i, i);
			} else {
				table.removeRowSelectionInterval(i, i);
			}
		}
	}

	private TableModel getTableModel() {

		String[] columnNames = { "name", "state", "instances", "memory", "disk", "urls", "logged" };

		List<Application> appList = CfCommandLogger.getApplicationList(logger);
		List<Command> commandList = Command.getCommandList();

		Object[][] data = new Object[appList.size()][7];
		for (int i = 0; i < appList.size(); i++) {

			Application app = appList.get(i);
			Boolean isLogged = false;
			for (Command command : commandList) {
				String s = command.getCommandString();
				if (s.contains(app.getName())) {
					isLogged = true;
					break;
				}
			}
			data[i][6] = isLogged;
			data[i][0] = app.getName();
			data[i][1] = app.getState();
			data[i][2] = app.getInstances();
			data[i][3] = app.getMemory();
			data[i][4] = app.getDisk();
			data[i][5] = app.getUrls();

		}

		TableModel tableModel = new DefaultTableModel(data, columnNames) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				return getValueAt(0, columnIndex).getClass();
			}
		};
		return tableModel;
	}

	private void logApplicationList() {
		CfCommandLogger.logApplicationList(logger);
		populateTable();
	}

	private void unlogApplicationList() {
		CfCommandLogger.unlogApplicationList();
		populateTable();
	}

	private void doActionCommand(String appName) {

		JRadioButton[] radioButtons = new JRadioButton[5];
		ButtonGroup group = new ButtonGroup();

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

		for (int i = 0; i < radioButtons.length; i++) {
			group.add(radioButtons[i]);
		}
		radioButtons[0].setSelected(true);

		JButton showButton = new JButton("execute");

		JPanel box = new JPanel();
		JLabel label = new JLabel("Actions for " + appName);

		box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
		box.add(label);

		for (int i = 0; i < radioButtons.length; i++) {
			box.add(radioButtons[i]);
		}

		JOptionPane pane = new JOptionPane();
		pane.setBorder(BorderFactory.createEtchedBorder());
		pane.setLayout(new BorderLayout());
		pane.add(box, BorderLayout.CENTER);
		pane.add(showButton, BorderLayout.SOUTH);

		JDialog dialog = pane.createDialog(table, appName);
		dialog.setModal(true);

		showButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String command = group.getSelection().getActionCommand();
				CfCommandLogger.logCommand(logger, command);
				dialog.dispose();
				populateTable();
			}
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

		StringBuilder sb = new StringBuilder();
		Logger logger = new Logger() {
			@Override
			public void log(String s) {
				sb.append(s).append(CRLF);
			}
		};

		try {
			CfCommandLogger.logCommandSync(logger, "cf env " + appName);
			Path path = Files.write(Paths.get("./tmp.txt"), sb.toString().getBytes(), StandardOpenOption.CREATE);
			Desktop desktop = Desktop.getDesktop();
			desktop.edit(path.toFile());
			path.toFile().deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
