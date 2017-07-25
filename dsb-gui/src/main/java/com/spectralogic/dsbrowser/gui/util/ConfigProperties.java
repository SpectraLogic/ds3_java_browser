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

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

//Singleton class for getting the language from config.properties
public class ConfigProperties {
    private final static Logger LOG = LoggerFactory.getLogger(ConfigProperties.class);

    private static ConfigProperties configProperties;
    private static String language;

    private ConfigProperties() {
    }

    public static ConfigProperties getInstance() {
        LOG.info("Reading properties file.");
        if (configProperties == null) {
            configProperties = new ConfigProperties();
            final Properties properties = new Properties();
            try {
                properties.load(Main.class.getResourceAsStream("/config.properties"));
                final String language = properties.getProperty("language");
                setLanguage(language);
            } catch (final Exception e) {
                LOG.error("Property File not Loaded", e);
            }
        }
        return configProperties;
    }

    public String getLanguage() {
        return language;
    }

    private static void setLanguage(final String language) {
        ConfigProperties.language = language;
    }
}
