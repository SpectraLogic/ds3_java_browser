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

package com.spectralogic.dsbrowser.gui.services.newSessionService;

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionModel;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class SessionModelServiceTest {
    private boolean successFlag = false;

    @Test
    public void createDefaultSessionTest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), true, false);
        final NewSessionModel newSessionModel = SessionModelService.setSessionModel(savedSession,false);
        if (!savedSession.isDefaultSession().equals(newSessionModel.getDefaultSession())) {
            successFlag = true;
            latch.countDown();
        } else {
            latch.countDown();
        }
        latch.await();
        assertTrue(successFlag);
    }
}
