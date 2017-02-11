package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import javafx.scene.control.TreeItem;

import java.util.Comparator;

public class PartialResultComparator implements Comparator<TreeItem<Ds3TreeTableValue>> {

    @Override
    public int compare(final TreeItem<Ds3TreeTableValue> o1, final TreeItem<Ds3TreeTableValue> o2) {
        final String type1 = o1.getValue().getType().toString();
        final String type2 = o2.getValue().getType().toString();
        if (type1.equals(Ds3TreeTableValue.Type.Directory.toString()) && !type2.equals(Ds3TreeTableValue.Type.Directory.toString())) {
            // Directory before non-directory
            return -1;
        } else if (!type1.equals(Ds3TreeTableValue.Type.Directory.toString()) && type2.equals(Ds3TreeTableValue.Type.Directory.toString())) {
            // Non-directory after directory
            return 1;
        } else {
            // Alphabetic order otherwise
            return o1.getValue().getFullName().compareTo(o2.getValue().getFullName());
        }
    }
}
