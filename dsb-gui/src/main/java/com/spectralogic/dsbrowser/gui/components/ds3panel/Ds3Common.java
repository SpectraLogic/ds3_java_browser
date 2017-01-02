package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Ds3Common {

    private final Map<String, TreeItem<Ds3TreeTableValue>> expandedNodesInfo = new HashMap<>();

    private final List<Session> currentSession = new ArrayList<>();

    private final List<TabPane> currentTabPane = new ArrayList<>();

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

    public List<Session> getCurrentSession() {
        return currentSession;
    }

    public List<TabPane> getCurrentTabPane() {
        return currentTabPane;
    }

    public DeepStorageBrowserPresenter getDeepStorageBrowserPresenter() {
        return deepStorageBrowserPresenter;
    }

    public void setDeepStorageBrowserPresenter(final DeepStorageBrowserPresenter deepStorageBrowserPresenter) {
        this.deepStorageBrowserPresenter = deepStorageBrowserPresenter;
    }

}
