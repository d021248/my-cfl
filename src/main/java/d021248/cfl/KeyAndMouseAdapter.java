package d021248.cfl;

import d021248.cfl.cmd.Shell;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.BiFunction;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

class KeyAndMouseAdapter {

    private CfLoggerUI loggerUI;
    private CfTextArea textArea;

    public KeyAndMouseAdapter(CfLoggerUI loggerUI) {
        this.loggerUI = loggerUI;
        this.loggerUI.filterValueTextField.addKeyListener(cfLoggerUIKeyAdapter);
        this.loggerUI.textAreaScrollPane.addMouseWheelListener(textAreaMouseAdapter);

        this.textArea = loggerUI.textArea;
        this.textArea.addKeyListener(textAreaKeyAdapter);
        this.textArea.addMouseMotionListener(textAreaMouseAdapter);
        this.textArea.addMouseListener(textAreaMouseAdapter);
    }

    private BiFunction<String, Boolean, String> applyHighlight = (filterValue, isUpdateTextFieldRequested) -> {
        if (filterValue == null || filterValue.isBlank()) {
            textArea.removeHighlight();
        } else {
            textArea.setHighlightText(filterValue);
            textArea.applyHighlight();
        }

        if (Boolean.TRUE.equals(isUpdateTextFieldRequested)) {
            loggerUI.filterValueTextField.setText(filterValue);
        }

        this.loggerUI.toggleFilterButton.setEnabled(!this.loggerUI.filterValueTextField.getText().isEmpty());
        this.loggerUI.toggleFilterButton.setText(
                this.loggerUI.toggleFilterButton.isEnabled() ? CfLoggerUI.BT_FILTER_ON : CfLoggerUI.BT_FILTER_OFF);

        // copy to clipboard
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(filterValue), null);
        return filterValue;
    };

    private boolean isControlKeyDown = false;
    private boolean isAltKeyDown = false;

    private KeyAdapter textAreaKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            isControlKeyDown = e.isControlDown();
            isAltKeyDown = e.isAltDown();
        }

        @Override
        public void keyReleased(KeyEvent e) {
            isAltKeyDown = e.isAltDown();
            isControlKeyDown = e.isControlDown();
        }
    };

    private MouseAdapter textAreaMouseAdapter = new MouseAdapter() {
        boolean isDraggedOn = false;
        int start = 0;
        int end = 0;
        int pos = 0;

        @Override
        public void mouseDragged(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());

            textArea.stopScrolling(); // TODO: update Button

            if (textArea.isFilterActive()) {
                return;
            }

            if (!isDraggedOn) {
                start = pos;
                end = pos;
                isDraggedOn = true;
            } else {
                end = pos;
            }

            textArea.setCaretPosition(pos);
            textArea.setSelectionStart(start < end ? start : end);
            textArea.setSelectionEnd(start < end ? end : start);
            textArea.setHighlightText(textArea.getSelectedText());
            applyHighlight.apply(textArea.getHighlightText(), true);
            textArea.repaint();
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            if (isDraggedOn) {
                isDraggedOn = false;
            }
            textArea.repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.setPos(pos);

            textArea.stopScrolling(); // TODO: update Button
            textArea.removeHighlight();
            // isDraggedOn = !isDraggedOn;
            loggerUI.toggleScrollButton.setText("start auto-scroll");
            textArea.stopScrolling();
            super.mousePressed(e);
            textArea.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.setPos(pos);
            super.mouseReleased(e);
            textArea.repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.setPos(pos);
            textArea.stopScrolling(); // TODO: update Button
            if (e.getButton() != MouseEvent.BUTTON1) {
                return;
            }

            if (e.getClickCount() != 2) {
                return;
            }
            if (e.isConsumed()) {
                return;
            }

            // ------------------------------------------------------------------
            // find the selected word
            // ------------------------------------------------------------------
            var cursorPos = textArea.viewToModel2D(e.getPoint());
            try {
                // ------------------------------------------------------------------
                // 1. find the selected line
                // ------------------------------------------------------------------
                var rowStartPos = Utilities.getRowStart(textArea, cursorPos);
                var rowEndPos = Utilities.getRowEnd(textArea, cursorPos);
                var rowCursorPos = cursorPos - rowStartPos;

                var selectedLine = textArea.getText().substring(rowStartPos, rowEndPos);

                // ------------------------------------------------------------------
                // 2. find the selected word
                // ------------------------------------------------------------------
                String selectedWord = null;

                var selectedWordStartPos = rowCursorPos;
                while (selectedWordStartPos > 0 && !Character.isWhitespace(selectedLine.charAt(selectedWordStartPos))) {
                    selectedWordStartPos--;
                }

                var selectedWordEndPos = rowCursorPos;
                while (!Character.isWhitespace(selectedLine.charAt(selectedWordEndPos))) {
                    selectedWordEndPos++;
                }

                // ------------------------------------------------------------------
                // 3. cut out the selected word
                // ------------------------------------------------------------------
                if (selectedWordEndPos >= selectedWordStartPos) {
                    selectedWord = selectedLine.substring(selectedWordStartPos, selectedWordEndPos).trim();
                }

                // ------------------------------------------------------------------
                // 4. toggle filter on/off
                // ------------------------------------------------------------------
                if (textArea.isHighlightActive()) {
                    textArea.stopFilter();
                    applyHighlight.apply("", true);
                } else {
                    applyHighlight.apply(selectedWord, true);
                }
            } catch (BadLocationException e1) {
            }
            textArea.repaint();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.setPos(pos);
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
            textArea.repaint();
        }
    };

    private KeyAdapter cfLoggerUIKeyAdapter = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent keyEvent) {
            var filterValue = String.format("%s%s", loggerUI.filterValueTextField.getText(),
                    toPrintableChar(keyEvent.getKeyChar()));
            if (!filterValue.startsWith(">")) {
                KeyAndMouseAdapter.this.applyHighlight.apply(filterValue, false);
            }
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            var filterValue = String.format("%s%s", loggerUI.filterValueTextField.getText(),
                    toPrintableChar(keyEvent.getKeyChar()));
            if (filterValue.startsWith(">")) {
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    var command = String.format("CMD /C %s", filterValue.substring(1).trim());
                    new Thread(Shell.cmd(command.split(" ")).stdoutConsumer(loggerUI::logger)).start();
                    KeyAndMouseAdapter.this.applyHighlight.apply("", false);
                    loggerUI.filterValueTextField.setText("");
                }
            }
        }

        public String toPrintableChar(char c) {
            return isPrintableChar(c) ? String.valueOf(c) : "";
        }

        public boolean isPrintableChar(char c) {
            var block = Character.UnicodeBlock.of(c);
            return ((!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null
                    && block != Character.UnicodeBlock.SPECIALS);
        }
    };
}
