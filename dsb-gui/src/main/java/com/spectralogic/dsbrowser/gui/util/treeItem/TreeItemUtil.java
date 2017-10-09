/* ****************************************************************************
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

package com.spectralogic.dsbrowser.gui.util.treeItem;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import javafx.scene.control.TreeItem;

import java.util.List;

public final class TreeItemUtil {
    public static List<TreeItem<Ds3TreeTableValue>> minimumPaths(final List<TreeItem<Ds3TreeTableValue>> treeItems)  {
       final PruningTree<String,TreeItem<Ds3TreeTableValue>> pruningTree = new PruningTree<>();
       pruningTree.addAll(treeItems, t -> t.getValue().getFullPath().split("/"));
       return pruningTree.toList();
    }

    public static String pathParent(final String string) {
       final String truncatedPath;
       if(endsinSlash(string)) {
            truncatedPath = string.substring(0, string.lastIndexOf('/'));
       } else {
           truncatedPath = string;
       }
       final int lastIndex = truncatedPath.lastIndexOf('/');
       if(lastIndex < 1) {
           return "/";
       }
       return truncatedPath.substring(0, lastIndex);
    }

    public static boolean endsinSlash(final String string) {
        return string.endsWith("/");
    }
}