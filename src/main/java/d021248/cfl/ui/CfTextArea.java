package d021248.cfl.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;

class CfTextArea extends JTextArea implements Highlight, Filter, Scrolling, AdjustmentListener {

    private static final String CRLF = System.lineSeparator();
    private static final int MAX_LINES = 65536;
    private static final int MIN_FONT_SIZE = 4;
    private static final int MAX_FONT_SIZE = 32;
    private static final List<String> FONT_NAMES = List.of("Arial", "Courier", "Helvetica", "Monospaced", "Plain");

    private int fontSize = 11;
    private int fontNameIndex = 3;
    private final DefaultHighlighter.DefaultHighlightPainter painter;
    private final CfLogo logo;
    private final String[] linesBuffer;
    private int linesBufferIndex;

    public CfTextArea() {
        setOpaque(false);
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));

        this.painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(128, 196, 255));
        this.linesBuffer = new String[MAX_LINES];
        this.linesBufferIndex = 0;

        this.logo = new CfLogo(this);
        this.logo.start();

        ((DefaultCaret) getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    public void increaseFontSize() {
        updateFontSize(1);
    }

    public void decreaseFontSize() {
        updateFontSize(-1);
    }

    private void updateFontSize(int change) {
        fontSize = Math.min(Math.max(fontSize + change, MIN_FONT_SIZE), MAX_FONT_SIZE);
        setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
        repaint();
    }

    public void changeFont() {
        fontNameIndex = (fontNameIndex + 1) % FONT_NAMES.size();
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));
        repaint();
    }

    public void exit() {
        this.logo.exit();
    }

    @Override
    protected void paintComponent(Graphics g) {
        applyHighlight();
        if (isScrollingActive) {
            truncate();
            setCaretPosition(getDocument().getLength());
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        super.paintComponent(g2d);
        logo.paintLogo(g2d);
        g2d.dispose();
    }

    public void truncate() {
        int numLinesToTruncate = getLineCount() - MAX_LINES - 1;
        if (numLinesToTruncate > 0) {
            try {
                int posOfLastLineToTruncate = getLineEndOffset(numLinesToTruncate - 1);
                replaceRange("", 0, posOfLastLineToTruncate);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void append(String text) {
        if (text == null || text.isBlank()) {
            return;
        }

        String line = text.endsWith(CRLF) ? text : text + CRLF;
        linesBuffer[linesBufferIndex] = line;
        linesBufferIndex = (linesBufferIndex + 1) % MAX_LINES;

        if (isPrettyLoggingActive) {
            var matcher = prettyLoggingPattern.matcher(line);
            if (matcher.find()) {
                line = String.format("%-20s | %s | %s%n", matcher.group(1), matcher.group(2), matcher.group(4));
            }
        }

        if (isHighlightActive && isFilterActive) {
            try {
                if (highlightPattern.matcher(line).find()) {
                    super.append(FILTER_PREFIX + line);
                }
            } catch (PatternSyntaxException pse) {
                super.append(FILTER_PREFIX + line);
            }
        } else {
            super.append(line);
        }
    }

    public void clear() {
        setText("");
        for (int i = 0; i < MAX_LINES; i++) {
            linesBuffer[i] = CRLF;
        }
        linesBufferIndex = 0;
    }

    private void refresh() {
        int caretPosition = getCaretPosition();
        setText("");
        String[] tmpLinesBuffer = new String[MAX_LINES];
        for (int i = 0; i < MAX_LINES; i++) {
            int j = (i + linesBufferIndex) % MAX_LINES;
            String line = linesBuffer[j];
            linesBuffer[j] = CRLF;
            tmpLinesBuffer[i] = line;
        }
        linesBufferIndex = 0;
        for (String line : tmpLinesBuffer) {
            append(line);
        }
        setCaretPosition(caretPosition);
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        // Implement any necessary actions for adjustment value changes here
    }

    // Pretty logging variables
    private boolean isPrettyLoggingActive = false;
    private Pattern prettyLoggingPattern = Pattern.compile("(.*) (.*) \\[(.*)\\] (.*)");

    // Scrolling variables
    private boolean isScrollingActive = true;

    @Override
    public boolean isScrollingActive() {
        return isScrollingActive;
    }

    @Override
    public void startScrolling() {
        isScrollingActive = true;
    }

    @Override
    public void stopScrolling() {
        isScrollingActive = false;
    }

    // Filter and Highlight variables
    private static final String FILTER_PREFIX = "]";
    private String highlightText = null;
    private Pattern highlightPattern = null;
    private boolean isHighlightActive = false;
    private boolean isFilterActive = false;

    @Override
    public void setFilterText(String text) {
        setHighlightText(text);
    }

    @Override
    public String getFilterText() {
        return getHighlightText();
    }

    @Override
    public void startFilter() {
        isFilterActive = !isFilterActive;
        if (isHighlightActive) {
            refresh();
        }
    }

    @Override
    public void stopFilter() {
        isFilterActive = false;
        refresh();
    }

    @Override
    public boolean isFilterActive() {
        return isFilterActive;
    }

    @Override
    public void setHighlightText(String text) {
        if (text == null) {
            return;
        }
        isHighlightActive = true;
        highlightText = text.trim();
        highlightPattern = Pattern.compile(Pattern.quote(highlightText));
        refresh();
    }

    @Override
    public String getHighlightText() {
        return highlightText;
    }

    @Override
    public void applyHighlight() {
        var highlighter = getHighlighter();
        highlighter.removeAllHighlights();
        if (!isHighlightActive) {
            return;
        }
        var matcher = highlightPattern.matcher(getText());
        while (matcher.find()) {
            try {
                highlighter.addHighlight(matcher.start(), matcher.end(), painter);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void removeHighlight() {
        getHighlighter().removeAllHighlights();
        isHighlightActive = false;
        refresh();
    }

    @Override
    public boolean isHighlightActive() {
        return isHighlightActive;
    }
}