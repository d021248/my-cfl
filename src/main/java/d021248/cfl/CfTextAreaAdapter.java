package d021248.cfl;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

public class CfTextAreaAdapter {

    private final CfTextArea textArea;

    CfTextAreaAdapter(CfTextArea textArea) {
        this.textArea = textArea;
    }

    private boolean isControlKeyDown = false;
    private boolean isAltKeyDown = false;
    private boolean isScrollingOn = false;

    public void init() {
        var keyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                isControlKeyDown = e.isControlDown();
                isAltKeyDown = e.isAltDown();
            }

            @Override
            public void keyReleased(KeyEvent e) {
                isControlKeyDown = e.isControlDown();
                isAltKeyDown = e.isAltDown();
            }
        };
        textArea.addKeyListener(keyAdapter);

        var mouseAdapter = new MouseAdapter() {
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
                // setHighlight.apply(getSelectedText(), true);
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
                // toggleScrollButton.setText("start auto-scroll");
                textArea.setScrollingOn(false);
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
                        // setHighlight.apply("", true);
                    } else {
                        // setHighlight.apply(selectedWord, true);
                    }
                } catch (BadLocationException e1) {}
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (isControlKeyDown) {
                    if (e.getWheelRotation() < 0) {
                        textArea.decreaseFontSize();
                    } else {
                        textArea.increaseFontSize();
                    }
                }
                if (isAltKeyDown) {
                    textArea.changeFont();
                }

                super.mouseWheelMoved(e);
            }
        };

        textArea.addMouseWheelListener(mouseAdapter);
        textArea.addMouseMotionListener(mouseAdapter);
        textArea.addMouseListener(mouseAdapter);
    }
}
