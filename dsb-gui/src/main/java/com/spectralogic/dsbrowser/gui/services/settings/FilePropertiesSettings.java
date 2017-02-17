package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class FilePropertiesSettings {

    public FilePropertiesSettings() {
        //Default constructor needed
    }

    public static final FilePropertiesSettings DEFAULT = createDefault();

    public static FilePropertiesSettings createDefault() {
        return new FilePropertiesSettings(Boolean.TRUE);
    }

    @JsonProperty("filePropertiesEnable")
    private final BooleanProperty filePropertiesEnable = new SimpleBooleanProperty();

    private FilePropertiesSettings(final boolean filePropertiesEnable) {
        this.filePropertiesEnable.set(filePropertiesEnable);
    }

    public Boolean getFilePropertiesEnable() {
        return filePropertiesEnable.get();
    }

    public BooleanProperty filePropertiesEnableProperty() {
        return filePropertiesEnable;
    }

    private void setFilePropertiesEnable(final boolean filePropertiesEnable) {
        this.filePropertiesEnable.set(filePropertiesEnable);
    }

    public void overwrite(final boolean settings) {
        this.setFilePropertiesEnable(settings);
    }

}
