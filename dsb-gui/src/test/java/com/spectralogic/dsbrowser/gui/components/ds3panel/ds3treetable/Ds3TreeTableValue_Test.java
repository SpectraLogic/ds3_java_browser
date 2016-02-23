package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Ds3TreeTableValue_Test {

    @Test
    public void formatNameForSimplePath() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/obj.txt", Ds3TreeTableValue.Type.FILE, 12, "sometime");

        assertThat(value.getName(), is("obj.txt"));
    }

    @Test
    public void formatDir() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/", Ds3TreeTableValue.Type.DIRECTORY, 0, "sometime");

        assertThat(value.getName(), is("hi"));
    }

    @Test
    public void formatBucket() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "bucket", Ds3TreeTableValue.Type.BUCKET, 0, "sometime");

        assertThat(value.getName(), is("bucket"));
    }

    @Test
    public void getParentDirForDir() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/", Ds3TreeTableValue.Type.DIRECTORY, 0, "sometime");

        assertThat(value.getDirectoryName(), is("hi/"));
    }

    @Test
    public void getParentDirForFile() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/obj.txt", Ds3TreeTableValue.Type.FILE, 0, "sometime");

        assertThat(value.getDirectoryName(), is("hi"));
    }

    @Test
    public void getParentDirForBucket() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "bucket", Ds3TreeTableValue.Type.BUCKET, 0, "sometime");

        assertThat(value.getDirectoryName(), is(""));
    }

    @Test
    public void nestedName() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "dirA/dirB/hi/", Ds3TreeTableValue.Type.DIRECTORY, 12, "sometime");

        assertThat(value.getName(), is("hi"));
    }
}
