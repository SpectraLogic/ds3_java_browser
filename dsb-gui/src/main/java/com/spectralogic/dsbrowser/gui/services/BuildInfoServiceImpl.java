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

package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfoServiceImpl implements BuildInfoService{
    private static final Logger LOG = LoggerFactory.getLogger(BuildInfoServiceImpl.class);

    public static final String buildPropertiesFile = "ds3_cli.properties";

    private final String buildVersion;
    private final String buildDate;

    public BuildInfoServiceImpl() {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(buildPropertiesFile);
        final Properties buildProps = new Properties();
        try {
            buildProps.load(is);
        } catch (IOException e) {
            LOG.error("Failed to read build properties from {}: ", buildPropertiesFile, e);
        }

        this.buildVersion = buildProps.getProperty("version");
        this.buildDate = buildProps.getProperty("build.date");

    }

    @Override
    public String getBuildVersion() {
        return this.buildVersion;
    }

    @Override
    public String getBuildDate() {
        return this.buildDate;
    }
}
