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

import com.google.inject.assistedinject.Assisted;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.ResourceBundle;


public class CreateFolderTask extends Ds3Task<Void> {

    private final static Logger LOG = LoggerFactory.getLogger(CreateFolderTask.class);

    private final Ds3Client ds3Client;
    private final String bucketName;
    private final String folderName;
    private final LoggingService loggingService;
    private final ResourceBundle resourceBundle;
    private final AlertService alertService;

    @Inject
    public CreateFolderTask(
            final Ds3Common ds3Common,
            final AlertService alertService,
            @Assisted("bucketName") final String bucketName,
            @Assisted("folderName") final String folderName,
            final LoggingService loggingService,
            final ResourceBundle resourceBundle
    ) {
        this.ds3Client = ds3Common.getCurrentSession().getClient();
        this.bucketName = bucketName;
        this.folderName = folderName;
        this.loggingService = loggingService;
        this.resourceBundle = resourceBundle;
        this.alertService = alertService;
    }

    @Override
    protected Void call() throws IOException {
        try {
            Ds3ClientHelpers.wrap(ds3Client).createFolder(bucketName, folderName);
        } catch (final FailedRequestException failedRequestException) {
            alertService.error("bucketAlreadyExists");
        } catch (final IOException e) {
            LOG.error("Failed to create folder", e);
            loggingService.logMessage(resourceBundle.getString("createFolderErr")
                    + StringConstants.SPACE + folderName
                    + StringConstants.SPACE + resourceBundle.getString("txtReason")
                    + StringConstants.SPACE + e, LogType.ERROR);
            throw e;
        }
        return null;
    }

    public interface CreateFolderTaskFactory {
        public CreateFolderTask create(@Assisted("bucketName") final String bucketName, @Assisted("folderName") final String folderName);
    }
}
