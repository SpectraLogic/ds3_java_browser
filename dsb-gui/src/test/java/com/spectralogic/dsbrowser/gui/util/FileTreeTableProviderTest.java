package com.spectralogic.dsbrowser.gui.util;

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class FileTreeTableProviderTest {

    @Test
    public void getRoot() throws Exception {
        final Stream<FileTreeModel> root = new FileTreeTableProvider().getRoot("My Computer");
        final List<FileTreeModel> listRoot = root.collect(Collectors.toList());
        assertTrue(com.spectralogic.ds3client.utils.Guard.isNotNullAndNotEmpty(listRoot));
    }

    @Test
    public void getPathType() throws Exception {
        final ClassLoader classLoader = FileTreeTableProviderTest.class.getClassLoader();
        final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER + SessionConstants.LOCAL_FILE);
        Path path = null;
        if (url != null) {
            path = new File(url.getFile()).toPath();
        }
        Assert.assertEquals(FileTreeTableProvider.getPathType(path), FileTreeModel.Type.File);
    }

    /**
     * TEST CASE PRE ASSUMING THAT FOLDER CONTAINS ONLY FILES
     *
     * @throws Exception
     */
    @Test
    public void getListForDir() throws Exception {
        final ClassLoader classLoader = FileTreeTableProviderTest.class.getClassLoader();
        final URL url = classLoader.getResource(SessionConstants.LOCAL_FOLDER);
        final File filePath ;
        if (url != null) {
            filePath = new File(url.getFile());
            final Stream<FileTreeModel> listForDir = new FileTreeTableProvider().getListForDir(new FileTreeModel(filePath.toPath(), FileTreeModel.Type.Directory, 0, 0, ""));
            Assert.assertEquals(filePath.list().length, listForDir.collect(Collectors.toList()).size());
        }
        else {
            Assert.fail("File Path not found");
        }
    }
}