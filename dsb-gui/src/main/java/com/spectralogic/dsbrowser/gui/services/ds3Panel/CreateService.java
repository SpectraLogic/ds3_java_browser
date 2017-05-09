package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketModel;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketPopup;
import com.spectralogic.dsbrowser.gui.components.createbucket.CreateBucketWithDataPoliciesModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderModel;
import com.spectralogic.dsbrowser.gui.components.createfolder.CreateFolderPopup;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ResourceBundle;

public final class CreateService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();

    public static void createBucketPrompt(final Ds3Common ds3Common, final Workers workers, final LoggingService loggingService) {
        LOG.info("Create Bucket Prompt");
        final Session session = ds3Common.getCurrentSession();
        if (session != null) {
            loggingService.logMessage(resourceBundle.getString("fetchingDataPolicies"), LogType.INFO);
            final Task<Optional<CreateBucketWithDataPoliciesModel>> getDataPolicies = new Task<Optional<CreateBucketWithDataPoliciesModel>>() {

                @Override
                protected Optional<CreateBucketWithDataPoliciesModel> call() throws Exception {
                    final Ds3Client client = session.getClient();
                    final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                            getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
                    final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                            new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                    loggingService.logMessage(resourceBundle.getString
                            ("dataPolicyRetrieved"), LogType.SUCCESS);
                    final Optional<CreateBucketWithDataPoliciesModel> first = dataPoliciesList.stream().findFirst();
                    return Optional.ofNullable(first.get());
                }
            };
            workers.execute(getDataPolicies);
            getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                final Optional<CreateBucketWithDataPoliciesModel> value = getDataPolicies.getValue();
                if (value.isPresent()) {
                    LOG.info("Launching create bucket popup {}", value.get().getDataPolicies().size());
                    CreateBucketPopup.show(value.get(), resourceBundle);
                    RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers, loggingService);
                }
            }));

        } else {
            Ds3Alert.show(null, resourceBundle.getString("invalidSession"), Alert.AlertType.ERROR);
        }

    }

    public static void createFolderPrompt(final Ds3Common ds3Common, final LoggingService loggingService) {
        ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3Common.getDs3TreeTableView().getRoot();

        if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.info("You can not create folder here. Please refresh your view");
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("cantCreateFolderHere"), Alert.AlertType.ERROR);
            return;
        } else if (values.size() > 1) {
            LOG.info("Only a single location can be selected to create empty folder");
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("selectSingleLocation"), Alert.AlertType.ERROR);
            return;
        } else if (Guard.isNullOrEmpty(values) && root != null && root.getValue() != null) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            values = builder.add(root).build();
        } else if (Guard.isNullOrEmpty(values)) {
            loggingService.logMessage(resourceBundle.getString("selectLocation"), LogType.ERROR);
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("locationNotSelected"), Alert.AlertType.ERROR);
            return;
        }
        final Optional<TreeItem<Ds3TreeTableValue>> first = values.stream().findFirst();
        if (first.isPresent()) {
            final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = first.get();
            final String location = ds3TreeTableValueTreeItem.getValue().getFullName();
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
            final Optional<String> bucketElement = buckets.stream().findFirst();
            if (bucketElement.isPresent()) {
                CreateFolderPopup.show(new CreateFolderModel(ds3Common.getCurrentSession().getClient(), location, bucketElement.get()), ds3Common.getDeepStorageBrowserPresenter(), resourceBundle);
            }
            Ds3PanelService.refresh(ds3TreeTableValueTreeItem);
        }
    }

}
