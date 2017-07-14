package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.Priority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PriorityFilter {

    /*
       Do we really need to do this at all? We only use this two places?
       Both times we use this we take the full list of possabilities
       and remove BACKGROUND and CRITICAL.
       A better solution would be to statically provide the list
       Or remove the entries from the enum and skip this wholesale.
     */

    public static Priority[] filterPriorities(final Priority[] priorities) {
        final Priority[] elements = {Priority.BACKGROUND, Priority.CRITICAL};
        final List<Priority> list = new ArrayList<Priority>(Arrays.asList(priorities));
        list.removeAll(Arrays.asList(elements));
        final int size = list.size();
        return list.toArray(new Priority[size]);
    }

}
