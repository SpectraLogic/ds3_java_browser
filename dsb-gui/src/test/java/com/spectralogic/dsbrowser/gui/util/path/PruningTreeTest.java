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
import com.spectralogic.dsbrowser.gui.util.treeItem.PruningTree;
import javafx.util.Pair;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.CoreMatchers.is;

public class PruningTreeTest {
    private PruningTree<String, String> pt;

    @Before
    public void setup() {
        pt = new PruningTree<>();
    }

    @Test
    public void emptyTreeIsEmpty() {
        assertThat(pt.toList().size(), is(0));
    }

    @Test
    public void singleItemTest() {
        pt.add("foo".split("/"), "bar");
        assertThat(pt.toList(), containsInAnyOrder("bar"));
    }

    @Test
    public void secondItemRemovesFirstItem() {
        pt.add("foo/bar/baz".split("/"), "baz");
        pt.add("foo".split("/"), "foo");
        assertThat(pt.toList(), containsInAnyOrder("foo"));
    }

    @Test
    public void secondItemShouldNotRemoveFirstItem() {
        pt.add("foo/bar".split("/"), "bar");
        assertThat(pt.toList(), containsInAnyOrder("bar"));
        pt.add("foo/baz".split("/"), "baz");
        assertThat(pt.toList(), containsInAnyOrder("baz", "bar"));
    }

    @Test
    public void addWithPair() {
        pt.add(new Pair<>("foo".split("/"), "foo"));
        assertThat(pt.toList(), containsInAnyOrder("foo"));
    }

    @Test
    public void addFromList() {
        final ImmutableList<String[]> stringsPaths = ImmutableList.of("foo".split("/"), "foo/bar".split("/"));
        final ImmutableList<String> strings = ImmutableList.of("foo", "bar");
        pt.addAll(stringsPaths, strings);
        assertThat(pt.toList(), containsInAnyOrder("foo"));
    }

    @Test
    public void functionalTest() {
        final ImmutableList<String> stringPaths = ImmutableList.of("foo/bar", "foo/baz");
        pt.addAll(stringPaths, s -> s.split("/"));
        assertThat(pt.toList(), containsInAnyOrder("foo/bar","foo/baz"));
    }
}
