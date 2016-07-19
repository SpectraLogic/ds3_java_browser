package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.helpers.Ds3ClientHelpers;
import com.spectralogic.ds3client.models.bulk.Ds3Object;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.FileSizeFormat;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.PathUtil;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJob extends Ds3JobTask {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PutJob.class);

    private final Ds3Client client;
    private final List<File> files;
    private final String bucket;
    private final String targetDir;
    private final DeepStorageBrowserPresenter deepStorageBrowserPresenter;


    public Ds3PutJob(final Ds3Client client, final List<File> files, final String bucket, final String targetDir, final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.client = client;
        this.files = files;
        this.bucket = bucket;
        this.targetDir = targetDir;
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    @Override
    public void executeJob() throws Exception {

        try {
            final String date = DateFormat.formatDate(new Date());
            updateTitle("PUT Job" + client.getConnectionDetails().getEndpoint() + " " + date);
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("PUT Job initiated " + client.getConnectionDetails().getEndpoint(), LogType.INFO);
            });

            final Ds3ClientHelpers helpers = Ds3ClientHelpers.wrap(client);

            final ImmutableList<Path> paths = files.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
            final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();

            files.stream().forEach(path1 -> expandedPaths.put(path1.getParent(), path1));
            directories.stream().forEach(path -> {
                try {
                    partOfDirBuilder.add(path);
                    expandedPaths.putAll(path, Files.walk(path).filter(child -> !Files.isDirectory(child)).collect(GuavaCollectors.immutableList()));
                } catch (final IOException e) {
                    LOG.error("Failed to list files for directory: " + path.toString(), e);
                }
            });

            final ImmutableSet<Path> partOfDir = partOfDirBuilder.build();
            final ImmutableMap.Builder<String, Path> fileMapBuilder = ImmutableMap.builder();
            final ImmutableList<Ds3Object> objects = expandedPaths.build().entries().stream().map(pair -> {
                try {
                    final long size = Files.size(pair.getValue());
                    final String ds3FileName = PathUtil.toDs3Path(targetDir, PathUtil.toDs3Obj(pair.getKey(), pair.getValue(), partOfDir.contains(pair.getKey())));
                    fileMapBuilder.put(ds3FileName, pair.getValue());
                    return new Ds3Object(ds3FileName, size);
                } catch (final IOException e) {
                    LOG.error("Failed to get file size for: " + pair.getValue(), e);
                    return null;
                }
            }).filter(item -> item != null).collect(GuavaCollectors.immutableList());

            final ImmutableMap<String, Path> fileMapper = fileMapBuilder.build();
            final long totalJobSize = objects.stream().mapToLong(Ds3Object::getSize).sum();
            final Ds3ClientHelpers.Job job = helpers.startWriteJob(bucket, objects);

            final AtomicLong totalSent = new AtomicLong(0L);

            job.attachDataTransferredListener(l -> {
                updateProgress(totalSent.getAndAdd(l), totalJobSize);
                updateMessage("Transferring " + FileSizeFormat.getFileSizeType(totalSent.addAndGet(l)) + " of " + FileSizeFormat.getFileSizeType(totalJobSize) + " in " + bucket + "\\" + targetDir);
            });

            job.transfer(s -> FileChannel.open(PathUtil.resolveForSymbolic(fileMapper.get(s)), StandardOpenOption.READ));

            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("PUT Job completed " + client.getConnectionDetails().getEndpoint() + " " + date, LogType.SUCCESS);
            });
        } catch (final Exception e) {
            Platform.runLater(() -> {
                deepStorageBrowserPresenter.logText("PUT Job Failed " + client.getConnectionDetails().getEndpoint() + ". Reason+" + e.toString(), LogType.ERROR);

            });
        }

    }
}
