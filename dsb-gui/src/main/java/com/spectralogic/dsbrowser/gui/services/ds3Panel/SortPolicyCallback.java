/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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

package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.util.BaseTreeModel;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

public class SortPolicyCallback implements javafx.util.Callback {
    private final static Logger LOG = LoggerFactory.getLogger(SortPolicyCallback.class);


    private final TreeTableView<BaseTreeModel> ds3TreeTable;

    public SortPolicyCallback(final TreeTableView<?> ds3TreeTable) {
        this.ds3TreeTable = (TreeTableView<BaseTreeModel>) ds3TreeTable;
    }

    @Override
    public Object call(final Object param) {
        try {
            if (param instanceof TreeTableView) {
                final TreeTableView<BaseTreeModel> param1 = (TreeTableView<BaseTreeModel>) param;
                final Comparator<TreeItem<BaseTreeModel>> comparator = (o1, o2) -> {
                    if (param1.getComparator() == null) {
                        return 0;
                    } else {
                        return param1.getComparator()
                                .compare(o1, o2);
                    }
                };
                if (ds3TreeTable.getRoot() != null) {
                    final ImmutableList<TreeItem<BaseTreeModel>> loaderList = ds3TreeTable.getRoot().getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(BaseTreeModel.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                    final ImmutableList<TreeItem<BaseTreeModel>> collect = ds3TreeTable.getRoot().getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(BaseTreeModel.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                    final ObservableList<TreeItem<BaseTreeModel>> treeItems = FXCollections.observableArrayList(collect);
                    FXCollections.sort(treeItems, comparator);

                    ds3TreeTable.getRoot().getChildren().removeAll(ds3TreeTable.getRoot().getChildren());
                    ds3TreeTable.getRoot().getChildren().addAll(treeItems);
                    final Optional<TreeItem<BaseTreeModel>> first = loaderList.stream().findFirst();
                    first.ifPresent(baseTreeModelTreeItem -> ds3TreeTable.getRoot().getChildren().add(baseTreeModelTreeItem));

                    treeItems.forEach(i -> {
                        if (i.isExpanded()) {
                            final Optional<TreeTableColumn<BaseTreeModel, ?>> firstElement = param1.getSortOrder().stream().findFirst();
                            if (firstElement.isPresent())
                                sortChild(i, comparator, firstElement.get().getText());
                            else
                                sortChild(i, comparator, StringConstants.EMPTY_STRING);
                        }
                    });
                    if (param1.getSortOrder().stream().findFirst().isPresent()) {
                        if (!param1.getSortOrder().stream().findFirst().get().getText().equals(StringConstants.TYPE)) {
                            FXCollections.sort(ds3TreeTable.getRoot().getChildren(), Comparator.comparing(t -> t.getValue().getType().toString()));
                        }

                    }
                }

            }
        } catch (final Exception e) {
            LOG.error("Unable to sort tree", e);
        }
        return true;
    }

    private void sortChild(final TreeItem<BaseTreeModel> o1, final Comparator<TreeItem<BaseTreeModel>> comparator, final String type) {
        try {
            if (comparator != null) {
                final ImmutableList<TreeItem<BaseTreeModel>> loaderList = o1.getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(BaseTreeModel.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                final ImmutableList<TreeItem<BaseTreeModel>> collect = o1.getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(BaseTreeModel.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                final ObservableList<TreeItem<BaseTreeModel>> treeItems = FXCollections.observableArrayList(collect);
                treeItems.forEach(i -> {
                    if (i.isExpanded())
                        sortChild(i, comparator, type);
                });
                FXCollections.sort(treeItems, comparator);
                o1.getChildren().removeAll(o1.getChildren());
                o1.getChildren().addAll(treeItems);
                final Optional<TreeItem<BaseTreeModel>> first = loaderList.stream().findFirst();

                first.ifPresent(baseTreeModelTreeItem -> o1.getChildren().add(baseTreeModelTreeItem));

                if (!type.equals(StringConstants.TYPE)) {
                    FXCollections.sort(o1.getChildren(), Comparator.comparing(t -> t.getValue().getType().toString()));
                }
            }
        } catch (final Exception e) {
            LOG.error("Unable to sort", e);
        }
    }
}
