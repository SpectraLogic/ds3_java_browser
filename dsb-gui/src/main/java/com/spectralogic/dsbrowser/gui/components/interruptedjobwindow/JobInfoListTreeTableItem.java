/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetObjectsWithFullDetailsSpectraS3Response;
import com.spectralogic.ds3client.models.DetailedS3Object;
import com.spectralogic.ds3client.models.S3ObjectType;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JobInfoListTreeTableItem extends TreeItem<JobInfoModel> {

    private final static Logger LOG = LoggerFactory.getLogger(JobInfoListTreeTableItem.class);

    private final String jobId;
    private final JobInfoModel modelValue;
    private boolean accessedChildren = false;
    private final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap;
    private final boolean leaf;
    private final Session session;
    private final Workers workers;

    public JobInfoListTreeTableItem(final String jobId, final JobInfoModel value, final Map<String, FilesAndFolderMap> stringFilesAndFolderMapMap, final Session session, final Workers workers) {
        super(value);
        this.jobId = jobId;
        this.modelValue = value;
        this.stringFilesAndFolderMapMap = stringFilesAndFolderMapMap;
        this.leaf = isLeaf(value);
        this.setGraphic(getIcon(value.getType()));
        this.session = session;
        this.workers = workers;
    }

    private static Node getIcon(final JobInfoModel.Type type) {
        switch (type) {
            case JOBID:
                return getImageView(ImageURLs.JOB_ICON);
            case Directory:
                return getImageView(ImageURLs.FOLDER_ICON);
            case File:
                return getImageView(ImageURLs.FILE_ICON);
            default:
                return null;
        }
    }

    private static Node getImageView(final String url) {
        final ImageView imageView = new ImageView(url);
        imageView.setFitHeight(15);
        imageView.setFitWidth(15);
        return imageView;
    }


    private boolean isLeaf(final JobInfoModel value) {
        return value.getType() == JobInfoModel.Type.File;
    }

    @Override
    public ObservableList<TreeItem<JobInfoModel>> getChildren() {
        if (!accessedChildren) {
            buildChildren(super.getChildren());
            accessedChildren = true;
        }
        return super.getChildren();
    }

    private void buildChildren(final ObservableList<TreeItem<JobInfoModel>> children) {
        final Map.Entry<String, FilesAndFolderMap> stringFilesAndFolderMapEntry = stringFilesAndFolderMapMap.entrySet().stream().filter(i -> i.getKey().equals(jobId)).findFirst().get();
        final FilesAndFolderMap value = stringFilesAndFolderMapEntry.getValue();
        final List<JobInfoListTreeTableItem> list = new ArrayList<>();
        final Node previousGraphics = super.getGraphic();
        final ImageView processImage = new ImageView(ImageURLs.CHILD_LOADER);
        processImage.setFitHeight(20);
        processImage.setFitWidth(20);
        super.setGraphic(processImage);
        final Task getChildren = new Task() {
            @Override
            protected Optional<Object> call() throws Exception {
                if (modelValue.getType().equals(JobInfoModel.Type.JOBID)) {
                    value.getFiles().entrySet().stream().forEach(i -> {
                        long size = 0;
                        try {
                            size = Files.size(i.getValue());
                        } catch (final IOException e) {
                            LOG.error("Unable to get children", e);
                        }
                        final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(i.getKey(), modelValue.getJobId(), StringConstants.TWO_DASH, size, i.getValue().toString(), modelValue.getJobType(), StringConstants.TWO_DASH, JobInfoModel.Type.File, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                        list.add(jobListTreeTableItem);
                    });
                    value.getFolders().entrySet().stream().forEach(i -> {
                        final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(i.getKey(), modelValue.getJobId(), StringConstants.TWO_DASH, 0, i.getValue().toString(), modelValue.getJobType(), StringConstants.TWO_DASH, JobInfoModel.Type.Directory, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                        list.add(jobListTreeTableItem);
                    });

                } else {
                    if (modelValue.getJobType().equals("PUT")) {
                        final File[] files;
                        if (value.getFolders().entrySet().stream().anyMatch(i -> i.getKey().equals(modelValue.getName()))) {
                            final Map.Entry<String, Path> stringPathEntry = value.getFolders().entrySet().stream().filter(i -> i.getKey().equals(modelValue.getName())).findFirst().get();
                            files = new File(stringPathEntry.getValue().toString()).listFiles();
                        } else {
                            files = new File(modelValue.getFullPath()).listFiles();
                        }
                        for (final File file : files) {
                            final FileTreeModel.Type type = getRootType(file);
                            if (type == FileTreeModel.Type.Directory) {
                                final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(file.getName(), modelValue.getJobId(), StringConstants.TWO_DASH, 0, file.getPath(), modelValue.getJobType(), StringConstants.TWO_DASH, JobInfoModel.Type.Directory, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                                list.add(jobListTreeTableItem);
                            } else {
                                long size = 0;
                                try {
                                    size = Files.size(file.toPath());
                                } catch (final IOException e) {
                                    LOG.error("file not found", e);
                                }
                                final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(file.getName(), modelValue.getJobId(), StringConstants.TWO_DASH, size, file.getPath(), modelValue.getJobType(), StringConstants.TWO_DASH, JobInfoModel.Type.File, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                                list.add(jobListTreeTableItem);
                            }
                        }
                    } else {
                        final GetObjectsWithFullDetailsSpectraS3Request request = new GetObjectsWithFullDetailsSpectraS3Request().withBucketId(modelValue.getBucket()).withIncludePhysicalPlacement(true);
                        request.withName(modelValue.getName() + StringConstants.PERCENT).withFolder(modelValue.getName());
                        try {
                            final GetObjectsWithFullDetailsSpectraS3Response objectsWithFullDetailsSpectraS3 = session.getClient().getObjectsWithFullDetailsSpectraS3(request);
                            final List<DetailedS3Object> detailedS3Objects = objectsWithFullDetailsSpectraS3.getDetailedS3ObjectListResult().getDetailedS3Objects();
                            detailedS3Objects.forEach(file -> {
                                if (file.getType() == S3ObjectType.FOLDER) {
                                    final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(file.getName(), modelValue.getJobId(), StringConstants.TWO_DASH, 0, file.getName(), modelValue.getJobType(), StringConstants.TWO_DASH, JobInfoModel.Type.Directory, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                                    list.add(jobListTreeTableItem);
                                } else {
                                    final long size = file.getSize();
                                    final JobInfoListTreeTableItem jobListTreeTableItem = new JobInfoListTreeTableItem(jobId, new JobInfoModel(file.getName(), modelValue.getJobId(), StringConstants.TWO_DASH, size, file.getName(), modelValue.getJobType(), "--", JobInfoModel.Type.File, StringConstants.TWO_DASH, modelValue.getBucket()), stringFilesAndFolderMapMap, session, workers);
                                    list.add(jobListTreeTableItem);
                                }
                            });
                        } catch (final IOException e) {
                            LOG.error("Failed to get full object details", e);
                        }
                    }
                }
                return Optional.empty();
            }
        };
        workers.execute(getChildren);
        getChildren.setOnSucceeded(event -> {
            super.setGraphic(previousGraphics);
            children.setAll(list);
        });

    }

    private FileTreeModel.Type getRootType(final File file) {
        if (Platform.isWin()) {
            if (file.isDirectory()) {
                return FileTreeModel.Type.Directory;
            } else if (file.isFile()) {
                return FileTreeModel.Type.File;
            } else {
                return FileTreeModel.Type.Media_Device;
            }
        } else if (file.isFile()) {
            return FileTreeModel.Type.File;
        } else {
            return FileTreeModel.Type.Directory;
        }
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }
}
