package d021248.cfl;

import d021248.cfl.cmd.Cf;
import d021248.cfl.cmd.Command;
import d021248.cfl.cmd.Shell;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiFunction;
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

public class CfLoggerUI implements Runnable {

    private static final String TITLE = ".-=:#[ cfLogger ]#:=-.";
    private static final String LOGO = "D021248.jpg";

    // Button Texts
    private static final String BT_CF_TARGET = "cf target";
    private static final String BT_CF_APPS = "cf apps";
    private static final String BT_LOG_ALL = "log all";
    private static final String BT_CLEAR = "clear";
    private static final String BT_START_AUTO_SCROLL = "start auto-scroll";
    private static final String BT_STOP_AUTO_SCROLL = "stop auto-scroll";
    private static final String BT_FILTER_ON = "filter on";
    private static final String BT_FILTER_OFF = "filter off";
    private static final String BT_SAVE = "save";

    private static final String SAVE_FILEPATH = "./tmp.txt";

    private CfTextArea textArea;

    public static CfLoggerUI startNewInstance() {
        var cfLoggerUI = new CfLoggerUI();
        new Thread(cfLoggerUI).start();
        return cfLoggerUI;
    }

    private CfLoggerUI() {}

    public void run() {
        initialize();
    }

    private void initialize() {
        // ------------------------------------------------------------------
        // set Look & Feel
        // ------------------------------------------------------------------
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}

        var frame = new JFrame(TITLE);

        // ------------------------------------------------------------------
        // load Logo
        // ------------------------------------------------------------------
        BufferedImage image = null;
        try {
            image = ImageIO.read(this.getClass().getResource(LOGO));
        } catch (Exception e) {}

        // ------------------------------------------------------------------
        // add the TextArea
        // ------------------------------------------------------------------
        textArea = new CfTextArea();
        var textAreaScrollPane = new JScrollPane(textArea);
        textAreaScrollPane.setBorder(BorderFactory.createEtchedBorder());
        textAreaScrollPane.getHorizontalScrollBar().addAdjustmentListener(textArea);
        textAreaScrollPane.getVerticalScrollBar().addAdjustmentListener(textArea);

        // ------------------------------------------------------------------
        // add the buttons
        // ------------------------------------------------------------------
        var buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        var cfTargetButton = new JButton(BT_CF_TARGET);
        ActionListener cfTargetButtonActionListener = e ->
            new Thread(
                () -> {
                    var target = Cf.target(this::logger);
                    SwingUtilities.invokeLater(() -> frame.setTitle(TITLE.replace("cfLogger", target.space)));
                }
            )
                .start();
        cfTargetButton.addActionListener(cfTargetButtonActionListener);
        buttonPanel.add(cfTargetButton);
        new Thread(() -> cfTargetButtonActionListener.actionPerformed(null)).start();

        var cfAppsButton = new JButton(BT_CF_APPS);
        cfAppsButton.addActionListener(e -> new Thread(() -> Cf.apps(this::logger)).start());
        buttonPanel.add(cfAppsButton);

        var cfLogsButton = new JButton(BT_LOG_ALL);
        cfLogsButton.addActionListener(e -> new Thread(() -> Cf.logs(this::logger)).start());
        buttonPanel.add(cfLogsButton);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        var vSep1 = new JSeparator(SwingConstants.VERTICAL);
        vSep1.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep1);
        buttonPanel.add(Box.createHorizontalStrut(2));

        // ------------------------------------------------------------------
        // add the buttons
        // ------------------------------------------------------------------
        var clearButton = new JButton(BT_CLEAR);
        clearButton.addActionListener(e -> textArea.clear());
        buttonPanel.add(clearButton);

        var toggleScrollButton = new JButton(BT_STOP_AUTO_SCROLL);
        toggleScrollButton.addActionListener(
            e -> {
                if (textArea.isScrollingOn()) {
                    toggleScrollButton.setText(BT_START_AUTO_SCROLL);
                    textArea.setScrolling(false);
                } else {
                    toggleScrollButton.setText(BT_STOP_AUTO_SCROLL);
                    textArea.setScrolling(true);
                }
            }
        );
        buttonPanel.add(toggleScrollButton);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        var vSep2 = new JSeparator(SwingConstants.VERTICAL);
        vSep2.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep2);
        buttonPanel.add(Box.createHorizontalStrut(0));

        // ------------------------------------------------------------------
        // add the filter
        // ------------------------------------------------------------------
        var filterValueTextField = new JTextField("", 20);
        var toggleFilterButton = new JButton(BT_FILTER_ON);
        BiFunction<String, Boolean, String> setHighlight = (filterValue, applyFilter) -> {
            if (filterValue == null || filterValue.isBlank()) {
                textArea.stopHighlight();
            } else {
                textArea.setHighlightText(filterValue);
                textArea.startHighlight();
            }

            // cfTargetButton.setEnabled(!textArea.isHighlightOn());
            // cfLogsButton.setEnabled(!textArea.isHighlightOn());
            // clearButton.setEnabled(!textArea.isHighlightOn());
            // cfAppsButton.setEnabled(!textArea.isHighlightOn());

            // if (applyFilter) {
            // filterValueTextField.setText(filterValue);
            // }
            // System.err.println(filterValueTextField.getText());
            toggleFilterButton.setEnabled(!filterValueTextField.getText().isEmpty());
            toggleFilterButton.setText(toggleFilterButton.isEnabled() ? BT_FILTER_ON : BT_FILTER_OFF);

            // copy to clipboard
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(filterValue), null);
            return filterValue;
        };

        toggleFilterButton.addActionListener(
            e -> {
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
        );
        toggleFilterButton.setEnabled(false);

        filterValueTextField.addKeyListener(
            new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent keyEvent) {
                    var filterValue = String.format(
                        "%s%s",
                        filterValueTextField.getText(),
                        getPrintableChar(keyEvent.getKeyChar())
                    );
                    if (!filterValue.startsWith(">")) {
                        setHighlight.apply(filterValue, false);
                    }
                }

                @Override
                public void keyPressed(KeyEvent keyEvent) {
                    var filterValue = String.format(
                        "%s%s",
                        filterValueTextField.getText(),
                        getPrintableChar(keyEvent.getKeyChar())
                    );
                    if (filterValue.startsWith(">")) {
                        if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                            var command = filterValue.substring(1).trim();
                            new Thread(Shell.cmd(command.split(" ")).stdoutConsumer(CfLoggerUI.this::logger)).start();
                            setHighlight.apply("", false);
                        }
                    }
                }

                public String getPrintableChar(char c) {
                    return isPrintableChar(c) ? String.valueOf(c) : "";
                }

                public boolean isPrintableChar(char c) {
                    var block = Character.UnicodeBlock.of(c);
                    return (
                        (!Character.isISOControl(c)) &&
                        c != KeyEvent.CHAR_UNDEFINED &&
                        block != null &&
                        block != Character.UnicodeBlock.SPECIALS
                    );
                }
            }
        );

        var filterValueTextPanel = new JPanel(new BorderLayout());
        // filterValueTextPanel.add(filterValueLabel, BorderLayout.WEST);
        filterValueTextPanel.add(toggleFilterButton, BorderLayout.WEST);
        filterValueTextPanel.add(filterValueTextField, BorderLayout.EAST);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(filterValueTextPanel);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        var vSep3 = new JSeparator(SwingConstants.VERTICAL);
        vSep3.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep3);
        buttonPanel.add(Box.createHorizontalStrut(0));

        // ------------------------------------------------------------------
        // add the button
        // ------------------------------------------------------------------
        var save = new JButton(BT_SAVE);
        // TODO save.addActionListener(e -> new Thread(() -> this.save()).start());
        buttonPanel.add(save);

        // ------------------------------------------------------------------
        // add the frame itself
        // ------------------------------------------------------------------
        // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(200, 200);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().add(textAreaScrollPane, BorderLayout.CENTER);
        frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        if (image != null) {
            frame.setIconImage(image);
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        var windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                frame.dispose();
                Command.stopAll();
                // System.exit(0);
            }
        };
        frame.addWindowListener(windowAdapter);
        frame.addWindowFocusListener(windowAdapter);
        frame.addWindowStateListener(windowAdapter);
        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);
    }

    private void save() {
        try {
            var path = Files.write(
                Paths.get(SAVE_FILEPATH),
                textArea.getText().getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
            var desktop = Desktop.getDesktop();
            desktop.edit(path.toFile());
            path.toFile().deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logger(String text) {
        System.err.println(text);
        textArea.append(text);
    }
}
