/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class FilePropertiesSettings {

    public static final FilePropertiesSettings DEFAULT = createDefault();

    private static FilePropertiesSettings createDefault() {
        return new FilePropertiesSettings(Boolean.FALSE);
    }


    private final BooleanProperty filePropertiesEnable = new SimpleBooleanProperty();

    public FilePropertiesSettings(final boolean filePropertiesEnable) {
        this.filePropertiesEnable.set(filePropertiesEnable);
    }

    public FilePropertiesSettings() {
        // pass
    }

    public FilePropertiesSettings copy() {
        final FilePropertiesSettings settings = new FilePropertiesSettings();
        settings.setFilePropertiesEnable(this.isFilePropertiesEnabled());
        return settings;
    }

    @JsonProperty("filePropertiesEnable")
    public Boolean isFilePropertiesEnabled() {
        return filePropertiesEnable.get();
    }

    public BooleanProperty filePropertiesEnableProperty() {
        return filePropertiesEnable;
    }

    public void setFilePropertiesEnable(final boolean filePropertiesEnable) {
        this.filePropertiesEnable.set(filePropertiesEnable);
    }

    public void overwrite(final boolean settings) {
        this.setFilePropertiesEnable(settings);
    }

}
