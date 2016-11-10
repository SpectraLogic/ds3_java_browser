package com.spectralogic.dsbrowser.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Platform {

    public final static OS OS_TYPE;
    private final static Logger LOG = LoggerFactory.getLogger(Platform.class);

    static {
        final String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            OS_TYPE = OS.WINDOWS;
        } else if (osName.contains("Mac")) {
            OS_TYPE = OS.OSX;
        } else if (osName.contains("Linux")) {
            OS_TYPE = OS.LINUX;
        } else if (osName.contains("FreeBSD")) {
            OS_TYPE = OS.FREE_BSD;
        } else {
            LOG.info("Unknown platform type: " + osName);
            OS_TYPE = OS.UNKNOWN;
        }
    }

    public static boolean isWin() {
        return OS_TYPE == OS.WINDOWS;
    }

    public enum OS {
        WINDOWS, LINUX, OSX, FREE_BSD, UNKNOWN
    }
}
