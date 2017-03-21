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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Properties;

public class BuildInfoServiceImpl implements BuildInfoService{
    private static final Logger LOG = LoggerFactory.getLogger(BuildInfoServiceImpl.class);

    public static final String buildPropertiesFile = "ds3_cli.properties";

    private String buildVersion;
    private LocalDateTime buildDateTime;

    public BuildInfoServiceImpl() {
        final Properties buildProps = getBuildProperties();
        this.buildVersion = initBuildVersion(buildProps);
        this.buildDateTime = initBuildDateTime(buildProps);
    }

    @Override
    public String getBuildVersion() {
        return this.buildVersion;
    }

    @Override
    public LocalDateTime getBuildDateTime() {
        return this.buildDateTime;
    }

    private Properties getBuildProperties() {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(buildPropertiesFile);
        final Properties buildProps = new Properties();
        try {
            buildProps.load(is);
        } catch (final IOException e) {
            LOG.error("Failed to read build properties from {}: ", buildPropertiesFile, e);
        }

        return buildProps;
    }

    private String initBuildVersion(final Properties buildProps) {
        return buildProps.getProperty("version");
    }

    private LocalDateTime initBuildDateTime(final Properties buildProps) {
        // build.date=Wed Mar 15 11:10:25 MDT 2017
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("E M d hh:mm:ss zzz yyyy");
        try {
            return LocalDateTime.parse(buildProps.getProperty("build.date"), dtf);
        }catch (final NullPointerException npe) {
            LOG.error("Failed to find build.date property in Resource file: {}\n", buildPropertiesFile, npe);
        }catch (final DateTimeParseException dtpe) {
            LOG.error("Failed to convert build.date property into Date object: {}\n", buildProps.getProperty("build.date"), dtpe);
        }

        return LocalDateTime.MIN;
    }
}
