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
