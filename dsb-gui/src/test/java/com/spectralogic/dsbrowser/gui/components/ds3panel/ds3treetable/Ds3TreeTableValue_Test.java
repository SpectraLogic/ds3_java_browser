package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Ds3TreeTableValue_Test {

    @Test
    public void formatNameForSimplePath() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("hi/obj.txt", Ds3TreeTableValue.Type.FILE, 12, "sometime");

        assertThat(value.getName(), is("obj.txt"));
    }

    @Test
    public void formatDir() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("hi/", Ds3TreeTableValue.Type.DIRECTORY, 0, "sometime");

        assertThat(value.getName(), is("hi"));
    }

    @Test
    public void formatBucket() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", Ds3TreeTableValue.Type.BUCKET, 0, "sometime");

        assertThat(value.getName(), is("bucket"));

    }
}
