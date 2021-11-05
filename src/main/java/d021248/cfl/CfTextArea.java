package d021248.cfl;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;

class CfTextArea extends JTextArea implements AdjustmentListener {

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

    private final CfTextAreaKeyAndMouseAdapter adapter;

    public CfTextArea() {
        super();
        setHighlighter(new DefaultHighlighter());
        setOpaque(false);
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));
        this.adapter = new CfTextAreaKeyAndMouseAdapter(this);
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
        highlight();
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

        if (isHighlightOn && isFilterOn) {
            try {
                if (line.matches(highlightValueRegexp)) {
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
    private String highlightValue = null;
    private String highlightValueRegexp = null;
    private boolean isHighlightOn = false;
    private boolean isFilterOn = false;

    public void highlight() {
        var highliter = getHighlighter();
        highliter.removeAllHighlights();
        if (!isHighlightOn) {
            return;
        }
        var pattern = Pattern.compile(Pattern.quote(highlightValue));
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

    public boolean isHighlightOn() {
        return isHighlightOn;
    }

    public boolean isFilterOn() {
        return isFilterOn;
    }

    public void setHighlight(String newHighlightValue) {
        if (newHighlightValue == null) {
            return;
        }
        isHighlightOn = true;
        highlightValue = newHighlightValue.trim();
        highlightValueRegexp = String.format(".*%s.*", highlightValue);
        refresh();
    }

    public void setFilter(boolean setFilterOn) {
        if (isFilterOn && !setFilterOn) {
            isFilterOn = false;
            refresh();
            return;
        }

        if (!isFilterOn && setFilterOn && isHighlightOn) {
            isFilterOn = true;
            refresh();
            return;
        }
    }

    public void clearHighlight() {
        getHighlighter().removeAllHighlights();
        isHighlightOn = false;
        refresh();
    }
}
