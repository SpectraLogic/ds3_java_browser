package com.spectralogic.dsbrowser.gui.components.ds3panel;

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

    private final List<Session> currentSessions = new ArrayList<>();

    private final List<TabPane> currentTabPanes = new ArrayList<>();

    public Ds3Common() {
    }

    public Map<String, TreeItem<Ds3TreeTableValue>> getExpandedNodesInfo() {
        return expandedNodesInfo;
    }

    public List<Session> getCurrentSessions() {
        return currentSessions;
    }

    public List<TabPane> getCurrentTabPanes() {
        return currentTabPanes;
    }
}
