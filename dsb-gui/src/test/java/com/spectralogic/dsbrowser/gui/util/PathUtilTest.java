package com.spectralogic.dsbrowser.gui.util;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class PathUtilTest {
    @Test
    public void toDs3Path() throws Exception {
        final String result1 = PathUtil.toDs3Path("TEST09/", "/file.txt");
        final String result2 = PathUtil.toDs3Path("TEST09", "file.txt");
        final String result3 = PathUtil.toDs3Path("TEST09", "/file.txt");
        final String result4 = PathUtil.toDs3Path("/TEST09", "file.txt");
        assertThat(result1, is("TEST09/file.txt"));
        assertThat(result2, is("TEST09/file.txt"));
        assertThat(result3, is("TEST09/file.txt"));
        assertThat(result3, is("TEST09/file.txt"));
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
}