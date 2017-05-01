package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.COLON;
import static com.spectralogic.dsbrowser.gui.util.StringConstants.SPACE;

public abstract class Ds3JobTask extends Task<Boolean> {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3JobTask.class);

    protected final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();
    protected Ds3Client ds3Client;
    protected Ds3Common ds3Common;
    protected LoggingService loggingService;
    protected Ds3ClientHelpers.Job job = null;

    boolean isJobFailed = false;

    @Override
    protected final Boolean call() throws Exception {
        LOG.info("Starting DS3 Job");
        try {
            executeJob();
        } catch (final Exception e) {
            LOG.error("Job failed with an exception", e);
            return false;
        }
        LOG.info("Job finished successfully");
        return true;
    }

    public abstract void executeJob() throws Exception;

    public void updateProgressPutJob() {
        updateProgress(0.1, 100);
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }

    public Ds3Common getDs3Common() {
        return ds3Common;
    }

     AtomicLong addDataTransferListener(final long totalJobSize) {
        final AtomicLong totalSent = new AtomicLong(0L);
        job.attachDataTransferredListener(l -> {
            updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
            totalSent.addAndGet(l);
        });
        return totalSent;
    }

     void addWaitingForChunkListener(final long totalJobSize, final String targetDir) {
        job.attachWaitingForChunksListener(retryAfterSeconds -> {
            for (int retryTimeRemaining = retryAfterSeconds; retryTimeRemaining >= 0; retryTimeRemaining--) {
                try {
                    updateMessage(resourceBundle.getString("noAvailableChunks") + SPACE + retryTimeRemaining + resourceBundle.getString("seconds"));
                    Thread.sleep(1000);
                } catch (final Exception e) {
                    LOG.error("Exception in attachWaitingForChunksListener: {}", e);
                }
            }
            updateMessage(StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize), targetDir).toString());
        });
    }
     void getTransferRates(final Instant jobStartInstant, final AtomicLong totalSent, final long totalJobSize, final String sourceLocation, final String targetLocation) {
        final Instant currentTime = Instant.now();
        final long timeElapsedInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTime.toEpochMilli() - jobStartInstant.toEpochMilli());
        long transferRate = 0;
        if (timeElapsedInSeconds != 0) {
            transferRate = (totalSent.get() / 2) / timeElapsedInSeconds;
        }

        if (transferRate != 0) {
            final long timeRemaining = (totalJobSize - (totalSent.get() / 2)) / transferRate;

            updateMessage(StringBuilderUtil.getTransferRateString(transferRate, timeRemaining, totalSent,
                    totalJobSize, sourceLocation, targetLocation).toString());
        } else {
            updateMessage(StringBuilderUtil.getTransferRateString(transferRate, 0, totalSent,
                    totalJobSize, sourceLocation, targetLocation).toString());
        }
        updateProgress(totalSent.get() / 2 , totalJobSize);

    }

     void updateInterruptedJobsBtn(final JobInterruptionStore jobInterruptionStore, final UUID jobId) {
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints().stream().collect(GuavaCollectors.immutableList()), ds3Client.getConnectionDetails().getEndpoint(), ds3Common.getDeepStorageBrowserPresenter().getJobProgressView(), jobId);
            final Session session = ds3Common.getCurrentSession();
            if (session != null) {
                final String currentSelectedEndpoint = session.getEndpoint() + COLON + session.getPortNo();
                if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, ds3Common.getDeepStorageBrowserPresenter());
                }

        }
    }

     void removeJobIdAndUpdateJobsBtn(final JobInterruptionStore jobInterruptionStore, final UUID jobId) {
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), ds3Common.getDeepStorageBrowserPresenter());
        final Session session = ds3Common.getCurrentSession();
        if (session != null) {
            final String currentSelectedEndpoint = session.getEndpoint() + COLON + session.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, ds3Common.getDeepStorageBrowserPresenter());
            }

        }
    }

    boolean isJobFailed() {
        return isJobFailed;
    }

     void hostNotAvaialble() {
         final String msg = resourceBundle.getString("host") + SPACE + ds3Client.getConnectionDetails().getEndpoint() + resourceBundle.getString("unreachable");
         BackgroundTask.dumpTheStack(msg);
         loggingService.logMessage(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
         Ds3Alert.show(resourceBundle.getString("unavailableNetwork"), msg, Alert.AlertType.INFORMATION);
    }

}
