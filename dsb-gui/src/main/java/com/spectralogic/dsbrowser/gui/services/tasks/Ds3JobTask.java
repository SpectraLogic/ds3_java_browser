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
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public abstract class Ds3JobTask extends Task<Boolean> {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3JobTask.class);

    protected Ds3Client ds3Client;

    @Override
    protected final Boolean call() throws Exception {
        LOG.info("Starting DS3 Job");
        executeJob();
        return true;
    }

    public abstract void executeJob() throws Exception;

    public abstract UUID getJobId();

    public Ds3Client getDs3Client() {
        return ds3Client;
    }

    public boolean isInCache() {
        return getProgress() == 1;
    }

}
