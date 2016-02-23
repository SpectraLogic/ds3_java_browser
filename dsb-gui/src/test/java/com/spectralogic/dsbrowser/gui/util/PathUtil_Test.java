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
        final String result = PathUtil.toDs3Obj(Paths.get("\\parent\\path/"), Paths.get("\\parent\\path\\subdir\\file.txt"));
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
