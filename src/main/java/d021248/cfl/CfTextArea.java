package d021248.cfl;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
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
import javax.swing.text.Utilities;

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

    private final CfTextAreaAdapter adapter;

    public CfTextArea() {
        super();
        setHighlighter(new DefaultHighlighter());
        setOpaque(false);
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));
        this.adapter = new CfTextAreaAdapter(this);
    }

    // ----------------------------------------------------------------------------------------
    // font
    // ----------------------------------------------------------------------------------------
    public synchronized void increaseFontSize() {
        fontSize = (fontSize >= MAX_FONT_SIZE) ? MAX_FONT_SIZE : fontSize + 1;
        setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
        repaint();
    }

    public synchronized void decreaseFontSize() {
        fontSize = (fontSize <= MIN_FONT_SIZE) ? MIN_FONT_SIZE : fontSize - 1;
        setFont(new Font(getFont().getName(), Font.PLAIN, fontSize));
        repaint();
    }

    public synchronized void changeFont() {
        fontNameIndex = ++fontNameIndex % FONT_NAMES.size();
        setFont(new Font(FONT_NAMES.get(fontNameIndex), Font.PLAIN, fontSize));
        repaint();
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());

        super.paintComponent(g2d);
        logo.paintLogo(g2d);
        g2d.dispose();
    }

    @Override
    public void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        var line = text.endsWith(CRLF) ? text : String.format("%s%n", text);
        linesBuffer.add(line);
        super.append(line);
        repaint();
    }

    private final DefaultHighlightPainter defaultHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(
        null
    );

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
    }

    public Object clear() {
        return null;
    }

    public boolean isScrollingOn() {
        return false;
    }

    public void setScrollingOn(boolean b) {}

    public boolean isHighlightOn() {
        return false;
    }

    public void unsetHighlight() {}

    public void setHighlighter(Highlighter h) {}

    public void setFilter(boolean b) {}

    public boolean isFilterOn() {
        return false;
    }

    public void setHighlight(String s) {}
}
