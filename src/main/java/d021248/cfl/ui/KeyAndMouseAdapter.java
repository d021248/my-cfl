package d021248.cfl.ui;

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

import d021248.cfl.cmd.Shell;

class KeyAndMouseAdapter {

    private CfLoggerUI loggerUI;
    private CfTextArea textArea;
    private boolean isControlKeyDown = false;
    private boolean isAltKeyDown = false;

    public KeyAndMouseAdapter(CfLoggerUI loggerUI) {
        this.loggerUI = loggerUI;
        this.textArea = loggerUI.textArea;
        initializeListeners();
    }

    private void initializeListeners() {
        loggerUI.filterValueTextField.addKeyListener(cfLoggerUIKeyAdapter);
        loggerUI.textAreaScrollPane.addMouseWheelListener(textAreaMouseAdapter);
        textArea.addKeyListener(textAreaKeyAdapter);
        textArea.addMouseMotionListener(textAreaMouseAdapter);
        textArea.addMouseListener(textAreaMouseAdapter);
    }

    private BiFunction<String, Boolean, String> applyHighlight = (filterValue, isUpdateTextFieldRequested) -> {
        if (filterValue == null || filterValue.isBlank()) {
            textArea.removeHighlight();
        } else {
            textArea.setHighlightText(filterValue);
            textArea.applyHighlight();
        }

        if (Boolean.TRUE.equals(isUpdateTextFieldRequested)) {
            updateTextField(filterValue);
        }

        toggleFilterButton();
        copyToClipboard(filterValue);
        return filterValue;
    };

    private void updateTextField(String filterValue) {
        loggerUI.filterValueTextField.setText(filterValue);
    }

    private void toggleFilterButton() {
        loggerUI.toggleFilterButton.setEnabled(!loggerUI.filterValueTextField.getText().isEmpty());
        loggerUI.toggleFilterButton.setText(
                loggerUI.toggleFilterButton.isEnabled() ? CfLoggerUI.BT_FILTER_ON : CfLoggerUI.BT_FILTER_OFF);
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private KeyAdapter textAreaKeyAdapter = new KeyAdapter() {
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

    private MouseAdapter textAreaMouseAdapter = new MouseAdapter() {
        boolean isDraggedOn = false;
        int start = 0;
        int end = 0;
        int pos = 0;

        @Override
        public void mouseDragged(MouseEvent e) {
            handleMouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (isDraggedOn) {
                isDraggedOn = false;
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            handleMousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            super.mouseReleased(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            handleMouseClicked(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            handleMouseWheelMoved(e);
        }

        private void handleMouseDragged(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.stopScrolling();

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

            textArea.setSelectionStart(Math.min(start, end));
            textArea.setSelectionEnd(Math.max(start, end));
            textArea.setHighlightText(textArea.getSelectedText());
            applyHighlight.apply(textArea.getHighlightText(), true);
        }

        private void handleMousePressed(MouseEvent e) {
            textArea.removeHighlight();
            loggerUI.toggleScrollButton.setText("start auto-scroll");
            textArea.stopScrolling();
            super.mousePressed(e);
        }

        private void handleMouseClicked(MouseEvent e) {
            pos = textArea.viewToModel2D(e.getPoint());
            textArea.stopScrolling();
            applyHighlight.apply("", true);

            if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 2 || e.isConsumed()) {
                return;
            }

            findAndToggleSelectedWord(e);
        }

        private void findAndToggleSelectedWord(MouseEvent e) {
            var cursorPos = textArea.viewToModel2D(e.getPoint());
            try {
                var rowStartPos = Utilities.getRowStart(textArea, cursorPos);
                var rowEndPos = Utilities.getRowEnd(textArea, cursorPos);
                var rowCursorPos = cursorPos - rowStartPos;
                var selectedLine = textArea.getText().substring(rowStartPos, rowEndPos);

                var selectedWord = findSelectedWord(selectedLine, rowCursorPos);

                if (textArea.isHighlightActive()) {
                    textArea.stopFilter();
                    applyHighlight.apply("", true);
                } else {
                    applyHighlight.apply(selectedWord, true);
                }
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }
        }

        private String findSelectedWord(String selectedLine, int rowCursorPos) {
            int selectedWordStartPos = rowCursorPos;
            while (selectedWordStartPos > 0 && !Character.isWhitespace(selectedLine.charAt(selectedWordStartPos))) {
                selectedWordStartPos--;
            }

            int selectedWordEndPos = rowCursorPos;
            while (selectedWordEndPos < selectedLine.length()
                    && !Character.isWhitespace(selectedLine.charAt(selectedWordEndPos))) {
                selectedWordEndPos++;
            }

            return selectedLine.substring(selectedWordStartPos, selectedWordEndPos).trim();
        }

        private void handleMouseWheelMoved(MouseWheelEvent e) {
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

    private KeyAdapter cfLoggerUIKeyAdapter = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent keyEvent) {
            handleKeyTyped(keyEvent);
        }

        @Override
        public void keyPressed(KeyEvent keyEvent) {
            handleKeyPressed(keyEvent);
        }

        private void handleKeyTyped(KeyEvent keyEvent) {
            var filterValue = String.format("%s%s", loggerUI.filterValueTextField.getText(),
                    toPrintableChar(keyEvent.getKeyChar()));
            if (!filterValue.startsWith(">")) {
                applyHighlight.apply(filterValue, false);
            }
        }

        private void handleKeyPressed(KeyEvent keyEvent) {
            var filterValue = String.format("%s%s", loggerUI.filterValueTextField.getText(),
                    toPrintableChar(keyEvent.getKeyChar()));
            if (filterValue.startsWith(">") && keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                var command = String.format("CMD /C %s", filterValue.substring(1).trim());
                Thread.ofVirtual().start(Shell.cmd(command.split(" ")).stdoutConsumer(loggerUI::logger));
                applyHighlight.apply("", false);
                loggerUI.filterValueTextField.setText("");
            }
        }

        private String toPrintableChar(char c) {
            return isPrintableChar(c) ? String.valueOf(c) : "";
        }

        private boolean isPrintableChar(char c) {
            var block = Character.UnicodeBlock.of(c);
            return ((!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null
                    && block != Character.UnicodeBlock.SPECIALS);
        }
    };
}