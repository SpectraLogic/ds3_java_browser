package com.spectralogic.dsbrowser.util;

public class Guard {
    public static void assertNotNull(final Object obj) {
        if (obj == null) throw new IllegalArgumentException("Argument cannot be null");
    }
}
