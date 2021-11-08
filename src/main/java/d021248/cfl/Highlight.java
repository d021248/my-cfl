package d021248.cfl;

interface Highlight {
    public void setHighlightText(String text);

    public String getHighlightText();

    public void startHighlight();

    public void stopHighlight();

    public boolean isHighlightActive();
}
