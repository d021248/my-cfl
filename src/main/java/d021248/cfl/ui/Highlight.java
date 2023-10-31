package d021248.cfl.ui;

interface Highlight {
    public void setHighlightText(String text);

    public String getHighlightText();

    public void applyHighlight();

    public void removeHighlight();

    public boolean isHighlightActive();
}
