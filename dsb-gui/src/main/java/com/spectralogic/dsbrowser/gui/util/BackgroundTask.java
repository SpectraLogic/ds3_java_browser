package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackgroundTask implements Runnable{

    private final static Logger LOG = LoggerFactory.getLogger(BackgroundTask.class);
    private final Ds3Common ds3Common;
    private final Workers workers;
    private boolean isAlertDisplayed = false;
    private final static Alert ALERT = new Alert(Alert.AlertType.INFORMATION);

    public BackgroundTask(final Ds3Common ds3Common, final Workers workers) {
        this.ds3Common = ds3Common;
        this.workers = workers;
    }

    @Override
    public void run() {
        ALERT.setHeaderText(null);
        ALERT.setTitle("Network connection error");
        while (true) {
            try {
                if (ds3Common.getCurrentSession().stream().findFirst().isPresent()) {
                    final Session session = ds3Common.getCurrentSession().stream().findFirst().get();
                    if (CheckNetwork.isReachable(session.getClient())) {
                        if (isAlertDisplayed) {
                            LOG.info("network is up");
                            Platform.runLater(() -> ParseJobInterruptionMap.refreshCompleteTreeTableView(ds3Common, workers));
                            isAlertDisplayed = false;
                        } else {
                            LOG.info("network is working");
                        }

                    } else {
                        LOG.info("network is not reachable");
                        if (!isAlertDisplayed) {
                            Platform.runLater(() -> {
                                final String msg = "Host " + session.getClient().getConnectionDetails().getEndpoint() + " is unreachable. Please check your connection";
                                dumpTheStack(msg);
                                ALERT.setContentText(msg);
                                ALERT.showAndWait();
                            });

                            isAlertDisplayed = true;
                        }
                    }
                } else {
                    LOG.info("No Connection..");
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
