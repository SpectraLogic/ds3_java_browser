package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class WindowSetting {

    public static final WindowSetting DEFAULT = createDefault();


    private static WindowSetting createDefault() {
        return new WindowSetting(600.0, 1000.0);
    }

    @JsonProperty("height")
    private final DoubleProperty height = new SimpleDoubleProperty();

    @JsonProperty("width")
    private final DoubleProperty width = new SimpleDoubleProperty();

    public WindowSetting(final double height, final double width) {
        this.height.set(height);
        this.width.set(width);
    }


    public WindowSetting() {
        // pass
    }

    public double getHeight() {
        return height.get();
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    public void setHeight(final double height) {
        this.height.set(height);
    }

    public double getWidth() {
        return width.get();
    }

    public DoubleProperty widthProperty() {
        return width;
    }

    public void setWidth(final double width) {
        this.width.set(width);
    }

    public WindowSetting copy() {
        final WindowSetting settings = new WindowSetting();
        settings.setHeight(this.getHeight());
        settings.setWidth(this.getWidth());
        return settings;
    }

    public void overwrite(final WindowSetting settings) {
        this.setHeight(settings.getHeight());
        this.setWidth(settings.getWidth());
    }
}
