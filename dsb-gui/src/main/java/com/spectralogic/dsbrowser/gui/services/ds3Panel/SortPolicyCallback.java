package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class SortPolicyCallback implements javafx.util.Callback {
    private final static Logger LOG = LoggerFactory.getLogger(SortPolicyCallback.class);


    private final Ds3Common ds3Common;

    public SortPolicyCallback(final Ds3Common ds3Common) {
        this.ds3Common = ds3Common;
    }

    @Override
    public Object call(Object param) {
        try {
            final TreeTableView<Ds3TreeTableValue> ds3TreeTable = ds3Common.getDs3TreeTableView();
            if (param instanceof TreeTableView) {
                final TreeTableView<Ds3TreeTableValue> param1 = (TreeTableView<Ds3TreeTableValue>) param;
                final Comparator<TreeItem<Ds3TreeTableValue>> comparator = (o1, o2) -> {
                    if (param1.getComparator() == null) {
                        return 0;
                    } else {
                        return param1.getComparator()
                                .compare(o1, o2);
                    }
                };
                if (ds3TreeTable.getRoot() != null) {
                    final ImmutableList<TreeItem<Ds3TreeTableValue>> loaderList = ds3TreeTable.getRoot().getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                    final ImmutableList<TreeItem<Ds3TreeTableValue>> collect = ds3TreeTable.getRoot().getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                    final ObservableList<TreeItem<Ds3TreeTableValue>> treeItems = FXCollections.observableArrayList(collect);
                    FXCollections.sort(treeItems, comparator);
                    if (!param1.getSortOrder().stream().findFirst().get().getText().equals("Type")) {
                        ds3TreeTable.getRoot().getChildren().removeAll(ds3TreeTable.getRoot().getChildren());
                        ds3TreeTable.getRoot().getChildren().addAll(treeItems);
                        if (loaderList.stream().findFirst().orElse(null) != null)
                            ds3TreeTable.getRoot().getChildren().add(loaderList.stream().findFirst().get());
                    }
                    treeItems.forEach(i -> {
                        if (i.isExpanded()) {
                            if (param1.getSortOrder().stream().findFirst().isPresent())
                                sortChild(i, comparator, param1.getSortOrder().stream().findFirst().get().getText());
                            else
                                sortChild(i, comparator, "");
                        }
                    });
                    if (param1.getSortOrder().stream().findFirst().isPresent()) {
                        if (!param1.getSortOrder().stream().findFirst().get().getText().equals("Type")) {
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

    private void sortChild(final TreeItem<Ds3TreeTableValue> o1, final Comparator<TreeItem<Ds3TreeTableValue>> comparator, final String type) {
        try {
            if (comparator != null) {
                final ImmutableList<TreeItem<Ds3TreeTableValue>> loaderList = o1.getChildren().stream().filter(i -> (i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                final ImmutableList<TreeItem<Ds3TreeTableValue>> collect = o1.getChildren().stream().filter(i -> !(i.getValue().getType().toString().equals(Ds3TreeTableValue.Type.Loader.toString()))).collect(GuavaCollectors.immutableList());
                final ObservableList<TreeItem<Ds3TreeTableValue>> treeItems = FXCollections.observableArrayList(collect);
                treeItems.forEach(i -> {
                    if (i.isExpanded())
                        sortChild(i, comparator, type);
                });
                FXCollections.sort(treeItems, comparator);
                o1.getChildren().removeAll(o1.getChildren());
                o1.getChildren().addAll(treeItems);
                if (loaderList.stream().findFirst().orElse(null) != null)
                    o1.getChildren().add(loaderList.stream().findFirst().get());
                if (!type.equals("Type")) {
                    FXCollections.sort(o1.getChildren(), Comparator.comparing(t -> t.getValue().getType().toString()));
                }


            }
        } catch (final Exception e) {
            LOG.error("Unable to sort", e);
        }
    }
}
