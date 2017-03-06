package com.spectralogic.dsbrowser.gui.services.tasks;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.helpers.FileObjectGetter;
import com.spectralogic.ds3client.helpers.channelbuilders.PrefixRemoverObjectChannelBuilder;
import com.spectralogic.ds3client.metadata.MetadataReceivedListenerImpl;
import com.spectralogic.ds3client.models.JobRequestType;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Ds3GetJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3GetJob.class);
    private final List<Ds3TreeTableValueCustom> list;
    private final Path fileTreeModel;
    private final List<Ds3TreeTableValueCustom> nodes;
    private final String jobPriority;
    private final Map<Path, Path> map;
    private final int maximumNumberOfParallelThreads;
    private final JobInterruptionStore jobInterruptionStore;
    private final ResourceBundle resourceBundle;
    private UUID jobId;

    public Ds3GetJob(final List<Ds3TreeTableValueCustom> list, final Path fileTreeModel, final Ds3Client client, final String jobPriority, final int maximumNumberOfParallelThreads, final JobInterruptionStore jobInterruptionStore, final Ds3Common ds3Common) {
        this.list = list;
        this.fileTreeModel = fileTreeModel;
        this.ds3Client = client;
        nodes = new ArrayList<>();
        this.jobPriority = jobPriority;
        this.map = new HashMap<>();
        this.maximumNumberOfParallelThreads = maximumNumberOfParallelThreads;
        this.jobInterruptionStore = jobInterruptionStore;
        this.ds3Common = ds3Common;
        this.resourceBundle = ResourceBundleProperties.getResourceBundle();
    }

    @Override
    public void executeJob() throws Exception {
        //Job start time
        final Instant jobStartTimeInstant = Instant.now();
        LOG.info("Get Job started");
        try {
            updateTitle(resourceBundle.getString("blackPearlHealth"));
            if (CheckNetwork.isReachable(ds3Client)) {
                final String startJobDate = DateFormat.formatDate(new Date());
                updateTitle(StringBuilderUtil.jobInitiatedString(JobRequestType.GET.toString(), startJobDate, ds3Client.getConnectionDetails().getEndpoint()).toString());
                ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.jobInitiatedString
                        (JobRequestType.GET.toString(), startJobDate, ds3Client.getConnectionDetails().getEndpoint())
                        .toString(), LogType.INFO);
                updateMessage(resourceBundle.getString("transferring") + StringConstants.DOUBLE_DOTS);

                final ImmutableList<Ds3TreeTableValueCustom> directories = getDirectoriesOrFiles(list, false);
                final ImmutableList<Ds3TreeTableValueCustom> files = getDirectoriesOrFiles(list, true);
                final Map<Path, Boolean> duplicateFileMap = new HashMap<>();
                final ImmutableMap.Builder<String, Path> fileMap = createFileMap(files);
                final ImmutableMap.Builder<String, Path> folderMap = createFolderMap(directories);

                final List<Ds3TreeTableValueCustom> filteredNode = nodes.stream().filter(i -> i.getSize() != 0).collect(Collectors.toList());
                final ImmutableList<Ds3Object> objects = getDS3Object(filteredNode);
                final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
                final long freeSpace = new File(fileTreeModel.toString()).getFreeSpace();
                if (totalJobSize > freeSpace) {
                    LOG.info("GET Job failed:Restoration directory does not have enough space");
                    ds3Common.getDeepStorageBrowserPresenter().logText(
                            StringBuilderUtil.getJobFailedMessage(resourceBundle.getString("getJobFailed"),
                                    ds3Client.getConnectionDetails().getEndpoint(),
                                    resourceBundle.getString("notEnoughSpace"), null), LogType.ERROR);
                    Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("notEnoughSpaceAlert"), Alert.AlertType.ERROR);
                } else {
                    job = getJob(filteredNode, objects, fileMap, folderMap, totalJobSize);
                    updateMessage(StringBuilderUtil.transferringTotalJobString(FileSizeFormat.getFileSizeType(totalJobSize),
                            fileTreeModel.toString()).toString());
                    //Change Priority
                    if (!Guard.isStringNullOrEmpty(jobPriority)) {
                        ds3Client.modifyJobSpectraS3(new ModifyJobSpectraS3Request(job.getJobId()).withPriority(Priority.valueOf(jobPriority)));
                    }
                    final AtomicLong totalSent = addDataTransferListener(totalJobSize);
                    //Execute when job completed
                    job.attachObjectCompletedListener(obj -> {
                        if (duplicateFileMap.get(Paths.get(fileTreeModel + StringConstants.FORWARD_SLASH + obj))
                                != null && duplicateFileMap.get(Paths.get(fileTreeModel
                                + StringConstants.FORWARD_SLASH + obj)).equals(true)) {
                            ds3Common.getDeepStorageBrowserPresenter().logText(
                                    resourceBundle.getString("fileOverridden")
                                            + StringConstants.SPACE + obj + StringConstants.SPACE
                                            + resourceBundle.getString("to")
                                            + StringConstants.SPACE + fileTreeModel, LogType.SUCCESS);
                        } else {
                            getTransferRates(jobStartTimeInstant, totalSent, totalJobSize, obj, fileTreeModel.toString());
                            ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.objectSuccessfullyTransferredString(obj, fileTreeModel.toString(), DateFormat.formatDate(new Date()), null).toString(), LogType.SUCCESS);

                        }
                    });
                    //get meta data saved on server
                    LOG.info("Registering metadata receiver");
                    final MetadataReceivedListenerImpl metadataReceivedListener = new MetadataReceivedListenerImpl(fileTreeModel.toString());
                    job.attachMetadataReceivedListener((s, metadata) -> {
                        LOG.info("Restoring metadata for {}", s);
                        try {
                            metadataReceivedListener.metadataReceived(s, metadata);
                        } catch (final Exception e) {
                            LOG.error("Error in metadata receiving", e);
                        }
                    });
                    // check whether chunk are available
                    addWaitingForChunkListener(totalJobSize, fileTreeModel.toString());

                    job.transfer(l -> {
                        final String skipPath = getSkipPath(l, files, duplicateFileMap);
                        if (!Guard.isStringNullOrEmpty(skipPath)) {
                            return new PrefixRemoverObjectChannelBuilder(new FileObjectGetter(fileTreeModel), skipPath)
                                    .buildChannel(l.substring((StringConstants.FORWARD_SLASH + skipPath).length()));
                        } else {
                            return new FileObjectGetter(fileTreeModel).buildChannel(l);
                        }
                    });
                    updateProgress(totalJobSize, totalJobSize);
                    updateMessage(StringBuilderUtil.jobSuccessfullyTransferredString(JobRequestType.GET.toString(),
                            FileSizeFormat.getFileSizeType(totalJobSize), fileTreeModel.toString(),
                            DateFormat.formatDate(new Date()), null, false).toString());
                    updateProgress(totalJobSize, totalJobSize);
                    //Can't assign final.
                    ParseJobInterruptionMap.removeJobID(jobInterruptionStore, jobId.toString(),
                            ds3Client.getConnectionDetails().getEndpoint(), ds3Common.getDeepStorageBrowserPresenter());
                    ds3Common.getDeepStorageBrowserPresenter().logText(
                            StringBuilderUtil.getJobCompleted(totalJobSize, ds3Client).toString(), LogType.SUCCESS);
                }
            } else {
                hostNotAvaialble();
            }

        } catch (final NoSuchElementException e) {
            LOG.error("The job failed to process", e);
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    StringBuilderUtil.getJobFailedMessage(resourceBundle.getString("getJobFailed"),
                            ds3Client.getConnectionDetails().getEndpoint(),
                            resourceBundle.getString("emptyBucketTransfer"), e), LogType.ERROR);
        } catch (final RuntimeException e) {
            isJobFailed = true;
            LOG.error("The job failed to process", e);
            removeJobIdAndUpdateJobsBtn(jobInterruptionStore, jobId);
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    (StringBuilderUtil.getJobFailedMessage(resourceBundle.getString("getJobFailed"),
                            ds3Client.getConnectionDetails().getEndpoint(), StringConstants.EMPTY_STRING, e)), LogType.ERROR);
        } catch (final Exception e) {
            isJobFailed = true;
            LOG.error("The job failed to process", e);
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    StringBuilderUtil.getJobFailedMessage(resourceBundle.getString("getJobFailed"),
                            ds3Client.getConnectionDetails().getEndpoint(), StringConstants.EMPTY_STRING, e), LogType.ERROR);
            updateInterruptedJobsBtn(jobInterruptionStore, jobId);
        }
    }


    private Ds3ClientHelpers.Job getJob(final List<Ds3TreeTableValueCustom> filteredNode,
                                        final ImmutableList<Ds3Object> objects,
                                        final ImmutableMap.Builder<String, Path> fileMap,
                                        final ImmutableMap.Builder<String, Path> folderMap,
                                        final long totalJobSize) throws Exception {
        final ImmutableList<String> buckets = getBuckets(filteredNode);
        final String bucket = buckets.stream().findFirst().get();
        final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(ds3Client, 100);
        final Ds3ClientHelpers.Job job = helpers.startReadJob(bucket, objects).withMaxParallelRequests(maximumNumberOfParallelThreads);
        if (jobId == null) {
            setJobID(job.getJobId());
        }
        ParseJobInterruptionMap.saveValuesToFiles(jobInterruptionStore, fileMap.build(), folderMap.build(),
                ds3Client.getConnectionDetails().getEndpoint(), jobId, totalJobSize, fileTreeModel.toString(),
                "GET", bucket);
        return job;
    }

    private ImmutableList<String> getBuckets(final List<Ds3TreeTableValueCustom> filteredNode) {
        return filteredNode.stream().map(Ds3TreeTableValueCustom::getBucketName).distinct().collect(GuavaCollectors.immutableList());
    }

    /**
     * Get list of directories.
     *
     * @param list   list
     * @param isFile true for list of files
     * @return List of directories
     */
    private ImmutableList<Ds3TreeTableValueCustom> getDirectoriesOrFiles(final List<Ds3TreeTableValueCustom> list,
                                                                         final boolean isFile) {
        if (isFile) {
            return list.stream().filter(value ->
                    value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        } else {
            return list.stream().filter(value ->
                    !value.getType().equals(Ds3TreeTableValue.Type.File)).collect(GuavaCollectors.immutableList());
        }
    }

    private String getSkipPath(final String l, final ImmutableList<Ds3TreeTableValueCustom> files,
                               final Map<Path, Boolean> duplicateFileMap) {
        final File file = new File(l);
        String skipPath = null;
        if (new File(fileTreeModel + StringConstants.FORWARD_SLASH + l).exists()) {
            duplicateFileMap.put(Paths.get(fileTreeModel + StringConstants.FORWARD_SLASH + l), true);
        }
        if (Guard.isMapNullOrEmpty(map) && file.getParent() != null) {
            skipPath = file.getParent();
        }
        if (Guard.isNullOrEmpty(files)) {
            final Path skipPath1 = map.get(file.toPath());
            if (skipPath1 != null) {
                skipPath = skipPath1.toString();
            }
        }
        return skipPath;
    }

    /**
     * Get Ds3Object from list
     *
     * @param filteredNode list of objects having size nonzero
     * @return Ds3Object list
     */
    public ImmutableList<Ds3Object> getDS3Object(final List<Ds3TreeTableValueCustom> filteredNode) {
        return filteredNode.stream().map(pair -> {
            try {
                return new Ds3Object(pair.getFullName(), pair.getSize());
            } catch (final SecurityException e) {
                LOG.error("Exception while creation directories ", e);
                ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.getJobFailedMessage(resourceBundle.getString("getJobFailed"), ds3Client.getConnectionDetails().getEndpoint(), StringConstants.EMPTY_STRING, e), LogType.ERROR);
                return null;
            }
        }).filter(Objects::nonNull).collect(GuavaCollectors.immutableList());
    }

    /**
     * create map of folders from list of objects
     *
     * @param directories list of objects
     * @return map of folders
     */
    public ImmutableMap.Builder<String, Path> createFolderMap(final ImmutableList<Ds3TreeTableValueCustom> directories) {
        final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
        directories.forEach(value -> {
            if (null != value && value.getType().equals(Ds3TreeTableValue.Type.Directory)) {
                if (Paths.get(value.getFullName()).getParent() != null) {
                    folderMap.put(value.getFullName(), Paths.get(value.getFullName()).getParent());
                    addAllDescendants(value, nodes, Paths.get(value.getFullName()).getParent());
                } else {
                    addAllDescendants(value, nodes, null);
                    folderMap.put(value.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
                }
            }
        });
        return folderMap;
    }

    /**
     * Create map of files from the selected objects
     *
     * @param files selected objects
     * @return map of files
     */
    public ImmutableMap.Builder<String, Path> createFileMap(final ImmutableList<Ds3TreeTableValueCustom> files) {
        final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
        files.forEach(file -> {
            fileMap.put(file.getFullName(), Paths.get(StringConstants.FORWARD_SLASH));
            nodes.add(file);
        });
        return fileMap;
    }

    /**
     * Get all descendants
     *
     * @param value item needs to be transferred
     * @param nodes filtered nodes
     * @param path  path
     */
    public Map<Path, Path> addAllDescendants(final Ds3TreeTableValueCustom value, final List nodes, final Path path) {
        try {
            LOG.info("adding up all descendants");
            final GetBucketRequest request = new GetBucketRequest(value.getBucketName()).withDelimiter("/").withMaxKeys(1000000);
            if (value.getType() != Ds3TreeTableValue.Type.Bucket) {
                request.withPrefix(value.getFullName());
            }
            final GetBucketResponse bucketResponse = ds3Client.getBucket(request);
            final ImmutableList<Ds3TreeTableValueCustom> files = bucketResponse.getListBucketResult()
                    .getObjects().stream()
                    .map(f -> new Ds3TreeTableValueCustom(value.getBucketName(), f.getKey(), Ds3TreeTableValue.Type.File, f.getSize(), "", f.getOwner().getDisplayName(), false)
                    ).collect(GuavaCollectors.immutableList());
            files.forEach(i -> {
                nodes.add(i);
                map.put(Paths.get(i.getFullName()), path);
            });
            final ImmutableList<Ds3TreeTableValueCustom> directoryValues = bucketResponse.getListBucketResult()
                    .getCommonPrefixes().stream().map(CommonPrefixes::getPrefix)
                    .map(c -> new Ds3TreeTableValueCustom(value.getBucketName(), c, Ds3TreeTableValue.Type.Directory, 0, "", "--", false))
                    .collect(GuavaCollectors.immutableList());
            directoryValues.forEach(i -> addAllDescendants(i, nodes, path));

        } catch (final IOException e) {
            LOG.error("Unable to add descendants", e);
           ds3Common.getDeepStorageBrowserPresenter().logText(StringBuilderUtil.getJobFailedMessage("GET Job Cancelled", ds3Client.getConnectionDetails().getEndpoint(), "", e), LogType.ERROR);
        }
        return map;
    }


    public UUID getJobId() {
        return jobId;
    }

    public void setJobID(final UUID jobID) {
        this.jobId = jobID;
    }
}
