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
                null, new SavedCredentials(SessionConstants.ACCESS_ID, SessionConstants.SECRET_KEY), true);
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
