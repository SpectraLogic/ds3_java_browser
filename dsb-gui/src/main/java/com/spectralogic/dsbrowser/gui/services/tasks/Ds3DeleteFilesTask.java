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

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Ds3DeleteFilesTask extends Ds3Task {

    private final ImmutableList<String> buckets;
    private final Ds3Client ds3Client;
    private String errorMsg;
    private final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap;

    public Ds3DeleteFilesTask(final Ds3Client ds3Client,
                              final ImmutableList<String> buckets,
                              final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap) {
        this.ds3Client = ds3Client;
        this.buckets = buckets;
        this.bucketObjectsMap = bucketObjectsMap;
    }

    @Override
    protected Optional<String> call() {
        try {
            int deleteSize = 0;
            final Set<String> bucketSet = bucketObjectsMap.keySet();
            for (final String bucket : buckets) {
                ds3Client.deleteObjects(new DeleteObjectsRequest(bucket,
                        bucketObjectsMap.get(bucket).stream().map(Ds3TreeTableValue::getFullName)
                                .collect(Collectors.toList())));
                deleteSize++;
                if (deleteSize == bucketSet.size()) {
                    return Optional.of(StringConstants.SUCCESS);
                }
            }
        } catch (final IOException e) {
            errorMsg = e.getMessage();
            this.fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
            return Optional.empty();
        }
        return Optional.empty();
    }

    @Override
    public String getErrorMsg() {
        return errorMsg;
    }
}
