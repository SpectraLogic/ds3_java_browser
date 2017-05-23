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

package com.spectralogic.dsbrowser.gui.injector.providers;

import com.google.inject.Provider;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SavedSessionProvider implements Provider<SavedSessionStore> {

    private static final Logger LOG = LoggerFactory.getLogger(SavedSessionProvider.class);

    @Override
    public SavedSessionStore get() {
        try {
            return SavedSessionStore.loadSavedSessionStore();
        } catch (final IOException e) {
            LOG.error("Failed to load any saved sessions, continuing with none", e);
            return SavedSessionStore.empty();
        }
    }
}
