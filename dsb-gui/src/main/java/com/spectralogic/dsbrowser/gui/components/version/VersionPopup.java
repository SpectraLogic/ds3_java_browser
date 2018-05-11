package com.spectralogic.dsbrowser.gui.components.version;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.GetBucketRequest;
import com.spectralogic.ds3client.models.Contents;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.util.LazyAlert;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
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
import java.util.stream.Collectors;

public class VersionPopup {
    private static final Logger LOG = LoggerFactory.getLogger(VersionPopup.class);

    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final LoggingService loggingService;
    private final LazyAlert lazyAlert;

    @Inject
    public VersionPopup(
            final ResourceBundle resourceBundle,
            final Ds3Common ds3Common,
            final LoggingService loggingService,
            final LazyAlert lazyAlert
    ) {
       this.resourceBundle = resourceBundle;
       this.ds3Common = ds3Common;
       this.loggingService = loggingService;
       this.lazyAlert = lazyAlert;
    }

    public void show(final Ds3TreeTableValue item) {
        final List<VersionItem> versions = getVersioned(item)
                .stream()
                .map(contents -> new VersionItem(contents.getKey(), contents.getLastModified(), contents.getSize(), contents.getVersionId()))
                .collect(GuavaCollectors.immutableList());

        if (versions.isEmpty()) {
            lazyAlert.warningRaw(resourceBundle.getString("unabletogetversions"));
        } else {
            final Stage popup = new Stage();
            final VersionView view = new VersionView(new VersionModel(item.getBucketName(), versions, popup));
            final Scene popupScene = new Scene(view.getView());

            popup.initModality(Modality.APPLICATION_MODAL);
            popup.getIcons().add(new Image(StringConstants.DSB_ICON_PATH));
            popup.setScene(popupScene);
            popup.setTitle(resourceBundle.getString("versionView"));
            popup.setAlwaysOnTop(false);
            popup.setResizable(true);
            popup.showAndWait();
        }
    }

    private List<Contents> getVersioned(final Ds3TreeTableValue item) {
        final Ds3Client client = ds3Common.getCurrentSession().getClient();
        GetBucketRequest getBucketRequest = new GetBucketRequest(item.getBucketName());
        getBucketRequest.withVersions(true);
        getBucketRequest.withPrefix(item.getFullName());

        try {
            return client.getBucket(getBucketRequest).getListBucketResult().getVersionedObjects();
        } catch (final IOException io) {
            LOG.error("Could not get versions for " + item.getFullName(), io);
            loggingService.logMessage(resourceBundle.getString("couldnotgetversion"), LogType.ERROR);
            return Collections.emptyList();
        }
    }
}
