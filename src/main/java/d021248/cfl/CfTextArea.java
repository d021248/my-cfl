package d021248.cfl;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
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
    private static final String LOGO = "D021248.png";

    private int fontSize = 11;

    private final DefaultHighlightPainter defaultHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(
            null);

    private final CfLogo logo = new CfLogo(this.getClass().getResource(LOGO).toString(), this);

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
    }

    private static final long serialVersionUID = 1L;

    public CfTextArea() {
        super();
        setHighlighter(new DefaultHighlighter());
        setOpaque(false);

        // setFont(new Font("Verdana", Font.PLAIN, 10));
        setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        // setFont(new Font("Courier", Font.PLAIN, 11));
        // setFont(new Font("Helvetica", Font.PLAIN, 11));
        // setFont(new Font("Arial", Font.PLAIN, 12));
    }

    public void increaseFont() {
        if (++fontSize > MAX_FONT_SIZE) {
            fontSize = MAX_FONT_SIZE;
        } else {
            setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
            repaint();
        }
    }

    public void decreaseFont() {
        if (--fontSize < MIN_FONT_SIZE) {
            fontSize = MIN_FONT_SIZE;
        } else {
            setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
            repaint();
        }
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        highlight();
        if (isScrollingOn) {
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
        int numLinesToTrunk = this.getLineCount() - MAX_LINES - 1;
        if (numLinesToTrunk > 0) {
            try {
                int posOfLastLineToTrunk = this.getLineEndOffset(numLinesToTrunk - 1);
                this.replaceRange("", 0, posOfLastLineToTrunk);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
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

    private void printLines(String[] lines) {
        textBuffer = new StringBuilder(MAX_LINES);
        setText("");
        for (String text : lines) {
            appendLine(text);
        }
    }

    public void appendLine(String text) {
        if (text == null || text.length() == 0) {
            return;
        }

        String line = text.endsWith(CRLF) ? text : text + CRLF;
        if (isHighlightOn && isFilterOn) {
            try {
                if (text.matches(lineHighlightValue)) {
                    append(FILTER_PREFIX + line);
                }
            } catch (PatternSyntaxException pse) {
                append(FILTER_PREFIX + line);
            }
        } else {
            append(line);
        }
        textBuffer.append(line);
        repaint();
    }

    public void clear() {
        setText("");
        textBuffer.setLength(0);
    }

    public void highlight() {
        Highlighter highliter = getHighlighter();
        highliter.removeAllHighlights();
        if (!isHighlightOn) {
            return;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(highlightValue));
        String text = getText();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            try {
                highliter.addHighlight(matcher.start(), matcher.end(), defaultHighlightPainter);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setText(String t) {
        super.setText(t);
        repaint();
    }

    // ----------------------------------------------------------------------------------------
    // filtering
    // ----------------------------------------------------------------------------------------
    private static final String FILTER_PREFIX = "]";
    private StringBuilder textBuffer = new StringBuilder(MAX_LINES);
    private String highlightValue = null;
    private String lineHighlightValue = null;
    private boolean isHighlightOn = false;
    private boolean isFilterOn = false;

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
        lineHighlightValue = ".*" + highlightValue + ".*";
        printLines(textBuffer.toString().split(CRLF));
    }

    public void setFilter(boolean setFilterOn) {
        if (isFilterOn && !setFilterOn) {
            isFilterOn = false;
            printLines(textBuffer.toString().split(CRLF));
            return;
        }

        if (!isFilterOn && setFilterOn && isHighlightOn) {
            isFilterOn = true;
            printLines(textBuffer.toString().split(CRLF));
            return;
        }
    }

    public void unsetHighlight() {
        getHighlighter().removeAllHighlights();
        isHighlightOn = false;
        printLines(textBuffer.toString().split(CRLF));
    }
}
