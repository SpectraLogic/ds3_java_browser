package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.PutBulkJobSpectraS3Request;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ResourceBundle;


public class CreateFolderTask extends Ds3Task {

    private final static Logger LOG = LoggerFactory.getLogger(CreateFolderTask.class);

    private final Ds3Client ds3Client;
    private final CreateFolderModel createFolderModel;
    private final String folderName;
    private final List<Ds3Object> ds3ObjectList;
    private final LoggingService loggingService;
    private final ResourceBundle resourceBundle;

    public CreateFolderTask(final Ds3Client ds3Client,
                            final CreateFolderModel createFolderModel,
                            final String folderName,
                            final List<Ds3Object> ds3ObjectList,
                            final LoggingService loggingService,
                            final ResourceBundle resourceBundle) {
        this.ds3Client = ds3Client;
        this.createFolderModel = createFolderModel;
        this.folderName = folderName;
        this.ds3ObjectList = ds3ObjectList;
        this.loggingService = loggingService;
        this.resourceBundle = resourceBundle;
    }

    @Override
    protected Object call() throws Exception {
        try {
            return ds3Client.putBulkJobSpectraS3(new PutBulkJobSpectraS3Request(createFolderModel.getBucketName().trim(),
                    ds3ObjectList));
        } catch (final Exception e) {
            LOG.error("Failed to create folder", e);
            loggingService.logMessage(resourceBundle.getString("createFolderErr")
                    + StringConstants.SPACE + folderName.trim() + StringConstants.SPACE
                    + resourceBundle.getString("txtReason")
                    + StringConstants.SPACE + e, LogType.ERROR);
            throw e;
        }
    }
}
