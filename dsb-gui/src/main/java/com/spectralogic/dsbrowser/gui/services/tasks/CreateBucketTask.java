package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.PutBucketSpectraS3Request;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;


public class CreateBucketTask extends Ds3Task {
    private final static Logger LOG = LoggerFactory.getLogger(CreateBucketTask.class);

    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;
    private final String bucketName;
    private final Ds3Client ds3Client;
    private final CreateBucketModel createBucketModel;
    private final ResourceBundle resourceBundle;

    public CreateBucketTask(final CreateBucketModel createBucketModel,
                            final Ds3Client ds3Client, final String bucketName,
                            final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
        this.bucketName = bucketName;
        this.createBucketModel = createBucketModel;
        this.ds3Client = ds3Client;
        resourceBundle = ResourceBundleProperties.getResourceBundle();
    }

    @Override
    protected Object call() throws Exception {
        try {
            return ds3Client.putBucketSpectraS3(new PutBucketSpectraS3Request(bucketName)
                    .withDataPolicyId(createBucketModel.getId()));
        } catch (final Exception e) {
            LOG.error("Failed to create bucket", e);
            if (null != deepStorageBrowserPresenter) {
                deepStorageBrowserPresenter.logText(resourceBundle.getString("createBucketFailedErr") + e, LogType.ERROR);
            }
            throw e;
        }
    }
}
