package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public class BackgroundTask implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(BackgroundTask.class);
    private final Ds3Common ds3Common;
    private final Workers workers;
    private boolean isAlertDisplayed = false;
    private final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public BackgroundTask(final Ds3Common ds3Common, final Workers workers) {
        this.ds3Common = ds3Common;
        this.workers = workers;
    }

    @Override
    public void run() {

        while (true) {
            try {
                if (ds3Common.getCurrentSession() != null) {
                    final Session session = ds3Common.getCurrentSession();
                    if (CheckNetwork.isReachable(session.getClient())) {
                        if (isAlertDisplayed) {
                            LOG.info("network is up");
                            Platform.runLater(() -> RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers));
                            isAlertDisplayed = false;
                        } else {
                            LOG.info("network is working");
                        }

                    } else {
                        LOG.error("network is not reachable");
                        if (!isAlertDisplayed) {
                            final String msg = resourceBundle.getString("host") + session.getClient().getConnectionDetails().getEndpoint() + resourceBundle.getString("unreachable");
                            Platform.runLater(() -> {
                                dumpTheStack(msg);
                            });
                            Ds3Alert.show(resourceBundle.getString("networkConError"), msg, Alert.AlertType.ERROR);
                            isAlertDisplayed = true;
                        }
                    }
                } else {
                    LOG.error("No Connection..");
                }
                Thread.sleep(3000);
            } catch (final Throwable e) {
                LOG.error("Encountered an error when attempting to verify that the bp is reachable", e);
            }
        }
    }

    public static void dumpTheStack(final String msg) {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 1; i < elements.length; i++) {
            final StackTraceElement s = elements[i];
            LOG.info(msg + "====> \tat " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
        }
    }
}
