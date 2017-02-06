package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                final Path propertiesFile = Paths.get(Main.class.getResource("/config.properties").toURI());
                properties.load(Files.newBufferedReader(propertiesFile));
                final String language = properties.getProperty("language");
                setLanguage(language);
            } catch (final URISyntaxException use) {
                LOG.error("Property File not found", use);
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
