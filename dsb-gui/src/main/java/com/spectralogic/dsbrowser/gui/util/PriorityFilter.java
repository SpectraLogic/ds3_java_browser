package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PriorityFilter {
    public static Priority[] filterPriorities(final Priority[] priorities) {
        final Priority[] elements = {Priority.BACKGROUND, Priority.CRITICAL};
        final List<Priority> list = new ArrayList<Priority>(Arrays.asList(priorities));
        list.removeAll(Arrays.asList(elements));
        return list.toArray(new Priority[0]);
    }

}
