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

package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;

public class LoggingServiceImpl implements LoggingService {

    private final List<LoggingService.LoggingListener> listeners = new ArrayList<>();

    @Override
    public void registerLogListener(final LoggingService.LoggingListener loggerListener) {
        listeners.add(loggerListener);
    }

    @Override
    public void logMessage(final String message, final LogType logType) {
        listeners.forEach(loggingListener -> loggingListener.log(message, logType));
    }
}
