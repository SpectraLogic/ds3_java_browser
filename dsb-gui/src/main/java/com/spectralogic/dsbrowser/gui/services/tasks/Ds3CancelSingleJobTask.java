package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.CancelJobSpectraS3Response;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;

import com.spectralogic.dsbrowser.gui.util.StringConstants;

public class Ds3CancelSingleJobTask extends Task {

    private final Logger LOG = LoggerFactory.getLogger(Ds3CancelSingleJobTask.class);
    private final ResourceBundle resourceBundle;
    private final String uuid;
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private String jobType;

    public Ds3CancelSingleJobTask(final String uuid, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore , final String jobType) {
        this.jobType = jobType;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
    }

    @Override
    protected CancelJobSpectraS3Response call() throws Exception {
        try {
            final CancelJobSpectraS3Response cancelJobSpectraS3Response = endpointInfo.getClient().cancelJobSpectraS3(new CancelJobSpectraS3Request(uuid));
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("cancelJobStatus") + StringConstants.SPACE + cancelJobSpectraS3Response, LogType.SUCCESS));
        } catch (final Exception e) {
            LOG.error("Unable to cancel " + jobType + "  job", e);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("failedCancelJob") + StringConstants.SPACE + e, LogType.ERROR));
            fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
        } finally {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid, endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
            ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
        }
        return null;
    }
}
