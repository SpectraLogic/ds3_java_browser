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

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.concurrent.Task;
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
    private static final LazyAlert alert = new LazyAlert("Error");

    protected ResourceBundle resourceBundle;
    protected Ds3Client ds3Client;
    protected DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    protected Session currentSession;
    protected LoggingService loggingService;
    protected Ds3ClientHelpers.Job job = null;

    boolean isJobFailed = false;

    @Override
    protected final Boolean call() throws Exception {
        LOG.info("Starting DS3 Job");
        try {
            executeJob();
        } catch (final Exception e) {
            LOG.error("Job failed with an exception: " + e.getMessage(), e);
            return false;
        }
        LOG.info("Job finished successfully");
        return true;
    }

    public abstract void executeJob() throws Exception;

    public abstract UUID getJobId();

    public void updateProgressPutJob() {
        updateProgress(0.1, 100);
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
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
                    LOG.error("Failed attempting to updateMessage while waiting for chunks to become available for job: " + job.getJobId(), e);
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
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(
                jobInterruptionStore.getJobIdsModel().getEndpoints().stream().collect(GuavaCollectors.immutableList()),
                ds3Client.getConnectionDetails().getEndpoint(),
                deepStorageBrowserPresenter.getJobProgressView(), jobId);
        if (currentSession != null) {
            final String currentSelectedEndpoint = currentSession.getEndpoint() + COLON + currentSession.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
            }
        }
    }

    void removeJobIdAndUpdateJobsBtn(final JobInterruptionStore jobInterruptionStore, final UUID jobId) {
        final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(), ds3Client.getConnectionDetails().getEndpoint(), deepStorageBrowserPresenter, loggingService);
        if (currentSession != null) {
            final String currentSelectedEndpoint = currentSession.getEndpoint() + COLON + currentSession.getPortNo();
            if (currentSelectedEndpoint.equals(ds3Client.getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, deepStorageBrowserPresenter);
            }
        }
    }

    boolean isJobFailed() {
        return isJobFailed;
    }

    void hostNotAvaialble() {
        final String msg = resourceBundle.getString("host") + SPACE + ds3Client.getConnectionDetails().getEndpoint() + resourceBundle.getString("unreachable");
        ErrorUtils.dumpTheStack(msg);
        loggingService.logMessage(resourceBundle.getString("unableToReachNetwork"), LogType.ERROR);
        alert.showAlert(msg);
    }
}

