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

import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class CheckNetwork_Test {
    private static Session session;
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO, null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false, false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false));
        });
    }

    @Test
    public void formatUrlPassThrough() {
        assertThat(CheckNetwork.formatUrl("http://host"), is("http://host"));
    }

    @Test
    public void formatUrlWithHttp() {
        assertThat(CheckNetwork.formatUrl("host"), is("http://host"));
    }

    @Test
    public void formatUrlWithHttps() {
        assertThat(CheckNetwork.formatUrl("https://host"), is("http://host"));
    }

    @Test
    public void isReachable_Test() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                successFlag = CheckNetwork.isReachable(session.getClient());
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await(5, TimeUnit.SECONDS);
        assertTrue(successFlag);
    }
}
