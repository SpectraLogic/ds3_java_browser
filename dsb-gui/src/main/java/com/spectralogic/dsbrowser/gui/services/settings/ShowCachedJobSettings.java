package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ShowCachedJobSettings {
    public static final ShowCachedJobSettings DEFAULT = createDefault();

    public static ShowCachedJobSettings createDefault() {
        return new ShowCachedJobSettings(Boolean.TRUE);
    }

    @JsonProperty("showCachedJob")
    private final BooleanProperty showCachedJob = new SimpleBooleanProperty();

    public ShowCachedJobSettings(final boolean showCachedJob) {
        this.showCachedJob.set(showCachedJob);
    }

    public ShowCachedJobSettings() {
        // pass
    }

    public ShowCachedJobSettings copy() {
        final ShowCachedJobSettings settings = new ShowCachedJobSettings();
        settings.setShowCachedJob(this.getShowCachedJob());
        return settings;
    }

    public Boolean getShowCachedJob() {
        return showCachedJob.get();
    }

    public BooleanProperty filePropertiesEnableProperty() {
        return showCachedJob;
    }

    public void setShowCachedJob(final boolean filePropertiesEnable) {
        this.showCachedJob.set(filePropertiesEnable);
    }

    public void overwrite(final boolean settings) {
        this.setShowCachedJob(settings);
    }

}


