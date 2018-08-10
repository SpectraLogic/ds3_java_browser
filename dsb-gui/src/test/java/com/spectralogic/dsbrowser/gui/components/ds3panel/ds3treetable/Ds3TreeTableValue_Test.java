/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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

        assertThat(value.getDirectoryName(), is("hi/"));
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
