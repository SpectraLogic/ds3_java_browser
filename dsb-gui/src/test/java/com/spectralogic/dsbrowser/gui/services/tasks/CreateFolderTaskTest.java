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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.ConfigProperties;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import com.spectralogic.dsbrowser.gui.util.SessionConstants;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class CreateFolderTaskTest {

    private final Workers workers = new Workers();
    private Session session;
    private boolean successFlag = false;
    private final static ResourceBundle resourceBundle = ResourceBundle.getBundle("lang", new Locale(ConfigProperties.getInstance().getLanguage()));

    @Before
    public void setUp() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession(SessionConstants.SESSION_NAME, SessionConstants.SESSION_PATH, SessionConstants.PORT_NO,
                    null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), false);
            session = new CreateConnectionTask().createConnection(SessionModelService.setSessionModel(savedSession, false), resourceBundle);
        });
    }

    @Test
    public void createFolder() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String folderName = SessionConstants.DS3_PANEL_SERVICE_TEST_FOLDER_NAME
                        + StringConstants.UNDER_SCORE + LocalDateTime.now().getSecond();
                final CreateFolderModel createFolderModel = new CreateFolderModel(session.getClient(), SessionConstants.ALREADY_EXIST_BUCKET,
                        SessionConstants.ALREADY_EXIST_BUCKET);
                final String location = PathUtil.getFolderLocation(createFolderModel.getLocation(),
                        createFolderModel.getBucketName());
                //Instantiating create folder task
                final CreateFolderTask createFolderTask = new CreateFolderTask(session.getClient(), createFolderModel,
                        folderName, PathUtil.getDs3ObjectList(location, folderName),
                        null, null);
                workers.execute(createFolderTask);
                //Validating test case
                createFolderTask.setOnSucceeded(event -> {
                    successFlag = true;
                    //Releasing main thread
                    latch.countDown();
                });
                createFolderTask.setOnFailed(event -> {
                    //Releasing main thread
                    latch.countDown();
                });
                createFolderTask.setOnCancelled(event -> {
                    //Releasing main thread
                    latch.countDown();
                });
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}
