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
