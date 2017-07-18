package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.control.TreeTableCell;

public class ValueTreeTableCell<T> extends TreeTableCell<T,Number> {
    @Override
    protected void updateItem(final Number item, final boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
        } else {
            setText(FileSizeFormat.getFileSizeType(item.longValue()));
        }
    }
}
