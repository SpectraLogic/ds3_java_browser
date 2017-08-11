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

import com.google.common.collect.ImmutableList;
import javafx.util.Pair;

import java.util.*;
import java.util.function.Function;

public class PruningTree<K, V> {
    private final Map<K, PruningTree<K, V>> children = new HashMap<>();
    private final V value;

    PruningTree(final V root) {
        this.value = root;
    }

    private boolean isLeaf() {
        return value != null;
    }

    public void add(final K[] path, final V value) {
        if (isLeaf()) {
            return;
        }
        if (path.length == 1) {
            children.put(path[0], new PruningTree<>(value));
        } else {
            final PruningTree<K, V> pruningTree;
            if (children.containsKey(path[0])) {
                pruningTree = children.get(path[0]);
                children.put(path[0], pruningTree);
            } else {
                pruningTree = new PruningTree<>(null);
            }
            pruningTree.add(Arrays.copyOfRange(path, 1, path.length), value);
        }
    }

    public void add(final Pair<K[], V> pair) {
        add(pair.getKey(), pair.getValue());
    }

    public void addAll(final List<K[]> paths, final List<V> list) {
        for (int i = 0; i < list.size(); i++) {
            this.add(paths.get(i), list.get((i)));
        }
    }

    public void addAll(final List<V> list, final Function<V, K[]> f) {
        list.forEach(item -> add(f.apply(item), item));
    }

    public ImmutableList<V> toList() {
        final ImmutableList.Builder<V> builder = new ImmutableList.Builder<>();
        final Stack<PruningTree<K, V>> childStack = new Stack<>();
        childStack.push(this);
        while (!childStack.empty()) {
            final PruningTree<K, V> currentPruningTree = childStack.pop();
            if (currentPruningTree.isLeaf()) {
                builder.add(currentPruningTree.value);
            }
            currentPruningTree.children.forEach((k, v) -> childStack.push(v));
        }
        return builder.build();
    }

}
