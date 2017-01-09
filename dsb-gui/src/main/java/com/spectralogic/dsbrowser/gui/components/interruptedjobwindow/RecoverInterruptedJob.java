package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetJobSpectraS3Response;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.FileObjectPutter;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.networking.FailedRequestException;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import javafx.application.Platform;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class RecoverInterruptedJob extends Ds3JobTask {

    private final UUID uuid;
    private final EndpointInfo endpointInfo;
    private final JobInterruptionStore jobInterruptionStore;
    private final Ds3Client ds3Client;

    public RecoverInterruptedJob(final UUID uuid, final EndpointInfo endpointInfo, final JobInterruptionStore jobInterruptionStore) {
        this.uuid = uuid;
        this.endpointInfo = endpointInfo;
        this.jobInterruptionStore = jobInterruptionStore;
        ds3Client = endpointInfo.getClient();
    }

    public Ds3Client getDs3Client() {
        return ds3Client;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void executeJob() throws Exception {
        try {
            final FilesAndFolderMap filesAndFolderMapMap = endpointInfo.getJobIdAndFilesFoldersMap().get(uuid.toString());
            final Ds3Client client = endpointInfo.getClient();
            final String date = DateFormat.formatDate(new Date());
            updateTitle("Recovering " + filesAndFolderMapMap.getType() + " job of " + endpointInfo.getEndpoint() + " " + date);
            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Recovering " + filesAndFolderMapMap.getType() + " job of " + filesAndFolderMapMap.getDate(), LogType.INFO));
            final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client, 100);

            Ds3ClientHelpers.Job job = null;
            Path fileTreeModel = null;

            final Map<String, Path> files = filesAndFolderMapMap.getFiles();
            final Map<String, Path> folders = filesAndFolderMapMap.getFolders();

            final long totalJobSize = filesAndFolderMapMap.getTotalJobSize();

            if (filesAndFolderMapMap.getType().equals("PUT")) {
                job = helpers.recoverWriteJob(uuid);
                updateMessage("Initiating transfer to " + job.getBucketName());

            } else if (filesAndFolderMapMap.getType().equals("GET")) {
                job = helpers.recoverReadJob(uuid);
                fileTreeModel = Paths.get(filesAndFolderMapMap.getTargetLocation());

                updateMessage("Initiating transfer from " + job.getBucketName());
            }

            final AtomicLong totalSent = new AtomicLong(0L);


            job.attachDataTransferredListener(l -> {
                updateProgress(totalSent.getAndAdd(l) / 2, totalJobSize);
                totalSent.addAndGet(l);
            });

            job.attachObjectCompletedListener(s -> {
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Successfully transferred: " + s + " to " + filesAndFolderMapMap.getTargetLocation(), LogType.SUCCESS));
                updateMessage(FileSizeFormat.getFileSizeType(totalSent.get() / 2) + "/" + FileSizeFormat.getFileSizeType(totalJobSize) + " Transferring file -> " + s + " to " + filesAndFolderMapMap.getTargetLocation());
            });


            final Path finalFileTreeModel = fileTreeModel;
            job.transfer(s -> {
                        if (filesAndFolderMapMap.getType().equals("PUT")) {
                            if (files.containsKey(s)) {
                                return new FileObjectPutter(files.get(s)).buildChannel("");
                            } else {
                                final Map.Entry<String, Path> stringPathEntry = folders.entrySet().stream().filter(value -> s.contains(value.getKey())).findFirst().get();
                                final String restOfThePath = s.replaceFirst(stringPathEntry.getKey(), "");
                                final Path finalPath = Paths.get(stringPathEntry.getValue().toString(), restOfThePath);
                                return new FileObjectPutter(finalPath).buildChannel("");
                            }

                        } else {
                            if (filesAndFolderMapMap.isNonAdjacent())
                                return new FileObjectGetter(finalFileTreeModel).buildChannel(s);
                            else {
                                String skipPath = "";
                                final File file = new File(s);
                                if (folders.size() == 0) {
                                    if (file.getParent() != null)
                                        skipPath = file.getParent();
                                    else
                                        skipPath = "";
                                }/* else {
                                    final Stream<Map.Entry<String, Path>> entryStream = folders.entrySet().stream().filter(i -> s.startsWith(i.getKey()));
                                    if (entryStream != null) {
                                        skipPath = entryStream.findFirst().get().getValue().toString();
                                    }
                                }*/
                                if (skipPath.isEmpty()) {
                                    return new FileObjectGetter(finalFileTreeModel).buildChannel(s);
                                } else {
                                    return new PrefixRemoverObjectChannelBuilder(new FileObjectGetter(finalFileTreeModel), skipPath).buildChannel(s.substring(("/" + skipPath).length()));
                                }
                            }

                        }
                    }
            );
            Platform.runLater(() -> {
                endpointInfo.getDeepStorageBrowserPresenter().logText("Recovering " + filesAndFolderMapMap.getType() + " job. Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to" + filesAndFolderMapMap.getTargetLocation() + "(BlackPearl cache).Waiting for the storage target allocation.", LogType.SUCCESS);
                updateMessage("Recovering " + filesAndFolderMapMap.getType() + " job. Files [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + "] transferred to " + filesAndFolderMapMap.getTargetLocation() + "(BlackPearl cache). Waiting for the storage target allocation.");
                updateProgress(totalJobSize, totalJobSize);
            });


//            //Can not assign final.
            GetJobSpectraS3Response response = client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));

            while (!response.getMasterObjectListResult().getStatus().toString().equals("COMPLETED")) {
                Thread.sleep(60000);
                response = client.getJobSpectraS3(new GetJobSpectraS3Request(job.getJobId()));
            }

            Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Job [Size: " + FileSizeFormat.getFileSizeType(totalJobSize) + " ] recovery completed. File transferred to " + filesAndFolderMapMap.getTargetLocation() + " (storage location)", LogType.SUCCESS));
            final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.removeJobID(jobInterruptionStore, uuid.toString(), endpointInfo.getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter());
            final Session session = endpointInfo.getDs3Common().getCurrentSession().stream().findFirst().get();
            final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
            if (currentSelectedEndpoint.equals(session.getClient().getConnectionDetails().getEndpoint())) {
                ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
            }
        } catch (final Exception e) {
            e.printStackTrace();
            if (e instanceof FailedRequestException) {
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Job not found. Cancelling job.", LogType.INFO));
                cancel();
            } else {
                Platform.runLater(() -> endpointInfo.getDeepStorageBrowserPresenter().logText("Exception" + e.toString() + "-User Interruption", LogType.ERROR));
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(jobInterruptionStore.getJobIdsModel().getEndpoints(), endpointInfo.getClient().getConnectionDetails().getEndpoint(), endpointInfo.getDeepStorageBrowserPresenter().getJobProgressView(), uuid);
                final Session session = endpointInfo.getDs3Common().getCurrentSession().stream().findFirst().get();
                final String currentSelectedEndpoint = session.getEndpoint() + ":" + session.getPortNo();
                if (currentSelectedEndpoint.equals(endpointInfo.getClient().getConnectionDetails().getEndpoint())) {
                    ParseJobInterruptionMap.setButtonAndCountNumber(jobIDMap, endpointInfo.getDeepStorageBrowserPresenter());
                }
            }

        }
    }
}
