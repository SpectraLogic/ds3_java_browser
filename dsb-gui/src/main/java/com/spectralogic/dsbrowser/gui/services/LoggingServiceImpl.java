/*
 * ******************************************************************************
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
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui.services;

import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class LoggingServiceImpl implements LoggingService {
    private final PublishSubject<LogEvent> subject  = PublishSubject.create();

    @Override
    public Observable<LogEvent> getLoggerObservable() {
        return subject;
    }

    @Override
    public void logMessage(final String message, final LogType logType) {
        subject.onNext(new LogEvent(message, logType)); // log an event to all subscribers (in our case: DeepStorageBrowserPresenter::logText())
    }

}
