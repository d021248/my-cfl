package d021248.cfl;

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
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

class CfTextArea extends JTextArea implements Highlight, Filter, Scrolling, AdjustmentListener {

    private static final String CRLF = String.format("%n");
    private static final int MAX_LINES = 65536;
    private static final int MIN_FONT_SIZE = 4;
    private static final int MAX_FONT_SIZE = 32;
    private static final List<String> FONT_NAMES = List.of("Arial", "Courier", "Helvetica", "Monospaced", "Plain");
    private static final String EMPTY_LINE = CRLF;

    private static final long serialVersionUID = 1L;
    private int fontSize = 11;
    private int fontNameIndex = 3;
    private final transient DefaultHighlightPainter painter;
    private final transient CfLogo logo;
    private final String[] tmpLinesBuffer;
    private final String[] linesBuffer;
    private int linesBufferIndex;

    public CfTextArea() {
        super();
        setOpaque(false);
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));

        this.painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(128, 196, 255));
        this.tmpLinesBuffer = new String[MAX_LINES];
        this.linesBuffer = new String[MAX_LINES];
        this.linesBufferIndex = 0;

        this.logo = new CfLogo(this);
        this.logo.start();

        var caret = (DefaultCaret) this.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
    }

    // ----------------------------------------------------------------------------------------
    // font
    // ----------------------------------------------------------------------------------------
    public void increaseFontSize() {
        fontSize = (fontSize >= MAX_FONT_SIZE) ? MAX_FONT_SIZE : fontSize + 1;
        setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
        repaint();
    }

    public void decreaseFontSize() {
        fontSize = (fontSize <= MIN_FONT_SIZE) ? MIN_FONT_SIZE : fontSize - 1;
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
    protected synchronized void paintComponent(Graphics graphics) {
        applyHighlight();
        if (isScrollingActive) {
            truncate();
            setCaretPosition(getDocument().getLength());
        }

        var graphics2d = (Graphics2D) graphics.create();
        graphics2d.setColor(getBackground());
        graphics2d.fillRect(0, 0, getWidth(), getHeight());

        super.paintComponent(graphics2d);
        logo.paintLogo(graphics2d);
        graphics2d.dispose();
    }

    public void truncate() {
        var numLinesToTruncate = this.getLineCount() - MAX_LINES - 1;
        if (numLinesToTruncate > 0) {
            try {
                var posOfLastLineToTrunk = this.getLineEndOffset(numLinesToTruncate - 1);
                this.replaceRange("", 0, posOfLastLineToTrunk);
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

        var line = text.endsWith(CRLF) ? text : String.format("%s%n", text);
        linesBuffer[linesBufferIndex] = line;
        linesBufferIndex = (linesBufferIndex + 1) % MAX_LINES;

        if (isPrettyLoggingActive) {
            var matcher = prettyLoggingPattern.matcher(line);
            if (matcher.find()) {
                var tmp = String.format("%-20s | %s | %s%n", matcher.group(1), matcher.group(2), matcher.group(4));
                line = tmp;
            }
        }

        if (isHighlightActive && isFilterActive) {
            try {
                if (highlightPattern.matcher(line).find()) {
                    super.append(String.format("%s%s", FILTER_PREFIX, line));
                }
            } catch (PatternSyntaxException pse) {
                super.append(String.format("%s%s", FILTER_PREFIX, line));
            }
        } else {
            super.append(line);
        }
        // repaint();
    }

    public void clear() {
        setText("");
        for (int i = 0; i < MAX_LINES; i++) {
            this.linesBuffer[i] = EMPTY_LINE;
        }
        linesBufferIndex = 0;
    }

    private void refresh() {
        var caretPosition = this.getCaretPosition();
        setText("");
        String line;
        for (int i = 0; i < MAX_LINES; i++) {
            var j = (i + linesBufferIndex) % MAX_LINES;
            line = this.linesBuffer[j];
            this.linesBuffer[j] = EMPTY_LINE;
            this.tmpLinesBuffer[i] = line;
        }
        linesBufferIndex = 0;
        for (int i = 0; i < MAX_LINES; i++) {
            append(this.tmpLinesBuffer[i]);
        }
        this.setCaretPosition(caretPosition);
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        // repaint();
    }

    // ----------------------------------------------------------------------------------------
    // pretty logging
    // ----------------------------------------------------------------------------------------
    private boolean isPrettyLoggingActive = false;
    private Pattern prettyLoggingPattern = Pattern.compile("(.*) (.*) \\[(.*)\\] (.*)");

    // ----------------------------------------------------------------------------------------
    // scrolling
    // ----------------------------------------------------------------------------------------
    private boolean isScrollingActive = true;

    @Override
    public boolean isScrollingActive() {
        return isScrollingActive;
    }

    @Override
    public void startScrolling() {
        isScrollingActive = true;
        // repaint();
    }

    @Override
    public void stopScrolling() {
        isScrollingActive = false;
        // repaint();
    }

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
        if (isFilterActive) {
            isFilterActive = false;
            refresh();
        } else {
            if (isHighlightActive) {
                isFilterActive = true;
                refresh();
            }
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
        var highliter = getHighlighter();
        highliter.removeAllHighlights();
        if (!isHighlightActive) {
            return;
        }
        var matcher = highlightPattern.matcher(getText());
        while (matcher.find()) {
            try {
                highliter.addHighlight(matcher.start(), matcher.end(), painter);
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
