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

package com.spectralogic.dsbrowser.gui.services.tasks;

import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.commands.GetBucketResponse;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.ds3client.models.ListBucketResult;
import com.spectralogic.ds3client.models.common.CommonPrefixes;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.FilesCountModel;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GetNoOfItemsTask extends Task<FilesCountModel> {

    private final static Logger LOG = LoggerFactory.getLogger(GetNoOfItemsTask.class);

    private final Ds3Common ds3Common;
    private ListBucketResult listBucketResult;
    private final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems;

    public GetNoOfItemsTask(final Ds3Common ds3Common,
                            final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        this.ds3Common = ds3Common;
        this.selectedItems = selectedItems;
    }

    @Override
    protected FilesCountModel call() throws Exception {

        final FilesCountModel filesCountModelTotal = new FilesCountModel();

        //return null if user has selected child and parent both
        if (!selectedItems.isEmpty()) {
            for (int i = 0; i < selectedItems.size(); i++) {
                for (int j = 0; j < selectedItems.size(); j++) {
                    if (null != selectedItems.get(i).getValue() && selectedItems.get(i).getValue().getType().equals(Ds3TreeTableValue.Type.Bucket)) {
                        if (i != j && selectedItems.get(j).getValue().getBucketName()
                                .equals(selectedItems.get(i).getValue().getBucketName())) {
                            return null;
                        }
                    } else if (i != j && selectedItems.get(j).getValue().getFullName().contains(selectedItems.get(i).getValue().getFullName())
                            && selectedItems.get(j).getValue().getBucketName().equals(selectedItems.get(i).getValue().getBucketName())) {
                        return null;
                    }
                }
            }
            selectedItems.forEach(item -> {
                if (null != item && null != item.getValue()) {
                    if (item.getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
                        filesCountModelTotal.setNoOfFiles(filesCountModelTotal.getNoOfFiles() + 1);
                        filesCountModelTotal.setTotalCapacity(filesCountModelTotal.getTotalCapacity() + item.getValue().getSize());
                    } else {
                        final GetBucketRequest request = new GetBucketRequest(item.getValue().getBucketName()).withDelimiter("/");
                        if (item.getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
                            request.withPrefix(item.getValue().getFullName());
                        }
                        try {
                            final GetBucketResponse bucketResponse = ds3Common.getCurrentSession().getClient().getBucket(request);
                            listBucketResult = bucketResponse.getListBucketResult();
                            if (bucketResponse.getListBucketResult().getObjects().size() > 0) {
                                if (bucketResponse.getListBucketResult().getObjects().get(0).getKey().equals(item.getValue().getFullName()) && bucketResponse.getListBucketResult().getObjects().get(0).getETag() == null) {
                                    bucketResponse.getListBucketResult().getObjects().remove(0);
                                }
                            }
                            FilesCountModel filesCountModel = new FilesCountModel();
                            filesCountModel = getNoOfItemsInFolder(listBucketResult, filesCountModel, GetNoOfItemsTask.this, item.getValue().getBucketName());
                            filesCountModelTotal.setNoOfFolders(filesCountModelTotal.getNoOfFolders() + filesCountModel.getNoOfFolders());
                            filesCountModelTotal.setNoOfFiles(filesCountModelTotal.getNoOfFiles() + filesCountModel.getNoOfFiles());
                            filesCountModelTotal.setTotalCapacity(filesCountModelTotal.getTotalCapacity() + filesCountModel.getTotalCapacity());
                        } catch (final IOException e) {
                            LOG.error("Exception while getting bucket list", e);
                        }
                    }
                }
            });
        }
        return filesCountModelTotal;
    }

    /**
     * To get the count of item in a folder
     *
     * @param listBucketResult listBucketResult
     * @param filesCountModel  filesCountModel
     * @param getNoOfItemsTask getNoOfItemsTask
     * @param bucketName       bucketName
     * @return FilesCountModel
     */
    private FilesCountModel getNoOfItemsInFolder(final ListBucketResult listBucketResult, FilesCountModel filesCountModel, final GetNoOfItemsTask getNoOfItemsTask, final String bucketName) {
        try {
            if (!getNoOfItemsTask.isCancelled()) {
                final int noOfFiles = filesCountModel.getNoOfFiles() + listBucketResult.getObjects().size();
                final int noOfFolders = filesCountModel.getNoOfFolders() + listBucketResult.getCommonPrefixes().size();
                final long sum = listBucketResult.getObjects().stream().mapToLong(Contents::getSize).sum();
                final long totalCapacity = filesCountModel.getTotalCapacity() + sum;
                filesCountModel.setNoOfFiles(noOfFiles);
                filesCountModel.setNoOfFolders(noOfFolders);
                filesCountModel.setTotalCapacity(totalCapacity);
                if (!Guard.isNullOrEmpty(listBucketResult.getCommonPrefixes())) {
                    for (final CommonPrefixes pref : listBucketResult.getCommonPrefixes()) {
                        final GetBucketRequest request = new GetBucketRequest(bucketName).withDelimiter("/");
                        request.withPrefix(pref.getPrefix());
                        final GetBucketResponse bucketResponse = ds3Common.getCurrentSession().getClient().getBucket(request);
                        final ListBucketResult listBucketResultLocal = bucketResponse.getListBucketResult();
                        if (bucketResponse.getListBucketResult().getObjects().size() > 0) {
                            if (bucketResponse.getListBucketResult().getObjects().get(0).getKey().equals(pref.getPrefix()) && bucketResponse.getListBucketResult().getObjects().get(0).getETag() == null) {
                                bucketResponse.getListBucketResult().getObjects().remove(0);
                            }
                        }
                        filesCountModel = getNoOfItemsInFolder(listBucketResultLocal, filesCountModel, getNoOfItemsTask, bucketName);
                    }
                }
            }
        } catch (final IOException e) {
            LOG.error("Exception while getting count of items inside a folder", e);
        }

        return filesCountModel;
    }
}

