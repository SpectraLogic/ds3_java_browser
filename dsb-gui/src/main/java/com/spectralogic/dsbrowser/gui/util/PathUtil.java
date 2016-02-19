package com.spectralogic.dsbrowser.gui.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PathUtil {

    private PathUtil() {
        // pass
    }

    public static String toDs3Path(final String ds3Dir, final String newPath) {
        if (ds3Dir.endsWith("/") && newPath.startsWith("/")) {
            return ds3Dir + newPath.substring(1);
        } else if (!ds3Dir.endsWith("/") && !newPath.startsWith("/")) {
            return ds3Dir + "/" + newPath;
        }
        return ds3Dir + newPath;
    }

    public static String toDs3Obj(final Path rootPath, final Path fullObjPath) {
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
