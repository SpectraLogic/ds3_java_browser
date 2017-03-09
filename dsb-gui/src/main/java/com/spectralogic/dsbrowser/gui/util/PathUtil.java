/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtil {

    private PathUtil() {
        // pass
    }

    public static String toDs3Path(final String ds3Dir, final String newPath) {
        final String path;
        if (ds3Dir.endsWith("/") && newPath.startsWith("/")) {
            path = ds3Dir + newPath.substring(1);
        } else if (!ds3Dir.endsWith("/") && !newPath.startsWith("/")) {
            path = ds3Dir + "/" + newPath;
        } else {
            path = ds3Dir + newPath;
        }
        if (path.startsWith("/")) {
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

    public static Path resolveForSymbolic(final Path path) throws IOException {
        if (Files.isSymbolicLink(path)) {
            final Path simLink = Files.readSymbolicLink(path);
            if (!simLink.isAbsolute()) {
                // Resolve the path such that the path is relative to the symbolically
                // linked file's directory
                final Path symLinkParent = path.toAbsolutePath().getParent();
                return symLinkParent.resolve(simLink);
            }

            return simLink;
        }
        return path;
    }
}
