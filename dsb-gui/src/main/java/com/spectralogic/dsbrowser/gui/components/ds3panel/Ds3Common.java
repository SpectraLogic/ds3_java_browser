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

package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTablePresenter;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.stage.Window;

import java.util.HashMap;
import java.util.Map;

public class Ds3Common {

    private final Map<String, TreeItem<Ds3TreeTableValue>> expandedNodesInfo = new HashMap<>();

    private Session currentSession;

    private TabPane currentTabPane;

    private Session sessionOfClosedTab;

    private TreeTableView<FileTreeModel> localTreeTableView;

    private TreeTableView<Ds3TreeTableValue> ds3TreeTableView;

    private Label localFilePathIndicator;

    private DeepStorageBrowserPresenter deepStorageBrowserPresenter;

    private Ds3PanelPresenter ds3PanelPresenter;
    private LocalFileTreeTablePresenter localFileTreeTablePresenter;

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

    public TreeTableView<FileTreeModel> getLocalTreeTableView() {
        return localTreeTableView;
    }

    public void setLocalTreeTableView(final TreeTableView<FileTreeModel> localTreeTableView) {
        this.localTreeTableView = localTreeTableView;
    }

    public Label getLocalFilePathIndicator() {
        return localFilePathIndicator;
    }

    public void setLocalFilePathIndicator(final Label localFilePathIndicator) {
        this.localFilePathIndicator = localFilePathIndicator;
    }

    public  TreeTableView<Ds3TreeTableValue> getDs3TreeTableView() {
        return ds3TreeTableView;
    }

    public void setDs3TreeTableView(final  TreeTableView<Ds3TreeTableValue> ds3TreeTableView) {
        this.ds3TreeTableView = ds3TreeTableView;
    }

    public void setLocalFileTreeTablePresenter(final LocalFileTreeTablePresenter localFileTreeTablePresenter) {
        this.localFileTreeTablePresenter = localFileTreeTablePresenter;
    }

    public LocalFileTreeTablePresenter getLocalFileTreeTablePresenter() {
        return localFileTreeTablePresenter;
    }

    public Window getWindow() {
        return this.getLocalTreeTableView().getScene().getWindow();
    }
}
