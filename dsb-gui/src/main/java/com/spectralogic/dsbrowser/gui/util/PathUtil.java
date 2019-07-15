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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.spectralogic.dsbrowser.gui.util.StringConstants.FORWARD_SLASH;

public final class PathUtil {


    public static String toDs3Path(final String ds3Dir, final String newPath) {
        final String path;
        if (ds3Dir.endsWith(FORWARD_SLASH) && newPath.startsWith(FORWARD_SLASH)) {
            path = ds3Dir + newPath.substring(1);
        } else if (!ds3Dir.endsWith(FORWARD_SLASH) && !newPath.startsWith(FORWARD_SLASH)) {
            path = ds3Dir + FORWARD_SLASH + newPath;
        } else {
            path = ds3Dir + newPath;
        }
        if (path.startsWith(FORWARD_SLASH)) {
            return path.substring(1);
        }
        return path;
    }

    public static String toDs3Obj(final Path rootPath, final Path fullObjPath) {
        return toDs3Obj(rootPath, fullObjPath, false);
    }

    public static String toDs3Obj(final Path rootPath, final Path fullObjPath, final boolean includeParentDir) {
        if (includeParentDir) {
            return toDs3Obj(rootPath.getParent(), fullObjPath);
        }
        return rootPath.relativize(fullObjPath).toString().replace('\\', '/');
    }

    public static String toDs3ObjWithFiles(final Path rootPath, final Path fullObjPath) {
        return rootPath.relativize(fullObjPath).toString().replace('\\', '/');
    }


    public static String getFolderLocation(final String location, final String bucketName) {
        String newLocation = StringConstants.EMPTY_STRING;
        if (!location.equals(bucketName)) {
            newLocation = location;
            //if creating folder while file is selected
            if (!newLocation.endsWith(StringConstants.FORWARD_SLASH)) {
                final int lastIndex = newLocation.lastIndexOf(StringConstants.FORWARD_SLASH);
                newLocation = newLocation.substring(0, lastIndex + 1);
            }
        }
        return newLocation;
    }

    public static List<Ds3Object> getDs3ObjectList(final String location, final String folderName) {
        final List<Ds3Object> ds3ObjectList = new ArrayList<>();
        final Ds3Object object = new Ds3Object(location + folderName + StringConstants.FORWARD_SLASH, 0);
        ds3ObjectList.add(object);
        return ds3ObjectList;
    }
}
