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
