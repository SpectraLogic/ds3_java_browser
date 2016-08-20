package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class  ProcessSettings {

    public static final ProcessSettings DEFAULT = createDefault();

    private static ProcessSettings createDefault() {
        return new ProcessSettings(10);
    }

    @JsonProperty("maximumNumberOfParallelThreads")
    private final IntegerProperty maximumNumberOfParallelThreads = new SimpleIntegerProperty();

    public ProcessSettings(final int maximumNumberOfParallelThreads) {
        this.maximumNumberOfParallelThreads.set(maximumNumberOfParallelThreads);
    }

    public ProcessSettings() {
        // pass
    }

    public ProcessSettings copy() {
        final ProcessSettings settings = new ProcessSettings();
        settings.setMaximumNumberOfParallelThreads(this.getMaximumNumberOfParallelThreads());
        return settings;
    }

    public int getMaximumNumberOfParallelThreads() {
        return maximumNumberOfParallelThreads.get();
    }

    public IntegerProperty maximumNumberOfParallelThreadsProperty() {
        return maximumNumberOfParallelThreads;
    }

    public void setMaximumNumberOfParallelThreads(final int maximumNumberOfParallelThreads) {
        this.maximumNumberOfParallelThreads.set(maximumNumberOfParallelThreads);
    }

    public void overwrite(final ProcessSettings settings) {
        this.setMaximumNumberOfParallelThreads(settings.getMaximumNumberOfParallelThreads());
    }


}
