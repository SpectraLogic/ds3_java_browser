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

    public BooleanProperty showCachedJobEnableProperty() {
        return showCachedJob;
    }

    public void setShowCachedJob(final boolean showCachedJob) {
        this.showCachedJob.set(showCachedJob);
    }

    public void overwrite(final boolean settings) {
        this.setShowCachedJob(settings);
    }

}


