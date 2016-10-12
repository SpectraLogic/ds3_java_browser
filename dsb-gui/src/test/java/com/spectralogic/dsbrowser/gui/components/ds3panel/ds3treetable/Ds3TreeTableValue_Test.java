package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Ds3TreeTableValue_Test {

    @Test
    public void formatNameForSimplePath() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/obj.txt", Ds3TreeTableValue.Type.File, 12, "sometime", "", false, null);

        assertThat(value.getName(), is("obj.txt"));
    }

    @Test
    public void formatDir() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/", Ds3TreeTableValue.Type.Directory, 0, "sometime", "", false, null);

        assertThat(value.getName(), is("hi"));
    }

    @Test
    public void formatBucket() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "bucket", Ds3TreeTableValue.Type.Bucket, 0, "sometime", "", false, null);

        assertThat(value.getName(), is("bucket"));
    }

    @Test
    public void getParentDirForDir() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/", Ds3TreeTableValue.Type.Directory, 0, "sometime", "", false, null);

        assertThat(value.getDirectoryName(), is("hi/"));
    }

    @Test
    public void getParentDirForFile() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "hi/obj.txt", Ds3TreeTableValue.Type.File, 0, "sometime", "", false, null);

        assertThat(value.getDirectoryName(), is("hi"));
    }

    @Test
    public void getParentDirForBucket() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "bucket", Ds3TreeTableValue.Type.Bucket, 0, "sometime", "", false, null);

        assertThat(value.getDirectoryName(), is(""));
    }

    @Test
    public void nestedName() {
        final Ds3TreeTableValue value = new Ds3TreeTableValue("bucket", "dirA/dirB/hi/", Ds3TreeTableValue.Type.Directory, 12, "sometime", "", false, null);

        assertThat(value.getName(), is("hi"));
    }
}
