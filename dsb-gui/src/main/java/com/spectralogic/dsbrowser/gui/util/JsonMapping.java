/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
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
