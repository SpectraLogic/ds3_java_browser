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
import com.spectralogic.ds3client.commands.spectrads3.DeleteFolderRecursivelySpectraS3Request;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;

import java.util.Optional;

public class Ds3DeleteFolderTask extends Ds3Task {

    private final Ds3Client ds3Client;
    private final String bucketName;
    private final String fullName;

    public Ds3DeleteFolderTask(final Ds3Client ds3Client, final String bucketName, final String fullName) {
        this.ds3Client = ds3Client;
        this.bucketName = bucketName;
        this.fullName = fullName;
    }


    @Override
    protected Optional<Object> call() throws Exception {

        try {
            ds3Client.deleteFolderRecursivelySpectraS3(
                    new DeleteFolderRecursivelySpectraS3Request(bucketName, fullName));
            return Optional.of(StringConstants.SUCCESS);
        } catch (final Exception e) {
            errorMsg = e.getMessage();
            this.fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
            return Optional.empty();
        }
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }
}
