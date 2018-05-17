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

import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


public class PopupTest {
    private final static Popup popup = new Popup(new Ds3Common());

    @Test
    public void show() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        new JFXPanel();
        Platform.runLater(() -> {
            popup.show(new AboutView().getView(), "About");
        });
        latch.await(10, TimeUnit.SECONDS);
    }
}