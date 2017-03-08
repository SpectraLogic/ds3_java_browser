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

package com.spectralogic.dsbrowser.gui.services.sessionStore;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.stream.Stream;

public class Ds3SessionStore implements SessionStore {
    private final ObservableList<Session> sessions = FXCollections.observableArrayList();

    @Override
    public void addSession(final Session session) {
        sessions.add(session);
    }

    @Override
    public Stream<Session> getSessions() {
        return sessions.stream();
    }

    @Override
    public ObservableList<Session> getObservableList() {
        return this.sessions;
    }

    @Override
    public void removeSession(final Session session) {
        sessions.remove(session);
    }

    @Override
    public boolean isEmpty() {
        return sessions.isEmpty();
    }

    @Override
    public int size() {
        return sessions.size();
    }
}
