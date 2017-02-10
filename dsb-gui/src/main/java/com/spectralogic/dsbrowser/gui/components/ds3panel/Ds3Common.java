package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ds3Common {

    private final Map<String, TreeItem<Ds3TreeTableValue>> expandedNodesInfo = new HashMap<>();

    private Session currentSession;

    private TabPane currentTabPane;

    private Session sessionOfClosedTab;

    private TreeTableView localTreeTableView;

    private Label localFilePathIndicator;

    private DeepStorageBrowserPresenter deepStorageBrowserPresenter = new DeepStorageBrowserPresenter();

    private Ds3PanelPresenter ds3PanelPresenter;

    public Ds3Common() {
    }

    public Ds3PanelPresenter getDs3PanelPresenter() {
        return ds3PanelPresenter;
    }

    public void setDs3PanelPresenter(final Ds3PanelPresenter ds3PanelPresenter) {
        this.ds3PanelPresenter = ds3PanelPresenter;
    }

    public Map<String, TreeItem<Ds3TreeTableValue>> getExpandedNodesInfo() {
        return expandedNodesInfo;
    }


    public DeepStorageBrowserPresenter getDeepStorageBrowserPresenter() {
        return deepStorageBrowserPresenter;
    }

    public void setDeepStorageBrowserPresenter(final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

    public Session getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(final Session currentSession) {
        this.currentSession = currentSession;
    }

    public TabPane getCurrentTabPane() {
        return currentTabPane;
    }

    public void setCurrentTabPane(final TabPane currentTabPane) {
        this.currentTabPane = currentTabPane;
    }

    public Session getSessionOfClosedTab() {
        return sessionOfClosedTab;
    }

    public void setSessionOfClosedTab(final Session sessionOfClosedTab) {
        this.sessionOfClosedTab = sessionOfClosedTab;
    }

    public TreeTableView getLocalTreeTableView() {
        return localTreeTableView;
    }

    public void setLocalTreeTableView(final TreeTableView localTreeTableView) {
        this.localTreeTableView = localTreeTableView;
    }

    public Label getLocalFilePathIndicator() {
        return localFilePathIndicator;
    }

    public void setLocalFilePathIndicator(final Label localFilePathIndicator) {
        this.localFilePathIndicator = localFilePathIndicator;
    }
}
