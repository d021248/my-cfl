package d021248.cfl;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextArea;
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
        if (text == null || text.isBlank()) {
            return;
        }

        var line = text.endsWith(CRLF) ? text : String.format("%s%n", text);
        linesBuffer.add(line);
        super.append(line);
        repaint();
    }

    public void clear() {
        linesBuffer.clear();
        setText("");
    }

    private final DefaultHighlightPainter defaultHighlightPainter = new DefaultHighlighter.DefaultHighlightPainter(
        null
    );

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        repaint();
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
