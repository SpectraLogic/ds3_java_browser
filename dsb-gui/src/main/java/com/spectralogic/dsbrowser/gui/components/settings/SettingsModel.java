/*
 * ****************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */

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
