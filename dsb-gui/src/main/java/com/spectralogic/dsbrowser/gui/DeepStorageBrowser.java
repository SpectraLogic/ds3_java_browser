
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

import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.util.CloseConfirmationHandler;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ResourceBundle;

class DeepStorageBrowser {

    private static final Logger LOG = LoggerFactory.getLogger(DeepStorageBrowser.class);

    private final ResourceBundle resourceBundle;
    private final BuildInfoService buildInfoService;
    private final ShutdownService shutdownService;
    private final JobWorkers jobWorkers;

    @Inject
    DeepStorageBrowser(
            final ResourceBundle resourceBundle,
            final BuildInfoService buildInfoService,
            final ShutdownService shutdownService,
            final JobWorkers jobWorkers) {
        this.resourceBundle = resourceBundle;
        this.buildInfoService = buildInfoService;
        this.shutdownService = shutdownService;
        this.jobWorkers = jobWorkers;
    }

    void start(final Stage primaryStage) {
        LOG.info("Starting Deep Storage Browser {}", buildInfoService.getBuildVersion());
        LOG.info("  {}", buildInfoService.getBuildDateTime());
        LOG.info(getPlatformInformation());

        final DeepStorageBrowserView mainView = new DeepStorageBrowserView();

        final Scene mainScene = new Scene(mainView.getView());
        primaryStage.getIcons().add(new Image(Main.class.getResource(ImageURLs.DEEP_STORAGE_BROWSER).toString()));
        primaryStage.setScene(mainScene);
        primaryStage.setMaximized(true);
        primaryStage.setTitle(resourceBundle.getString("title"));
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        primaryStage.show();
    }

    private static String getPlatformInformation() {
        return String.format("Java Version: {%s}\n", System.getProperty("java.version"))
                + String.format("Java Vendor: {%s}\n", System.getProperty("java.vendor"))
                + String.format("JVM Version: {%s}\n", System.getProperty("java.vm.version"))
                + String.format("JVM Name: {%s}\n", System.getProperty("java.vm.name"))
                + String.format("OS: {%s}\n", System.getProperty("os.name"))
                + String.format("OS Arch: {%s}\n", System.getProperty("os.arch"))
                + String.format("OS Version: {%s}", System.getProperty("os.version"));
    }

    private void handleWindowClose(final WindowEvent event) {
        final CloseConfirmationHandler closeConfirmationHandler = new CloseConfirmationHandler(resourceBundle, jobWorkers, shutdownService);
        closeConfirmationHandler.closeConfirmationAlert(event);
    }

}
