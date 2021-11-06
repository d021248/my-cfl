package d021248.cfl;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

class KeyAndMouseAdapter {

    private final CfTextArea textArea;

    KeyAndMouseAdapter(CfTextArea textArea) {
        this.textArea = textArea;
        this.textArea.addKeyListener(keyAdapter);
        this.textArea.addMouseWheelListener(mouseAdapter);
        this.textArea.addMouseMotionListener(mouseAdapter);
        this.textArea.addMouseListener(mouseAdapter);
    }

    private boolean isControlKeyDown = false;
    private boolean isAltKeyDown = false;
    private boolean isScrollingOn = false;

    private KeyAdapter keyAdapter = new KeyAdapter() {
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

    private MouseAdapter mouseAdapter = new MouseAdapter() {
        boolean isDraggedOn = false;
        int start = 0;
        int end = 0;
        int pos = 0;

        @Override
        public void mouseDragged(MouseEvent e) {
            if (textArea.isFilterActive()) {
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
            textArea.setHighlightText(textArea.getSelectedText());
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
                var rowStart = Utilities.getRowStart(textArea, offset);
                var rowEnd = Utilities.getRowEnd(textArea, offset);
                var xoffset = offset - rowStart;
                textArea.setCaretPosition(xoffset);
                var selectedLine = textArea.getText().substring(rowStart, rowEnd);

                // ------------------------------------------------------------------
                // 2. find the selected word
                // ------------------------------------------------------------------
                String selectedWord = null;
                var ws = 0; // word start position
                var we = 0; // word end position
                for (var i = 0; i < selectedLine.length(); i++) {
                    var c = selectedLine.charAt(i);

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
                if (textArea.isHighlightActive()) {
                    textArea.stopFilter();
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
}
