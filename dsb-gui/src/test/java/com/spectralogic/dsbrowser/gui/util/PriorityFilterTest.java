package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.Priority;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PriorityFilterTest {
    @Test
    public void filterPriorities() throws Exception {

        final Priority[] priorities = PriorityFilter.filterPriorities(Priority.values());
        assertThat(priorities[0], is(Priority.valueOf("URGENT")));
    }
}