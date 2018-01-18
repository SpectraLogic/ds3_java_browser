/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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
import com.spectralogic.ds3client.commands.spectrads3.DeleteBucketSpectraS3Request;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Ds3DeleteBucketTask extends Ds3Task {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3DeleteBucketTask.class);

    private final Ds3Client ds3Client;
    private final String bucketName;

    public Ds3DeleteBucketTask(final Ds3Client ds3Client, final String bucketName) {
        this.ds3Client = ds3Client;
        this.bucketName = bucketName;
    }

    @Override
    protected String call() {
        try {
            ds3Client.deleteBucketSpectraS3(new DeleteBucketSpectraS3Request(bucketName).withForce(true));
            return StringConstants.SUCCESS;
        } catch (final IOException e) {
            LOG.error("Failed to delete Bucket: ", e);
            setErrorMsg(e.getMessage());
            this.fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
            return e.toString();
        }
    }

    @Override
    public String getErrorMsg() {
        return this.errorMsg;
    }

    @Override
    public void setErrorMsg(final String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
