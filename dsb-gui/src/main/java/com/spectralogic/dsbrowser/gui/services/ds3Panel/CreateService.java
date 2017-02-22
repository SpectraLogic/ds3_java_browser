package com.spectralogic.dsbrowser.gui.services.ds3Panel;

import com.google.common.collect.ImmutableList;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.commands.spectrads3.GetDataPoliciesSpectraS3Request;
import com.spectralogic.ds3client.utils.Guard;
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
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.RefreshCompleteViewWorker;
import com.spectralogic.dsbrowser.gui.util.ResourceBundleProperties;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.TreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ResourceBundle;

public class CreateService {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelService.class);

    private static final ResourceBundle resourceBundle = ResourceBundleProperties.getResourceBundle();


    public static void createBucketPrompt(final Ds3Common ds3Common, final Workers workers) {
        LOG.info("Create Bucket Prompt");
        final Session session = ds3Common.getCurrentSession();
        if (session != null) {
            ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString("fetchingDataPolicies"), LogType.INFO);
            final Task<CreateBucketWithDataPoliciesModel> getDataPolicies = new Task<CreateBucketWithDataPoliciesModel>() {

                @Override
                protected CreateBucketWithDataPoliciesModel call() throws Exception {
                    final Ds3Client client = session.getClient();
                    final ImmutableList<CreateBucketModel> buckets = client.getDataPoliciesSpectraS3(new GetDataPoliciesSpectraS3Request()).getDataPolicyListResult().
                            getDataPolicies().stream().map(bucket -> new CreateBucketModel(bucket.getName(), bucket.getId())).collect(GuavaCollectors.immutableList());
                    final ImmutableList<CreateBucketWithDataPoliciesModel> dataPoliciesList = buckets.stream().map(policies ->
                            new CreateBucketWithDataPoliciesModel(buckets, session, workers)).collect(GuavaCollectors.immutableList());
                    Platform.runLater(() -> ds3Common.getDeepStorageBrowserPresenter().logText(resourceBundle.getString
                            ("dataPolicyRetrieved"), LogType.SUCCESS));
                    return dataPoliciesList.stream().findFirst().orElse(null);
                }
            };
            workers.execute(getDataPolicies);
            getDataPolicies.setOnSucceeded(taskEvent -> Platform.runLater(() -> {
                LOG.info("Launching create bucket popup {}", getDataPolicies.getValue().getDataPolicies().size());
                CreateBucketPopup.show(getDataPolicies.getValue(), ds3Common.getDeepStorageBrowserPresenter(), resourceBundle);
                RefreshCompleteViewWorker.refreshCompleteTreeTableView(ds3Common, workers);
            }));

        } else {
            Ds3Alert.show(null, resourceBundle.getString("invalidSession"), Alert.AlertType.ERROR);
        }

    }

    @SuppressWarnings("unchecked")
    public static void createFolderPrompt(final Ds3Common ds3Common) {
        ImmutableList<TreeItem<Ds3TreeTableValue>> values = ds3Common.getDs3TreeTableView().getSelectionModel().getSelectedItems()
                .stream().collect(GuavaCollectors.immutableList());
        final TreeItem<Ds3TreeTableValue> root = ds3Common.getDs3TreeTableView().getRoot();

        if (values.stream().map(TreeItem::getValue).anyMatch(Ds3TreeTableValue::isSearchOn)) {
            LOG.error("You can not create folder here. Please refresh your view");
            Ds3Alert.show(null, "You can not create folder here. Please refresh your view", Alert.AlertType.ERROR);
            return;
        }
        else if (values.size() > 1) {
            LOG.error("Only a single location can be selected to create empty folder");
            Ds3Alert.show(null, "Only a single location can be selected to create empty folder", Alert.AlertType.ERROR);
            return;
        }

        else if (Guard.isNullOrEmpty(values) && root != null && root.getValue() != null) {
            final ImmutableList.Builder<TreeItem<Ds3TreeTableValue>> builder = ImmutableList.builder();
            values = builder.add(root).build().asList();
        }
        else if (Guard.isNullOrEmpty(values)) {
            ds3Common.getDeepStorageBrowserPresenter().logText("Select bucket/folder where you want to create an empty folder.", LogType.ERROR);
            Ds3Alert.show(null, "Location is not selected", Alert.AlertType.ERROR);
            return;
        }
        final TreeItem<Ds3TreeTableValue> ds3TreeTableValueTreeItem = values.stream().findFirst().orElse(null);
        if (ds3TreeTableValueTreeItem != null) {
            //Can not assign final as assigning value again in next step
            final String location = ds3TreeTableValueTreeItem.getValue().getFullName();
            final ImmutableList<String> buckets = values.stream().map(TreeItem::getValue).map(Ds3TreeTableValue::getBucketName).distinct().collect(GuavaCollectors.immutableList());
            CreateFolderPopup.show(new CreateFolderModel(ds3Common.getCurrentSession().getClient(), location, buckets.stream().findFirst().orElse(null)), ds3Common.getDeepStorageBrowserPresenter(), resourceBundle);
            Ds3PanelService.refresh(ds3TreeTableValueTreeItem);
        }
    }


}
