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

package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.GetBucketTask;
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.DateTimeUtils;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import com.spectralogic.dsbrowser.util.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.Node;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Ds3TreeTableItem extends TreeItem<Ds3TreeTableValue> {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTableItem.class);
    private final static ImmutableList<BaseTreeModel.Type> leafs = ImmutableList.of(BaseTreeModel.Type.File, BaseTreeModel.Type.Loader);

    private final String bucket;
    private final Session session;
    private final Ds3TreeTableValue ds3Value;
    private final Workers workers;
    private final Ds3Common ds3Common;
    private boolean accessedChildren = false;
    private final DateTimeUtils dateTimeUtils;
    private TreeTableView ds3TreeTable;
    private final LoggingService loggingService;
    private final BaseTreeModel.Type type;

    public Ds3TreeTableItem(final String bucket,
            final Session session,
            final Ds3TreeTableValue value,
            final Workers workers,
            final Ds3Common ds3Common,
            final DateTimeUtils dateTimeUtils,
            final LoggingService loggingService) {
        super(value);
        this.bucket = bucket;
        this.session = session;
        this.ds3Value = value;
        this.workers = workers;
        this.dateTimeUtils = dateTimeUtils;
        this.ds3Common = ds3Common;
        this.setGraphic(getIcon(value.getType())); // sets the default icon
        this.loggingService = loggingService;
        this.type = value.getType();
    }

    public boolean isAccessedChildren() {
        return accessedChildren;
    }

    private static Node getIcon(final Ds3TreeTableValue.Type type) {
        switch (type) {
            case Bucket:
                return new ImageView(ImageURLs.BUCKET_ICON);
            case Directory:
                return new ImageView(ImageURLs.FOLDER_ICON);
            case File:
                return new ImageView(ImageURLs.FILE_ICON);
            case Loader:
                return new ImageView(ImageURLs.TRANSPARENT_ICON);
            default:
                return null;
        }
    }

    public void setDs3TreeTable(final TreeTableView ds3TreeTable) {
        this.ds3TreeTable = ds3TreeTable;
    }


    public void refresh() {
        if (super.getValue() != null) {
            String path = super.getValue().getFullName();
            if (!super.getValue().getType().equals(Ds3TreeTableValue.Type.Bucket))
                path = super.getValue().getBucketName() + StringConstants.FORWARD_SLASH + path;
            this.ds3Common.getDs3PanelPresenter().getDs3PathIndicatorTooltip().setText(path);
            this.ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText(path);
        }
        final ObservableList<TreeItem<Ds3TreeTableValue>> list = super.getChildren();
        list.remove(0, list.size());
        ds3Common.getDs3PanelPresenter().calculateFiles(ds3Common.getDs3TreeTableView());
        buildChildren(list);
    }

    /**
     * @param ds3TreeTable used to clear all selection in case of load moreused to save bucket selection
     */
    public void loadMore(final TreeTableView ds3TreeTable) {
        this.ds3TreeTable = ds3TreeTable;
        final ObservableList<TreeItem<Ds3TreeTableValue>> list = super.getParent().getChildren();
        buildChildren(list);
    }

    @Override
    public ObservableList<TreeItem<Ds3TreeTableValue>> getChildren() {
        if (!accessedChildren) {
            buildChildren(super.getChildren());
            accessedChildren = true;
        }
        return super.getChildren();
    }

    // query black pearl in the background and then update the main thread after
    private void buildChildren(final ObservableList<TreeItem<Ds3TreeTableValue>> observableList) {
        final Node previousGraphics = super.getGraphic();
        final ImageView processImage = new ImageView(ImageURLs.CHILD_LOADER);
        processImage.setFitHeight(20);
        processImage.setFitWidth(20);
        super.setGraphic(processImage);
        final GetBucketTask getBucketTask = new GetBucketTask(observableList, bucket, session, ds3Value, workers,
                dateTimeUtils, this, ds3TreeTable, ds3Common, loggingService);
        getBucketTask.setOnSucceeded(SafeHandler.logHandle(event -> {
            super.setGraphic(previousGraphics);
            if (ds3Common != null && ds3Common.getDs3PanelPresenter() != null && ds3Common.getDs3TreeTableView() != null) {
                ds3Common.getDs3TreeTableView().setPlaceholder(null);
            }
        }));
        getBucketTask.setOnCancelled(SafeHandler.logHandle(event -> super.setGraphic(previousGraphics)));
        getBucketTask.setOnFailed(SafeHandler.logHandle((WorkerStateEvent event) -> {
            super.setGraphic(previousGraphics);
            loggingService.logMessage("Failed to get bucket " + bucket, LogType.ERROR);
            LOG.error("Failed to get bucket " + bucket, event.getSource().getException());
        }));
        workers.execute(getBucketTask);
    }

    @Override
    public boolean isLeaf() {
        return leafs.contains(type) || getChildren().isEmpty();
    }

}
