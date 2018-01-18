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

package com.spectralogic.dsbrowser.gui;


import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Main_Test {

    @Test
    public void main() throws Exception {
    }

    @Test
    public void start() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
            new JFXPanel(); // Initializes the JavaFx Platform

            Platform.runLater(() -> {
                try {
                    new Main().start(new Stage());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
       // Initialize the thread
        latch.await(30, TimeUnit.SECONDS) ;// Time to use the app, without this, the thread will be killed too soon
    }
}
