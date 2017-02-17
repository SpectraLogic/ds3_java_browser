package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.Constants;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ProcessSettings {

    public static final ProcessSettings DEFAULT = createDefault();

    public static ProcessSettings createDefault() {
        return new ProcessSettings(Constants.MAX_PARALLEL_THREAD_DEFAULT);
    }

    @JsonProperty("maximumNumberOfParallelThreads")
    private final IntegerProperty maximumNumberOfParallelThreads = new SimpleIntegerProperty();

    public ProcessSettings(final int maximumNumberOfParallelThreads) {
        this.maximumNumberOfParallelThreads.set(maximumNumberOfParallelThreads);
    }

    public ProcessSettings() {
        //Default constructor needed
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
