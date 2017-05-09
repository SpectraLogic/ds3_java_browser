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
