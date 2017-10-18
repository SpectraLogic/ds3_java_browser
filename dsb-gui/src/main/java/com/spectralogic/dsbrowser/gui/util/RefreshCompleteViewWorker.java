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

package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.GetServiceTask;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public final class RefreshCompleteViewWorker {
    private final static Logger LOG = LoggerFactory.getLogger(RefreshCompleteViewWorker.class);

    public static void refreshCompleteTreeTableView(final Ds3Common ds3Common, final Workers workers, final DateTimeUtils dateTimeUtils, final LoggingService loggingService) {
        final Session session = ds3Common.getCurrentSession();
        if (session != null && ds3Common.getCurrentTabPane() != null) {
            loggingService.logMessage("Refreshing session " + session.getSessionName() +
                    StringConstants.SESSION_SEPARATOR +
                    session.getEndpoint(), LogType.INFO);
            @SuppressWarnings("unchecked") final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView(ds3Common);
            final Ds3PanelPresenter ds3PanelPresenter = ds3Common.getDs3PanelPresenter();
            final Label ds3PathIndicator = ds3PanelPresenter.getDs3PathIndicator();
            if (ds3TreeTableView != null && ds3TreeTableView.getColumns() != null) {
                final TreeItem<Ds3TreeTableValue> selectedRoot = ds3TreeTableView.getRoot();
                final TreeTableView.TreeTableViewSelectionModel<Ds3TreeTableValue> selectionModel = ds3TreeTableView.getSelectionModel();
                if (selectedRoot != null && selectedRoot.getValue() != null && selectedRoot.getParent() != null) {
                    refreshCurrentView(ds3Common, ds3TreeTableView, ds3PanelPresenter, selectedRoot, selectionModel);
                } else {
                    refreshBpRootView(ds3Common, workers, dateTimeUtils, loggingService, session, ds3TreeTableView, ds3PathIndicator, selectedRoot, selectionModel);
                }
            } else {
                LOG.info("TreeView is null");
            }
        }
    }

    private static void refreshBpRootView(final Ds3Common ds3Common, final Workers workers, final DateTimeUtils dateTimeUtils, final LoggingService loggingService, final Session session, final TreeTableView<Ds3TreeTableValue> ds3TreeTableView, final Label ds3PathIndicator, final TreeItem<Ds3TreeTableValue> selectedRoot, final TreeTableView.TreeTableViewSelectionModel<Ds3TreeTableValue> selectionModel) {
        if (selectedRoot != null && selectedRoot.getParent() == null) {
            LOG.warn("Parent folder no longer existed, redirecting to the root of the tree");
        }
        final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
        final ObservableList<TreeItem<Ds3TreeTableValue>> children = rootTreeItem.getChildren();
        final GetServiceTask getServiceTask = new GetServiceTask(children, session, workers, ds3Common, dateTimeUtils, loggingService);
        getServiceTask.setOnSucceeded(SafeHandler.logHandle(event -> {
            ds3TreeTableView.setRoot(rootTreeItem);
            final String key = session.getSessionName() + StringConstants.SESSION_SEPARATOR + session.getEndpoint();
            if (ds3Common.getExpandedNodesInfo().containsKey(key)) {
                expandNodes(ds3Common, session, selectionModel, children, key);
            }
        }));
        getServiceTask.setOnFailed(SafeHandler.logHandle(event -> {
            LOG.error("GetServiceTask failed", event);
            loggingService.logMessage("Get Service Task failed", LogType.ERROR);
            ds3TreeTableView.setRoot(null);
        }));
        workers.execute(getServiceTask);
    }

    private static void refreshCurrentView(final Ds3Common ds3Common, final TreeTableView<Ds3TreeTableValue> ds3TreeTableView, final Ds3PanelPresenter ds3PanelPresenter, final TreeItem<Ds3TreeTableValue> selectedRoot, final TreeTableView.TreeTableViewSelectionModel<Ds3TreeTableValue> selectionModel) {
        selectionModel.clearSelection();
        ds3TreeTableView.setRoot(selectedRoot);
        selectionModel.select(selectedRoot);
        ((Ds3TreeTableItem) selectedRoot).refresh();
        ds3PanelPresenter.calculateFiles(ds3TreeTableView);
    }

    private static void expandNodes(final Ds3Common ds3Common, final Session session, final TreeTableView.TreeTableViewSelectionModel<Ds3TreeTableValue> selectionModel, final ObservableList<TreeItem<Ds3TreeTableValue>> children, final String key) {
        final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(key);
        final String bucketName = item.getValue().getBucketName();
        children.forEach(treeItem ->
                treeItem.expandedProperty().addListener((observable, oldValue, newValue) -> {
                    final BooleanProperty bb = (BooleanProperty) observable;
                    final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                    ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                }));
        children.stream()
                .filter(i -> i.getValue().getBucketName().equals(bucketName))
                .findFirst()
                .ifPresent((TreeItem<Ds3TreeTableValue> value) -> {
                    value.setExpanded(false);
                    if (!value.isLeaf() && !value.isExpanded()) {
                        selectionModel.select(value);
                        value.setExpanded(true);
                    }
                });
    }


    @SuppressWarnings("unchecked")
    private static TreeTableView<Ds3TreeTableValue> getTreeTableView(final Ds3Common ds3Common) {
        final TabPane ds3SessionTabPane = ds3Common.getCurrentTabPane();
        if (null != ds3SessionTabPane) {
            try {
                final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
                final Optional<Node> first = vbox.getChildren().stream().filter(i -> i instanceof TreeTableView)
                        .findFirst();
                if (first.isPresent()) {
                    return (TreeTableView<Ds3TreeTableValue>) first.get();
                }
            } catch (final Exception e) {
                LOG.error("Tab pane is not present", e);
            }
        } else {
            LOG.info("TabPane is null");
        }

        return null;
    }

}
