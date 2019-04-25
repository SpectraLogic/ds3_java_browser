/*
 * ****************************************************************************
 *    Copyright 2014-2018 Spectra Logic Corporation. All Rights Reserved.
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
package com.spectralogic.dsbrowser.gui.util.treeItem;

import javafx.event.Event;
import javafx.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class SafeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SafeHandler.class);

    public static <T extends Event> EventHandler<T> logHandle(final EventHandler<T> handler) {
        return event -> {
            try {
                handler.handle(event);
            } catch (final Throwable t) {
                LOG.error("Callback failed", t);
            }
        };
    }
}
