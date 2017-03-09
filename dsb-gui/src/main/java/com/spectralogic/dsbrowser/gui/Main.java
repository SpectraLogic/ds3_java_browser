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

package com.spectralogic.dsbrowser.gui;

import com.google.inject.Injector;
import com.spectralogic.dsbrowser.gui.injector.GuicePresenterInjector;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(final String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) throws Exception {
        final Injector injector = GuicePresenterInjector.injector;

        // restoring the logging settings before any other components are loaded
        final ApplicationLoggerSettings applicationLoggerSettings = injector.getInstance(ApplicationLoggerSettings.class);
        applicationLoggerSettings.restoreLoggingSettings();

        final DeepStorageBrowser dsb = injector.getInstance(DeepStorageBrowser.class);
        dsb.start(primaryStage);
    }
}
