package com.spectralogic.dsbrowser.gui.util.path;/*
 * ****************************************************************************
 *    Copyright 2014-2017 Spectra Logic Corporation. All Rights Reserved.
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


import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import javafx.scene.control.TreeItem;

import java.util.List;

public final class PathUtil {
    public static List<TreeItem<Ds3TreeTableValue>> minimumPaths(final List<TreeItem<Ds3TreeTableValue>> treeItems)  {
       final Trie<TreeItem<Ds3TreeTableValue>> trie = new Trie<>(null);
       trie.addAll(treeItems, t -> t.getValue().getFullPath().split("/"));
       return trie.toList();
    }
}
