package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ShowCachedJobSettings {

    public ShowCachedJobSettings() {
        //Default constructor needed
    }

    public static final ShowCachedJobSettings DEFAULT = createDefault();

    public static ShowCachedJobSettings createDefault() {
        return new ShowCachedJobSettings(Boolean.TRUE);
    }

    @JsonProperty("showCachedJob")
    private final BooleanProperty showCachedJob = new SimpleBooleanProperty();

    public ShowCachedJobSettings(final boolean showCachedJob) {
        this.showCachedJob.set(showCachedJob);
    }

    public Boolean getShowCachedJob() {
        return showCachedJob.get();
    }

    public BooleanProperty getShowCachedJobEnableProperty() {
        return showCachedJob;
    }

    public void setShowCachedJob(final boolean showCachedJob) {
        this.showCachedJob.set(showCachedJob);
    }

    public void overwrite(final boolean settings) {
        this.setShowCachedJob(settings);
    }

}


