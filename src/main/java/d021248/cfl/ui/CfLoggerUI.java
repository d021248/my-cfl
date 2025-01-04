package d021248.cfl.ui;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import d021248.cfl.cmd.Cf;
import d021248.cfl.cmd.Command;

public class CfLoggerUI implements Runnable {

    private static final String TITLE = ".-=:#[ cfLogger ]#:=-.";
    private static final String LOGO = "D021248.jpg";

    // Button Texts
    protected static final String BT_CF_TARGET = "cf target";
    protected static final String BT_CF_APPS = "cf apps";
    protected static final String BT_LOG_ALL = "log all";
    protected static final String BT_CLEAR = "clear";
    protected static final String BT_START_AUTO_SCROLL = "start auto-scroll";
    protected static final String BT_STOP_AUTO_SCROLL = "stop auto-scroll";
    protected static final String BT_FILTER_ON = "filter on";
    protected static final String BT_FILTER_OFF = "filter off";
    protected static final String BT_SAVE = "save";

    private static final String SAVE_FILEPATH = "./tmp.txt";

    // accessible for KeyAndMouseAdapter
    protected KeyAndMouseAdapter keyAndMouseAdapter;
    protected CfTextArea textArea;
    protected JScrollPane textAreaScrollPane;
    protected JTextField filterValueTextField;
    protected JButton toggleFilterButton;
    protected JButton toggleScrollButton;

    public static CfLoggerUI startNewInstance() {
        var cfLoggerUI = new CfLoggerUI();
        Thread.ofPlatform().start(cfLoggerUI);
        return cfLoggerUI;
    }

    private CfLoggerUI() {
    }

    public void run() {
        initialize();
    }

    private void initialize() {
        setLookAndFeel();
        var frame = createMainFrame();
        loadLogo(frame);
        addTextArea(frame);
        addButtons(frame);
        keyAndMouseAdapter = new KeyAndMouseAdapter(this);
        frame.setVisible(true);
        fetchCfTarget(frame);
    }

    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JFrame createMainFrame() {
        var frame = new JFrame(TITLE);
        frame.setLocation(200, 200);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                frame.dispose();
                Command.stopAll();
                textArea.exit();
                System.err.println(String.format("exit: %s", CfLoggerUI.this.getClass().getSimpleName()));
                System.exit(0); // will close all instances
            }
        });
        frame.pack();
        frame.setSize(800, 600);
        return frame;
    }

    private void loadLogo(JFrame frame) {
        try {
            BufferedImage image = ImageIO.read(this.getClass().getResource(LOGO));
            if (image != null) {
                frame.setIconImage(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addTextArea(JFrame frame) {
        textArea = new CfTextArea();
        textAreaScrollPane = new JScrollPane(textArea);
        textAreaScrollPane.setBorder(BorderFactory.createEtchedBorder());
        textAreaScrollPane.getHorizontalScrollBar().addAdjustmentListener(textArea);
        textAreaScrollPane.getVerticalScrollBar().addAdjustmentListener(textArea);
        frame.getContentPane().add(textAreaScrollPane, BorderLayout.CENTER);
    }

    private void addButtons(JFrame frame) {
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        addButton(buttonPanel, BT_CF_TARGET, e -> fetchCfTarget(frame));
        addButton(buttonPanel, BT_CF_APPS, e -> new CfAppSelector(this));
        addButton(buttonPanel, BT_LOG_ALL, e -> Cf.logs(this::logger));
        addSeparator(buttonPanel);
        addButton(buttonPanel, BT_CLEAR, e -> textArea.clear());
        addToggleScrollButton(buttonPanel);
        addSeparator(buttonPanel);
        addFilterComponents(buttonPanel);
        addSeparator(buttonPanel);
        addButton(buttonPanel, BT_SAVE, e -> Thread.ofVirtual().start(this::save));

        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addButton(JPanel panel, String text, ActionListener actionListener) {
        var button = new JButton(text);
        button.addActionListener(actionListener);
        panel.add(button);
    }

    private void addSeparator(JPanel panel) {
        var separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 30));
        panel.add(Box.createHorizontalStrut(4));
        panel.add(separator);
        panel.add(Box.createHorizontalStrut(4));
    }

    private void addToggleScrollButton(JPanel panel) {
        toggleScrollButton = new JButton(BT_STOP_AUTO_SCROLL);
        toggleScrollButton.addActionListener(e -> toggleScrolling());
        panel.add(toggleScrollButton);
    }

    private void addFilterComponents(JPanel panel) {
        toggleFilterButton = new JButton(BT_FILTER_ON);
        filterValueTextField = new JTextField("", 20);

        toggleFilterButton.addActionListener(e -> toggleFilter());
        toggleFilterButton.setEnabled(false);

        panel.add(toggleFilterButton);
        panel.add(filterValueTextField);
    }

    private void fetchCfTarget(JFrame frame) {
        var target = Cf.target(this::logger);
        SwingUtilities.invokeLater(() -> frame.setTitle(TITLE.replace("cfLogger", target.space())));
    }

    private void toggleScrolling() {
        if (textArea.isScrollingActive()) {
            toggleScrollButton.setText(BT_START_AUTO_SCROLL);
            textArea.stopScrolling();
        } else {
            toggleScrollButton.setText(BT_STOP_AUTO_SCROLL);
            textArea.startScrolling();
        }
    }

    private void toggleFilter() {
        if (!textArea.isHighlightActive()) {
            return;
        }

        if (textArea.isFilterActive()) {
            toggleFilterButton.setText(BT_FILTER_ON);
            textArea.stopFilter();
        } else {
            toggleFilterButton.setText(BT_FILTER_OFF);
            textArea.startFilter();
        }
    }

    private void save() {
        try {
            var path = Files.write(
                    Paths.get(SAVE_FILEPATH),
                    textArea.getText().getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            var desktop = Desktop.getDesktop();
            desktop.edit(path.toFile());
            path.toFile().deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logger(String text) {
        textArea.append(text);
    }
}
