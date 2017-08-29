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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;


public class Ds3DeleteFoldersTask extends Ds3Task {
    private final static Logger LOG = LoggerFactory.getLogger(CancelAllRunningJobsTask.class);
    private final Ds3Client ds3Client;
    private final ImmutableMultimap<String, String> deleteFoldersMap;

    public Ds3DeleteFoldersTask(final Ds3Client ds3Client,
                                final ImmutableMultimap<String, String> deleteFoldersMap) {
        this.ds3Client = ds3Client;
        this.deleteFoldersMap = deleteFoldersMap;
    }


    @Override
    protected Optional<Object> call() {
        boolean success = true;
        for (final Map.Entry<String, String> entry : deleteFoldersMap.entries()) {
            final String bucketName = entry.getKey();
            final String folderPath = entry.getValue();

            try {
                ds3Client.deleteFolderRecursivelySpectraS3(new DeleteFolderRecursivelySpectraS3Request(bucketName, folderPath));
                LOG.info("Deleted folder " + bucketName + ":" + folderPath);
            } catch (final IOException e) {
                success = false;
                LOG.error("Failed to delete folder " + bucketName + ":" + folderPath, e);
                errorMsg = errorMsg.concat("\n" + e.getMessage());
            }
        }

        if (!success) {
            this.fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
            return Optional.empty();
        }

        return Optional.of(StringConstants.SUCCESS);
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }
}
