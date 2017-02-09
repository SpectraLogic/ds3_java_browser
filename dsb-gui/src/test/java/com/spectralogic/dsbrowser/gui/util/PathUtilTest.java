package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.ds3client.models.bulk.Ds3Object;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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


    @Test
    public void getLocation() throws Exception {
        final String folderLocation1 = PathUtil.getFolderLocation("temp", "TEST1");
        final String folderLocation2 = PathUtil.getFolderLocation("TEST1", "TEST1");
        final String folderLocation3 = PathUtil.getFolderLocation("TEST1/", "TEST1");
        assertTrue(folderLocation1.equals(""));
        assertTrue(folderLocation2.equals(""));
        assertTrue(folderLocation3.equals("TEST1/"));
    }

    @Test
    public void getDs3ObjectList() throws Exception {
        final String folderLocation = PathUtil.getFolderLocation("temp", "TEST1");
        final List<Ds3Object> objectList = PathUtil.getDs3ObjectList(folderLocation, "temp");
        assertTrue(objectList.stream().findFirst().orElse(null).getName().equals("temp/"));
    }
}