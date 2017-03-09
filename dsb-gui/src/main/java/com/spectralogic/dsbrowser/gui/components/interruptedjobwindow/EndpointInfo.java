/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;

import java.util.Map;

public class EndpointInfo {

    private final String endpoint;
    private final Ds3Client client;
    private final Map<String, FilesAndFolderMap> jobIdAndFilesFoldersMap;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final Ds3Common ds3Common;

    public EndpointInfo() {
        this(null, null, null, null, null);
    }

    public EndpointInfo(final String endpoint, final Ds3Client client, final Map<String, FilesAndFolderMap> jobIdAndFilesFoldersMap, final DeepStorageBrowserPresenter deepStorageBrowserPresenter, final Ds3Common ds3Common) {
        this.endpoint = endpoint;
        this.client = client;
        this.jobIdAndFilesFoldersMap = jobIdAndFilesFoldersMap;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.ds3Common = ds3Common;
    }

    public Ds3Common getDs3Common() {
        return ds3Common;
    }

    public DeepStorageBrowserPresenter getDeepStorageBrowserPresenter() {
        return deepStorageBrowserPresenter;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Ds3Client getClient() {
        return client;
    }

    public Map<String, FilesAndFolderMap> getJobIdAndFilesFoldersMap() {
        return jobIdAndFilesFoldersMap;
    }
}
