package com.spectralogic.dsbrowser.gui.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class JsonMapping {

    private final static ObjectMapper MAPPER = new ObjectMapper();

    public static <T> T fromJson(final InputStream stream, final Class<T> clazz) throws IOException {
        return MAPPER.readValue(stream, clazz);
    }

    public static void toJson(final OutputStream output, final Object obj) throws IOException {
        MAPPER.writeValue(output, obj);
    }
}
