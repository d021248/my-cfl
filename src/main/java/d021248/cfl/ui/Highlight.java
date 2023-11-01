package d021248.cfl.ui;

interface Highlight {
    void setHighlightText(String text);

    String getHighlightText();

    void applyHighlight();

    void removeHighlight();

    boolean isHighlightActive();
}
