package com.spectralogic.dsbrowser.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
