package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableItem;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.GetServiceTask;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RefreshCompleteViewWorker {
    private final static Logger LOG = LoggerFactory.getLogger(RefreshCompleteViewWorker.class);

    public static void refreshCompleteTreeTableView(final Ds3Common ds3Common, final Workers workers) {
        if (ds3Common.getCurrentSession() != null && ds3Common.getCurrentTabPane() != null) {
            final Session session = ds3Common.getCurrentSession();
            ds3Common.getDeepStorageBrowserPresenter().logText("Refreshing session " + session.getSessionName() +
                    StringConstants.SESSION_SEPARATOR +
                    session.getEndpoint(), LogType.INFO);
            @SuppressWarnings("unchecked")
            final TreeTableView<Ds3TreeTableValue> ds3TreeTableView = getTreeTableView(ds3Common);
            ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText(null);
            ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setTooltip(null);
            //invisible column of full path
            if (ds3TreeTableView != null && ds3TreeTableView.getColumns() != null) {
                final TreeTableColumn<Ds3TreeTableValue, ?> ds3TreeTableValueTreeTableColumn = ds3TreeTableView.getColumns().get(1);
                if (ds3TreeTableValueTreeTableColumn != null) {
                    ds3TreeTableValueTreeTableColumn.setVisible(false);
                }
                final TreeItem<Ds3TreeTableValue> selectedRoot = ds3TreeTableView.getRoot();
                if (selectedRoot != null && selectedRoot.getValue() != null) {
                    ds3TreeTableView.getSelectionModel().clearSelection();
                    ds3TreeTableView.setRoot(selectedRoot);
                    ds3TreeTableView.getSelectionModel().select(selectedRoot);
                    ((Ds3TreeTableItem) selectedRoot).refresh();
                    ds3Common.getDs3PanelPresenter().calculateFiles(ds3TreeTableView);
                } else {
                    final TreeItem<Ds3TreeTableValue> rootTreeItem = new TreeItem<>();
                    final GetServiceTask getServiceTask = new GetServiceTask(rootTreeItem.getChildren(), session, workers, ds3Common);
                    workers.execute(getServiceTask);
                    getServiceTask.setOnSucceeded(event -> {
                        ds3TreeTableView.setRoot(rootTreeItem);
                        if (ds3Common.getExpandedNodesInfo().containsKey(session.getSessionName() + StringConstants.SESSION_SEPARATOR +
                                session.getEndpoint())) {
                            rootTreeItem.getChildren().forEach(i ->
                                    i.expandedProperty().addListener((observable, oldValue, newValue) -> {
                                        final BooleanProperty bb = (BooleanProperty) observable;
                                        final TreeItem<Ds3TreeTableValue> bean = (TreeItem<Ds3TreeTableValue>) bb.getBean();
                                        ds3Common.getExpandedNodesInfo().put(session.getSessionName() + "-" + session.getEndpoint(), bean);
                                    }));
                            final TreeItem<Ds3TreeTableValue> item = ds3Common.getExpandedNodesInfo().get(session.getSessionName() + StringConstants.SESSION_SEPARATOR + session.getEndpoint());
                            if (rootTreeItem.getChildren().stream().anyMatch(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName()))) {
                                final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = rootTreeItem.getChildren().stream().filter(i -> i.getValue().getBucketName().equals(item.getValue().getBucketName())).findFirst().get();
                                ds3TreeTableValueTreeItem.setExpanded(false);
                                if (!ds3TreeTableValueTreeItem.isLeaf() && !ds3TreeTableValueTreeItem.isExpanded()) {
                                    ds3TreeTableView.getSelectionModel().select(ds3TreeTableValueTreeItem);
                                    ds3TreeTableValueTreeItem.setExpanded(true);
                                }
                            }
                        }
                        else {
                            ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setText(StringConstants.EMPTY_STRING);
                            ds3Common.getDs3PanelPresenter().getDs3PathIndicator().setTooltip(null);
                        }
                    });
                }
            } else {
                LOG.info("TreeView is null");
            }
        }
    }

    private static TreeTableView<Ds3TreeTableValue> getTreeTableView(final Ds3Common ds3Common) {
        final TabPane ds3SessionTabPane = ds3Common.getCurrentTabPane();
        if (null != ds3SessionTabPane) {
            try {
                final VBox vbox = (VBox) ds3SessionTabPane.getSelectionModel().getSelectedItem().getContent();
                return (TreeTableView<Ds3TreeTableValue>) vbox.getChildren().stream().filter(i -> i instanceof TreeTableView)
                        .findFirst().orElse(null);
            }
            catch (final Exception e) {
                LOG.error("Tab pane is not present", e);
                return null;
            }
        } else {
            LOG.info("TabPane is null");
            return null;
        }
    }

}
