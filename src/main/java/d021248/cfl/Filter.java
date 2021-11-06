package d021248.cfl;

public interface Filter {
    public void setFilterText(String text);

    public String getFilterText();

    public void startFilter();

    public void stopFilter();

    public boolean isFilterActive();
}
