package d021248.cfl;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

class CfTextArea extends JTextArea implements Highlight, Filter, AdjustmentListener {

    private static final String CRLF = System.getProperty("line.separator", "\n");
    private static final int MAX_LINES = 100_000;
    private static final int MIN_FONT_SIZE = 4;
    private static final int MAX_FONT_SIZE = 32;
    private static final List<String> FONT_NAMES = List.of("Arial", "Courier", "Helvetica", "Monospaced", "Plain");
    private static final String LOGO = "D021248.png";

    private static final long serialVersionUID = 1L;

    private int fontSize = 11;
    private int fontNameIndex = 3;

    private final List<String> linesBuffer = new ArrayList<>();

    private final CfLogo logo = new CfLogo(this.getClass().getResource(LOGO).toString(), this);

    public CfTextArea() {
        super();
        setHighlighter(new DefaultHighlighter());
        setOpaque(false);
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));
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

    @Override
    protected synchronized void paintComponent(Graphics graphics) {
        startHighlight();
        if (isScrollingOn) {
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
        linesBuffer.add(line);

        if (isHighlightActive && isFilterActive) {
            try {
                if (line.matches(highlightRegexp)) {
                    super.append(String.format("%s%s", FILTER_PREFIX, line));
                }
            } catch (PatternSyntaxException pse) {
                super.append(String.format("%s%s", FILTER_PREFIX, line));
            }
        } else {
            super.append(line);
        }

        repaint();
    }

    public void clear() {
        linesBuffer.clear();
        setText("");
    }

    private void refresh() {
        var tmp = new ArrayList<String>(linesBuffer);
        linesBuffer.clear();
        setText("");
        tmp.stream().forEach(this::append);
    }

    private final DefaultHighlightPainter defaultHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(
        null
    );

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
    }

    // ----------------------------------------------------------------------------------------
    // scrolling
    // ----------------------------------------------------------------------------------------
    private boolean isScrollingOn = true;

    public boolean isScrollingOn() {
        return isScrollingOn;
    }

    public void setScrolling(boolean isScrolling) {
        isScrollingOn = isScrolling;
        repaint();
    }

    // ----------------------------------------------------------------------------------------
    // filtering & highlighting
    // ----------------------------------------------------------------------------------------
    private static final String FILTER_PREFIX = "]";
    private String highlightText = null;
    private String highlightRegexp = null;
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
        highlightRegexp = String.format(".*%s.*", highlightText);
        refresh();
    }

    @Override
    public String getHighlightText() {
        return highlightText;
    }

    @Override
    public void startHighlight() {
        var highliter = getHighlighter();
        highliter.removeAllHighlights();
        if (!isHighlightActive) {
            return;
        }
        var pattern = Pattern.compile(Pattern.quote(highlightText));
        var text = getText();
        var matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                highliter.addHighlight(matcher.start(), matcher.end(), defaultHighlightPainter);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopHighlight() {
        getHighlighter().removeAllHighlights();
        isHighlightActive = false;
        refresh();
    }

    @Override
    public boolean isHighlightActive() {
        return isHighlightActive;
    }
}
