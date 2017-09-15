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

import com.spectralogic.dsbrowser.gui.components.localfiletreetable.FileTreeModel;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class FileTreeTableProviderTest {

    @Test
    public void getRoot() throws Exception {
        final Stream<FileTreeModel> root = new FileTreeTableProvider().getRoot("My Computer", new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        final List<FileTreeModel> listRoot = root.collect(Collectors.toList());
        assertTrue(com.spectralogic.ds3client.utils.Guard.isNotNullAndNotEmpty(listRoot));
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
            final Stream<FileTreeModel> listForDir = new FileTreeTableProvider().getListForDir(new FileTreeModel(filePath.toPath(), FileTreeModel.Type.Directory, 0, 0, ""), new DateTimeUtils(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            Assert.assertEquals(filePath.list().length, listForDir.collect(Collectors.toList()).size());
        }
        else {
            Assert.fail("File Path not found");
        }
    }
}