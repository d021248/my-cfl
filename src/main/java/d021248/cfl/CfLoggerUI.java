package d021248.cfl;

import d021248.cfl.cmd.*;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

public class CfLoggerUI {

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

    private CfTextArea textArea = new CfTextArea();
    private boolean isControlKeyDown = false;
    private boolean isScrollingOn = false;

    public CfLoggerUI() {
        Cf.setErrLogger(this::log);
        Cf.setOutLogger(this::log);
        SwingUtilities.invokeLater(this::initialize);
    }

    private void initialize() {
        // ------------------------------------------------------------------
        // set Look & Feel
        // ------------------------------------------------------------------
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
        }

        var frame = new JFrame(TITLE);

        // ------------------------------------------------------------------
        // load Logo
        // ------------------------------------------------------------------
        BufferedImage image = null;
        try {
            image = ImageIO.read(this.getClass().getResource(LOGO));
        } catch (Exception e) {
        }

        // ------------------------------------------------------------------
        // add the TextArea
        // ------------------------------------------------------------------
        var textAreaScrollPane = new JScrollPane(textArea);
        textAreaScrollPane.setBorder(BorderFactory.createEtchedBorder());

        textAreaScrollPane.getHorizontalScrollBar().addAdjustmentListener(textArea);
        textAreaScrollPane.getVerticalScrollBar().addAdjustmentListener(textArea);

        // ------------------------------------------------------------------
        // add the buttons
        // ------------------------------------------------------------------
        var buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBorder(BorderFactory.createEtchedBorder());

        // we need this to set the name of the SPACE in the title!!
        Consumer<String> space2titleLogger = s -> {
            var space = Cf.target().space;
            frame.setTitle(TITLE.replace("cfLogger", space));
            this.log(space);
        };

        // this is the action listener for button 'cf target'
        ActionListener cfTargetActionListener = e -> space2titleLogger.accept(Cf.target().space);
        cfTargetActionListener.actionPerformed(null); // we execute it here right away to set the name of the SPACE in
        // the title

        var cfTargetButton = new JButton(BT_CF_TARGET);
        cfTargetButton.addActionListener(cfTargetActionListener);
        buttonPanel.add(cfTargetButton);

        var cfAppsButton = new JButton(BT_CF_APPS);
        // cfAppsButton.addActionListener(e -> CfApplicationSelector.getInstance(this,
        // textArea));
        buttonPanel.add(cfAppsButton);

        var cfLogsButton = new JButton(BT_LOG_ALL);
        cfLogsButton.addActionListener(e -> Cf.logs());
        buttonPanel.add(cfLogsButton);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        JSeparator vSep1 = new JSeparator(SwingConstants.VERTICAL);
        vSep1.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep1);
        buttonPanel.add(Box.createHorizontalStrut(2));

        // ------------------------------------------------------------------
        // add the buttons
        // ------------------------------------------------------------------
        JButton clearButton = new JButton(BT_CLEAR);
        clearButton.addActionListener(e -> textArea.clear());
        buttonPanel.add(clearButton);

        JButton toggleScrollButton = new JButton(BT_STOP_AUTO_SCROLL);
        toggleScrollButton.addActionListener(e -> {
            if (textArea.isScrollingOn()) {
                toggleScrollButton.setText(BT_START_AUTO_SCROLL);
                textArea.setScrolling(false);
            } else {
                toggleScrollButton.setText(BT_STOP_AUTO_SCROLL);
                textArea.setScrolling(true);
            }
        });
        buttonPanel.add(toggleScrollButton);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        JSeparator vSep2 = new JSeparator(SwingConstants.VERTICAL);
        vSep2.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep2);
        buttonPanel.add(Box.createHorizontalStrut(0));

        // ------------------------------------------------------------------
        // add the filter
        // ------------------------------------------------------------------
        JTextField filterValueTextField = new JTextField("", 20);
        JButton toggleFilterButton = new JButton(BT_FILTER_ON);
        BiFunction<String, Boolean, String> setHighlight = (s, b) -> {
            if (s == null || s.length() == 0) {
                textArea.unsetHighlight();
            } else {
                textArea.setHighlight(s);
            }

            cfTargetButton.setEnabled(!textArea.isHighlightOn());
            cfLogsButton.setEnabled(!textArea.isHighlightOn());
            clearButton.setEnabled(!textArea.isHighlightOn());
            cfAppsButton.setEnabled(!textArea.isHighlightOn());

            if (b) {
                filterValueTextField.setText(s);
            }
            toggleFilterButton.setEnabled(filterValueTextField.getText().length() > 0);
            toggleFilterButton.setText(toggleFilterButton.isEnabled() ? BT_FILTER_ON : BT_FILTER_OFF);

            // copy to clipboard
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            StringSelection stringSelection = new StringSelection(s);
            clpbrd.setContents(stringSelection, null);
            return s;
        };

        toggleFilterButton.addActionListener(e -> {
            if (!textArea.isHighlightOn()) {
                return;
            }

            if (textArea.isFilterOn()) {
                toggleFilterButton.setText(BT_FILTER_ON);
                textArea.setFilter(false);
            } else {
                toggleFilterButton.setText(BT_FILTER_OFF);
                textArea.setFilter(true);
            }
        });
        toggleFilterButton.setEnabled(false);
        filterValueTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                String filterValue = filterValueTextField.getText() + getPrintableChar(keyEvent.getKeyChar());
                if (!filterValue.startsWith(">")) {
                    setHighlight.apply(filterValue, false);
                }
            }

            public void keyPressed(KeyEvent keyEvent) {
                String filterValue = filterValueTextField.getText() + getPrintableChar(keyEvent.getKeyChar());
                if (filterValue.startsWith(">")) {
                    if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                        String command = filterValue.substring(1).trim();
                        command = (command.startsWith("cf") ? "" : "CMD /C ") + command;
                        // TODO CfCommandLogger.logCommand(CfLoggerUI2.this, command);
                        setHighlight.apply("", false);
                    }
                }
            }

            public String getPrintableChar(char c) {
                return isPrintableChar(c) ? "" + c : "";
            }

            public boolean isPrintableChar(char c) {
                Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
                return ((!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null
                        && block != Character.UnicodeBlock.SPECIALS);
            }
        });

        JPanel filterValueTextPanel = new JPanel(new BorderLayout());
        // filterValueTextPanel.add(filterValueLabel, BorderLayout.WEST);
        filterValueTextPanel.add(toggleFilterButton, BorderLayout.WEST);
        filterValueTextPanel.add(filterValueTextField, BorderLayout.EAST);
        buttonPanel.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPanel.add(filterValueTextPanel);

        // ------------------------------------------------------------------
        // add the separator
        // ------------------------------------------------------------------
        JSeparator vSep3 = new JSeparator(SwingConstants.VERTICAL);
        vSep3.setPreferredSize(new Dimension(2, (int) cfTargetButton.getPreferredSize().getHeight()));
        buttonPanel.add(Box.createHorizontalStrut(4));
        buttonPanel.add(vSep3);
        buttonPanel.add(Box.createHorizontalStrut(0));

        // ------------------------------------------------------------------
        // add the button
        // ------------------------------------------------------------------
        JButton save = new JButton(BT_SAVE);
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
        WindowAdapter windowAdapter = new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                frame.dispose();
                Command.stopAll();
                System.exit(0);
            }
            // @Override
            // public void windowGainedFocus(WindowEvent e) {
            // System.out.println("windowGainedFocus");
            // frame.setOpacity(1.0f);
            //
            // }
            //
            // @Override
            // public void windowLostFocus(WindowEvent e) {
            // System.out.println("windowLostFocus");
            // frame.setOpacity(0.5f);
            // }
        };
        frame.addWindowListener(windowAdapter);
        frame.addWindowFocusListener(windowAdapter);
        frame.addWindowStateListener(windowAdapter);
        frame.pack();
        frame.setSize(800, 600);
        frame.setVisible(true);

        // ------------------------------------------------------------------
        // add the mouse listener for the textArea
        // since we manipulate ui elements (e.g. button texts),
        // we have to define the mouse listener
        // after the declarations of these ui elements!
        // ------------------------------------------------------------------

        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                isControlKeyDown = e.isControlDown();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                isControlKeyDown = e.isControlDown();
            }
        });

        MouseAdapter textAreaMouseAdapter = new MouseAdapter() {
            boolean isDraggedOn = false;
            int start = 0;
            int end = 0;
            int pos = 0;

            @Override
            public void mouseDragged(MouseEvent e) {
                if (textArea.isFilterOn()) {
                    return;
                }

                pos = textArea.getCaretPosition();
                if (!isDraggedOn) {
                    start = pos;
                    end = pos;
                    isDraggedOn = true;
                } else {
                    end = pos;
                }

                textArea.setSelectionStart(start < end ? start : end);
                textArea.setSelectionEnd(start > end ? start : end);
                setHighlight.apply(textArea.getSelectedText(), true);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                if (isDraggedOn) {
                    isDraggedOn = false;
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // setHighlight.apply("", true);
                isScrollingOn = textArea.isScrollingOn();
                toggleScrollButton.setText("start auto-scroll");
                textArea.setScrolling(false);
                super.mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // don't switch scrolling on again - otherwise screen will always move to the
                // end
                // toggleScrollButton.setText(isScrollingOn ? BT_STOP_AUTO_SCROLL :
                // BT_START_AUTO_SCROLL);
                // textArea.setScrolling(isScrollingOn);

                super.mouseReleased(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON1) {
                    return;
                }

                if (e.getClickCount() != 2) {
                    // toggleScrollButton.setText("stop auto-scroll");
                    // textArea.setScrolling(true);
                    return;
                }
                if (e.isConsumed()) {
                    return;
                }

                // ------------------------------------------------------------------
                // find the selected word
                // ------------------------------------------------------------------
                int offset = textArea.viewToModel(e.getPoint());
                try {
                    // ------------------------------------------------------------------
                    // 1. find the selected line
                    // ------------------------------------------------------------------
                    int rowStart = Utilities.getRowStart(textArea, offset);
                    int rowEnd = Utilities.getRowEnd(textArea, offset);
                    int xoffset = offset - rowStart;
                    textArea.setCaretPosition(xoffset);
                    String selectedLine = textArea.getText().substring(rowStart, rowEnd);

                    // ------------------------------------------------------------------
                    // 2. find the selected word
                    // ------------------------------------------------------------------
                    String selectedWord = null;
                    int ws = 0; // word start position
                    int we = 0; // word end position
                    for (int i = 0; i < selectedLine.length(); i++) {
                        char c = selectedLine.charAt(i);

                        if (c == ' ') { // reached end of word
                            if (i <= xoffset) { // we have not reached the
                                // cursor position
                                ws = i + 1; // new word start position
                                we = i + 1; // new word end position
                                continue;
                            } else { // we already have reached the cursor
                                // position
                                we = i; // this is the final word end position
                                break;
                            }
                        } else { // not yet reached the end of a word
                            we = we + 1; // word end --> one char more
                        }
                    }

                    // ------------------------------------------------------------------
                    // 3. cut out the selected word
                    // ------------------------------------------------------------------
                    if (we >= ws) {
                        selectedWord = selectedLine.substring(ws, we).trim();
                    }

                    // ------------------------------------------------------------------
                    // 4. toggle filter on/off
                    // ------------------------------------------------------------------
                    if (textArea.isHighlightOn()) {
                        textArea.setFilter(false);
                        setHighlight.apply("", true);
                    } else {
                        setHighlight.apply(selectedWord, true);
                    }
                } catch (BadLocationException e1) {
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (isControlKeyDown) {
                    if (e.getWheelRotation() < 0) {
                        textArea.decreaseFont();
                    } else {
                        textArea.increaseFont();
                    }
                }
                super.mouseWheelMoved(e);
            }
        };

        // textAreaPane.addMouseMotionListener(textAreaMouseAdapter);
        // textAreaPane.addMouseListener(textAreaMouseAdapter);
        textAreaScrollPane.addMouseWheelListener(textAreaMouseAdapter);
        textArea.addMouseMotionListener(textAreaMouseAdapter);
        textArea.addMouseListener(textAreaMouseAdapter);
        // textArea.addMouseWheelListener(textAreaMouseAdapter);

    }

    private void save() {
        try {
            Path path = Files.write(Paths.get(SAVE_FILEPATH), textArea.getText().getBytes(), StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Desktop desktop = Desktop.getDesktop();
            desktop.edit(path.toFile());
            path.toFile().deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String s) {
        textArea.appendLine(s);
    }
}
