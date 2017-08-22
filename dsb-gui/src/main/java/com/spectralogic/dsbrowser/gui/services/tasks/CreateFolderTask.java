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
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;


public class CreateFolderTask extends Ds3Task {

    private final static Logger LOG = LoggerFactory.getLogger(CreateFolderTask.class);

    private final Ds3Client ds3Client;
    private final String bucketName;
    private final String folderName;
    private final List<Ds3Object> ds3ObjectList;
    private final LoggingService loggingService;
    private final ResourceBundle resourceBundle;

    public CreateFolderTask(final Ds3Client ds3Client,
                            final String bucketName,
                            final String folderName,
                            final List<Ds3Object> ds3ObjectList,
                            final LoggingService loggingService,
                            final ResourceBundle resourceBundle) {
        this.ds3Client = ds3Client;
        this.bucketName = bucketName;
        this.folderName = folderName;
        this.ds3ObjectList = ds3ObjectList;
        this.loggingService = loggingService;
        this.resourceBundle = resourceBundle;
    }

    @Override
    protected Object call() throws Exception {
        try {
            return Ds3ClientHelpers.wrap(ds3Client).createFolder(bucketName, folderName);
        } catch (final Exception e) {
            LOG.error("Failed to create folder", e);
            loggingService.logMessage(resourceBundle.getString("createFolderErr")
                    + StringConstants.SPACE + folderName
                    + StringConstants.SPACE + resourceBundle.getString("txtReason")
                    + StringConstants.SPACE + e, LogType.ERROR);
            throw e;
        }
    }
}
