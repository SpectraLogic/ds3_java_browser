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

package com.spectralogic.dsbrowser.gui.services.jobprioritystore;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.file.Paths;

public class JobSettings {

    public static final JobSettings DEFAULT = createDefault();

    private static JobSettings createDefault() {
        final String logPath = Paths.get(System.getProperty("user.home"), ".dsbrowser", "log").toString();
        return new JobSettings("Data Policy Default (no change)", "Data Policy Default (no change)", false);
    }

    @JsonProperty("getJobPriority")
    private String getJobPriority;
    @JsonProperty("putJobPriority")
    private String putJobPriority;


    public JobSettings(final String getJobPriority, final String putJobPriority, final boolean isDefaultForAll) {
        this.putJobPriority = putJobPriority;
        this.getJobPriority = getJobPriority;
    }

    public JobSettings() {
        this(null, null, false);
    }

    public void setGetJobPriority(final String getJobPriority) {
        this.getJobPriority = getJobPriority;
    }

    public void setPutJobPriority(final String putJobPriority) {
        this.putJobPriority = putJobPriority;
    }

    public String getGetJobPriority() {
        return getJobPriority;
    }

    public String getPutJobPriority() {
        return putJobPriority;
    }

    public void overwrite(final JobSettings settings) {
        this.setPutJobPriority(settings.getPutJobPriority());
        this.setGetJobPriority(settings.getGetJobPriority());
    }
}
