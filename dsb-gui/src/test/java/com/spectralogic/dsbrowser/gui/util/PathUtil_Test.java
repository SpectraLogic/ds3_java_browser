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

package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PathUtil_Test {
    @Test
    public void removeParentFromPath() {
        final String result = PathUtil.toDs3Obj(Paths.get("/parent/path/"), Paths.get("/parent/path/file.txt"), false);
        assertThat(result, is("file.txt"));
    }

    @Test
    public void removeParentFromPathExcludingTopMostParent() {
        final String result = PathUtil.toDs3Obj(Paths.get("/parent/path/"), Paths.get("/parent/path/file.txt"), true);
        assertThat(result, is("path/file.txt"));
    }

    @Test
    public void removeSubPath() {
        final String result = PathUtil.toDs3Obj(Paths.get("/parent/path/"), Paths.get("/parent/path/subdir/file.txt"));
        assertThat(result, is("subdir/file.txt"));
    }

    @Test
    public void convertWindowsPaths() {
        final String result = PathUtil.toDs3Obj(Paths.get("\\parent\\path\\"), Paths.get("\\parent\\path\\subdir\\file.txt"));
        assertThat(result, is("subdir/file.txt"));
    }

    @Test
    public void prefixForObj() {
        final String result = PathUtil.toDs3Path("dirA", "subdir/file.txt");
        assertThat(result, is("dirA/subdir/file.txt"));
    }

    @Test
    public void prefixForObjWithDirSlash() {
        final String result = PathUtil.toDs3Path("dirA/", "subdir/file.txt");
        assertThat(result, is("dirA/subdir/file.txt"));
    }

}
