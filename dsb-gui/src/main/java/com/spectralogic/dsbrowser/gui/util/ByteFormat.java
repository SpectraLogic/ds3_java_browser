package com.spectralogic.dsbrowser.gui.util;

public final class ByteFormat {

    public static String humanReadableByteCount(final long bytes, final boolean flag) {
        final int unit = flag ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }

        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = (flag ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (flag ? "" : "i");

        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
