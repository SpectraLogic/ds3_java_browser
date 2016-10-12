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
