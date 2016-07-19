package com.spectralogic.dsbrowser.gui.components.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SettingsModel {
    @JsonProperty("logLocation")
    private final StringProperty logLocation = new SimpleStringProperty();
    @JsonProperty("logSize")
    private final IntegerProperty logSize = new SimpleIntegerProperty();
    @JsonProperty("numRollovers")
    private final IntegerProperty numRollovers = new SimpleIntegerProperty();

    public SettingsModel() {
        // pass
    }

    public String getLogLocation() {
        return logLocation.get();
    }

    public void setLogLocation(final String logLocation) {
        this.logLocation.set(logLocation);
    }

    public StringProperty logLocationProperty() {
        return logLocation;
    }

    public int getLogSize() {
        return logSize.get();
    }

    public void setLogSize(final int logSize) {
        this.logSize.set(logSize);
    }

    public IntegerProperty logSizeProperty() {
        return logSize;
    }

    public int getNumRollovers() {
        return numRollovers.get();
    }

    public void setNumRollovers(final int numRollovers) {
        this.numRollovers.set(numRollovers);
    }

    public IntegerProperty numRolloversProperty() {
        return numRollovers;
    }
}
