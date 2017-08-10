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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Trie<T> {
    private final Map<String, Trie<T>> children = new HashMap<>();
    private final T value;

    public Trie(final T root) {
        this.value = root;
    }

    private boolean isLeaf() {
        return value != null;
    }

    public void add(final String[] path, final T value) {
        if (isLeaf()) {
            return;
        }
        if (path.length == 1) {
            children.put(path[0], new Trie<>(value));
        } else {
            final Trie<T> trie;
            if (children.containsKey(path[0])) {
                trie = children.get(path[0]);
                children.put(path[0], trie);
            } else {
                trie = new Trie<>(null);
            }
            trie.add(Arrays.copyOfRange(path, 1, path.length), value);
        }
    }

    public void addAll(final List<String[]> paths, final List<T> list) {
        for (int i = 0; i < list.size(); i++) {
            this.add(paths.get(i), list.get((i)));
        }
    }

    public void addAll(final List<T> list,  final Function<T, String[]> f) {
        list.forEach(item -> add(f.apply(item), item));
    }


    public ImmutableList<T> toList() {
        final ImmutableList.Builder<T> builder = new ImmutableList.Builder<>();
        getChildren(builder);
        return builder.build();
    }

    private void getChildren(final ImmutableList.Builder<T> builder) {
        if (value != null) {
            builder.add(value);
        }
        if (!children.isEmpty()) {
            children.forEach((k, v) -> v.getChildren(builder));
        }
    }


}
