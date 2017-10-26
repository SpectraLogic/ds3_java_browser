/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services.settings;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.beans.property.*;

import java.nio.file.Paths;

public class LogSettings {

    public static final LogSettings DEFAULT = createDefault();

    @JsonProperty("logLocation")
    private final StringProperty logLocation = new SimpleStringProperty();
    @JsonProperty("logSize")
    private final IntegerProperty logSize = new SimpleIntegerProperty();
    @JsonProperty("numRollovers")
    private final IntegerProperty numRollovers = new SimpleIntegerProperty();
    @JsonProperty("debugLogging")
    private final BooleanProperty debugLogging = new SimpleBooleanProperty();
    @JsonProperty("consoleLogging")
    private final BooleanProperty consoleLogging = new SimpleBooleanProperty();

    public LogSettings(final String logLocation,
                       final int logSize,
                       final int numRollovers,
                       final boolean debugLogging,
                       final boolean consoleLogging) {
        this.logLocation.set(logLocation);
        this.logSize.set(logSize);
        this.numRollovers.set(numRollovers);
        this.debugLogging.set(debugLogging);
        this.consoleLogging.set(consoleLogging);
    }

    public LogSettings() {
        // pass
    }

    private static LogSettings createDefault() {
        final String logPath = Paths.get(System.getProperty(StringConstants.SETTING_FILE_PATH),
                StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.LOG).toString();
        return new LogSettings(logPath, 1, 10, true, false);
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

    public LogSettings copy() {
        final LogSettings settings = new LogSettings();
        settings.setLogLocation(this.getLogLocation());
        settings.setLogSize(this.getLogSize());
        settings.setNumRollovers(this.getNumRollovers());
        settings.setDebugLogging(this.getDebugLogging());
        settings.setConsoleLogging(this.getConsoleLogging());
        return settings;
    }

    public void overwrite(final LogSettings settings) {
        this.setLogLocation(settings.getLogLocation());
        this.setLogSize(settings.getLogSize());
        this.setNumRollovers(settings.getNumRollovers());
        this.setDebugLogging(settings.getDebugLogging());
        this.setConsoleLogging(settings.getConsoleLogging());
    }

    public boolean getDebugLogging() {
        return debugLogging.get();
    }

    public void setDebugLogging(final boolean debugLogging) {
        this.debugLogging.set(debugLogging);
    }

    public BooleanProperty debugLoggingProperty() {
        return debugLogging;
    }

    public boolean getConsoleLogging() {
        return consoleLogging.get();
    }

    public void setConsoleLogging(final boolean consoleLogging) {
        this.consoleLogging.set(consoleLogging);
    }

}
