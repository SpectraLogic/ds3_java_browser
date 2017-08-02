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

import com.spectralogic.ds3client.models.bulk.Ds3Object;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void toDs3Path() throws Exception {
        final String result1 = PathUtil.toDs3Path("TEST09/", "/file.txt");
        final String result2 = PathUtil.toDs3Path("TEST09", "file.txt");
        final String result3 = PathUtil.toDs3Path("TEST09", "/file.txt");
        final String result4 = PathUtil.toDs3Path("/TEST09", "file.txt");
        assertThat(result1, is("TEST09/file.txt"));
        assertThat(result2, is("TEST09/file.txt"));
        assertThat(result3, is("TEST09/file.txt"));
        assertThat(result4, is("TEST09/file.txt"));
    }

    @Test
    public void toDs3Obj() throws Exception {
        final String result = PathUtil.toDs3Obj(Paths.get("/parent/path/"),
                Paths.get("/parent/path/file.txt"));
        assertThat(result, is("file.txt"));
    }

    @Test
    public void toDs3Obj1() throws Exception {
        final String result = PathUtil.toDs3Obj(Paths.get("/parent/path/"),
                Paths.get("/parent/path/file.txt"), true);
        assertThat(result, is("path/file.txt"));
    }

    @Test
    public void toDs3ObjWithFiles() throws Exception {
        final String result = PathUtil.toDs3ObjWithFiles(Paths.get("/"), Paths.get
                ("/parent/path/file.txt"));
        assertThat(result, is(("parent/path/file.txt")));
    }

    @Test
    public void resolveForSymbolic() throws Exception {
        final Path path = PathUtil.resolveForSymbolic(Paths.get("/parent/path/file.txt"));
        assertThat(path, is(Paths.get("/parent/path/file.txt")));
    }


    @Test
    public void getLocation() throws Exception {
        final String folderLocation1 = PathUtil.getFolderLocation(SessionConstants.FOLDER_INSIDE_EXISTING_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET);
        final String folderLocation2 = PathUtil.getFolderLocation(SessionConstants.ALREADY_EXIST_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET);
        final String folderLocation3 = PathUtil.getFolderLocation(SessionConstants.ALREADY_EXIST_BUCKET + "/", SessionConstants.ALREADY_EXIST_BUCKET);
        assertTrue(folderLocation1.equals(""));
        assertTrue(folderLocation2.equals(""));
        assertTrue(folderLocation3.equals(SessionConstants.ALREADY_EXIST_BUCKET + "/"));
    }

    @Test
    public void getDs3ObjectList() throws Exception {
        final String folderLocation = PathUtil.getFolderLocation(SessionConstants.FOLDER_INSIDE_EXISTING_BUCKET, SessionConstants.ALREADY_EXIST_BUCKET);
        final List<Ds3Object> objectList = PathUtil.getDs3ObjectList(folderLocation, SessionConstants.FOLDER_INSIDE_EXISTING_BUCKET);
        final Optional<Ds3Object> objectElement = objectList.stream().findFirst();
        assertTrue(objectElement.isPresent() && objectElement.get().getName().equals(SessionConstants.FOLDER_INSIDE_EXISTING_BUCKET + "/"));
    }
}
