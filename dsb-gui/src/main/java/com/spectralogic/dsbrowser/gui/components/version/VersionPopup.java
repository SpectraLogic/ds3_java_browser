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

package com.spectralogic.dsbrowser.gui.components.version;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.AlertService;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class VersionPopup {
    private static final Logger LOG = LoggerFactory.getLogger(VersionPopup.class);

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final LoggingService loggingService;
    private final AlertService alertService;

    @Inject
    public VersionPopup(
            final ResourceBundle resourceBundle,
            final Ds3Common ds3Common,
            final LoggingService loggingService,
            final AlertService alertService
    ) {
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.loggingService = loggingService;
        this.alertService = alertService;
    }

    public void show(final Ds3TreeTableValue item) {
        Platform.runLater(() -> {
            final List<VersionItem> versions = getVersioned(item)
                    .stream()
                    .map(contents -> new VersionItem(contents.getKey(), contents.getLastModified(), contents.getVersionId(), contents.getSize()))
                    .collect(GuavaCollectors.immutableList());

            if (versions.isEmpty()) {
                alertService.warning("unableToGetVersions");
            } else {
                final Stage popup = new Stage();
                popup.initOwner(ds3Common.getDs3TreeTableView().getScene().getWindow());
                final VersionView view = new VersionView(new VersionModel(item.getBucketName(), versions, popup));
                final Scene popupScene = new Scene(view.getView());
                popup.initModality(Modality.APPLICATION_MODAL);
                popup.getIcons().add(new Image(StringConstants.DSB_ICON_PATH));
                popup.setScene(popupScene);
                popup.centerOnScreen();
                popup.setTitle(resourceBundle.getString("versionView"));
                popup.setAlwaysOnTop(false);
                popup.setResizable(true);
                popup.showAndWait();
            }
        });
    }

    private List<Contents> getVersioned(final Ds3TreeTableValue item) {
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        final GetBucketRequest getBucketRequest = new GetBucketRequest(item.getBucketName());
        getBucketRequest.withVersions(true);
        getBucketRequest.withPrefix(item.getFullName());

        try {
            return client.getBucket(getBucketRequest).getListBucketResult().getVersionedObjects();
        } catch (final IOException io) {
            LOG.error("Could not get versions for " + item.getFullName(), io);
            return Collections.emptyList();
        }
    }
}
