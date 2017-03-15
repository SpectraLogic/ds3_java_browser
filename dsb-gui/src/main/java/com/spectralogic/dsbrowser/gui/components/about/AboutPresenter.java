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

package com.spectralogic.dsbrowser.gui.components.about;

import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
public class AboutPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(AboutPresenter.class);

    final private ResourceBundle resourceBundle;
    final private BuildInfoService buildInfoService;

    @FXML
    private Label copyRightLabel1, copyRightLabel2;
    @FXML
    private Hyperlink hyperlink;

    @FXML
    private Label title;

    @FXML
    private Label buildVersion;

    @Inject
    public AboutPresenter(final ResourceBundle resourceBundle, final BuildInfoService buildInfoService) {
        this.resourceBundle = resourceBundle;
        this.buildInfoService = buildInfoService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        buildVersion.setText(buildInfoService.getBuildVersion());

        title.setText(resourceBundle.getString("title"));
        hyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("http://www.apache.org/licenses/LICENSE-2.0"));
            } catch (final IOException|URISyntaxException e) {
                LOG.error("Failed to open apache license in a browser", e);
            }
        });
        copyRightLabel1.setText(resourceBundle.getString("copyrightTxt1"));
        copyRightLabel2.setText(resourceBundle.getString("copyrightTxt2"));
    }
}
