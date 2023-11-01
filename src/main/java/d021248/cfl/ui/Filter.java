package d021248.cfl.ui;

interface Filter {
    void setFilterText(String text);

    String getFilterText();

    void startFilter();

    void stopFilter();

    boolean isFilterActive();
}
