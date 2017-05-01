package com.spectralogic.dsbrowser.gui.services.logservice;

import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogServiceTest {

    @Test
    public void updateLogSettings() throws Exception {
        final LogSettings defaultLogSettings = LogSettings.DEFAULT;
        assertEquals(defaultLogSettings.logLocationProperty().getValue(),
                Paths.get(System.getProperty(StringConstants.SETTING_FILE_PATH), StringConstants.SETTING_FILE_FOLDER_NAME, StringConstants.LOG).toString());
        assertEquals(defaultLogSettings.getLogSize(), 1);
        assertEquals(defaultLogSettings.getNumRollovers(), 10);
        assertTrue(defaultLogSettings.getDebugLogging());
        assertFalse(defaultLogSettings.getConsoleLogging());

        final LogSettings modifiedLogSettings = defaultLogSettings.copy();
        modifiedLogSettings.setLogSize(2);
        assertEquals(modifiedLogSettings.getLogSize(), 2);
        modifiedLogSettings.setNumRollovers(20);
        assertEquals(modifiedLogSettings.getNumRollovers(), 20);
    }

}
