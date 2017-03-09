/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

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
