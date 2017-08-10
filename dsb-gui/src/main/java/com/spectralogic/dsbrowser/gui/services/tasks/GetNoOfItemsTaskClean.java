package com.spectralogic.dsbrowser.gui.services.tasks;/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 *  ****************************************************************************
 */


import com.google.common.collect.FluentIterable;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.dsbrowser.gui.components.ds3panel.FilesCountModelClean;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("Guava")
public class GetNoOfItemsTaskClean extends Task<FilesCountModelClean> {

    private static final Contents[] emptyContents = new Contents[0];
    private final FluentIterable<Ds3TreeTableValue> values;
    private final Ds3Client client;

    public GetNoOfItemsTaskClean(final Ds3Client client, final FluentIterable<Ds3TreeTableValue> values) {
        this.client = client;
        this.values = values;
    }

    @Override
    protected FilesCountModelClean call() throws Exception {
        final long[] fileStats = {0L, 0L, 0L};
        final Set<String> pathParts = new HashSet<>();
        values
                .filter(Objects::nonNull)
                .transformAndConcat(this::listObjects)
                .stream().distinct()
                .forEach(ds3Obj -> {
                    final String fullPath = ds3Obj.getKey();
                    final long size = ds3Obj.getSize();
                    if (fullPath.endsWith("/") && size == 0) {
                        fileStats[1]++;
                    } else {
                        final String pathWithoutFileName = fullPath.substring(0, fullPath.lastIndexOf('/'));
                        fileStats[0]++;
                        pathParts.addAll(Arrays.asList(pathWithoutFileName.split("/")));
                    }
                    fileStats[2] += size;
                });
        return new FilesCountModelClean(fileStats[0], fileStats[1] + pathParts.size(), fileStats[2]);
    }

    private Iterable<Contents> listObjects(final Ds3TreeTableValue item) {
        final Ds3ClientHelpers wrappeddClient = Ds3ClientHelpers.wrap(client);
        try {
            return wrappeddClient.listObjects(item.getBucketName(), item.getFullName());
        } catch (final IOException e) {
            return FluentIterable.from(emptyContents);
        }
    }

}
