package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.DeleteObjectsRequest;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.Event;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Ds3DeleteFilesTask extends Ds3Task {

    private final ImmutableList<String> buckets;
    private final Ds3Client ds3Client;
    private String errorMsg;
    private final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap;

    public Ds3DeleteFilesTask(final Ds3Client ds3Client, final ImmutableList<String> buckets,
                              final Map<String, List<Ds3TreeTableValue>> bucketObjectsMap) {
        this.ds3Client = ds3Client;
        this.buckets = buckets;
        this.bucketObjectsMap = bucketObjectsMap;
    }

    @Override
    protected Object call() throws Exception {
        try {
            int deleteSize = 0;
            final Set<String> bucketSet = bucketObjectsMap.keySet();
            for (final String bucket : buckets) {
                ds3Client.deleteObjects(new DeleteObjectsRequest(bucket,
                        bucketObjectsMap.get(bucket).stream().map(Ds3TreeTableValue::getFullName)
                                .collect(Collectors.toList())));
                deleteSize++;
                if (deleteSize == bucketSet.size()) {
                    return StringConstants.SUCCESS;
                }
            }
        } catch (final Exception e) {
            errorMsg = e.getMessage();
            this.fireEvent(new Event(WorkerStateEvent.WORKER_STATE_FAILED));
            return null;
        }
        return null;
    }


    public String getErrorMsg() {
        return errorMsg;
    }

}
