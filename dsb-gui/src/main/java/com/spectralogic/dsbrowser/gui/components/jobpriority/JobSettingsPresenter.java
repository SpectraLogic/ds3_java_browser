package com.spectralogic.dsbrowser.gui.components.jobpriority;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.JobSettings;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPriorities;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class JobSettingsPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(JobSettingsPresenter.class);

    private final JobSettingsModel model = new JobSettingsModel();

    @FXML
    private ComboBox<Priority> getJobPriority, putJobPriority;

    @Inject
    private SavedJobPrioritiesStore jobPrioritiesStore;

    @Inject
    private Session session;

    private SavedJobPriorities savedJobPriorities;

    private JobSettings jobSettings;


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            this.jobSettings = jobPrioritiesStore.getJobSettings();
            initComboBox();
        } catch (final Throwable e) {
            LOG.error("Failed to startup job setting presenter");
        }
    }

    public void initComboBox() {
        putJobPriority.getItems().addAll(Priority.values());
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("NORMAL"))
            putJobPriority.getSelectionModel().select(Priority.NORMAL);
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("CRITICAL"))
            putJobPriority.getSelectionModel().select(Priority.CRITICAL);
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("URGENT"))
            putJobPriority.getSelectionModel().select(Priority.URGENT);
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("HIGH"))
            putJobPriority.getSelectionModel().select(Priority.HIGH);
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("LOW"))
            putJobPriority.getSelectionModel().select(Priority.LOW);
        if (jobSettings.getPutJobPriority().equalsIgnoreCase("BACKGROUND"))
            putJobPriority.getSelectionModel().select(Priority.BACKGROUND);

        getJobPriority.getItems().addAll(Priority.values());
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("NORMAL"))
            getJobPriority.getSelectionModel().select(Priority.NORMAL);
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("CRITICAL"))
            getJobPriority.getSelectionModel().select(Priority.CRITICAL);
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("URGENT"))
            getJobPriority.getSelectionModel().select(Priority.URGENT);
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("HIGH"))
            getJobPriority.getSelectionModel().select(Priority.HIGH);
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("LOW"))
            getJobPriority.getSelectionModel().select(Priority.LOW);
        if (jobSettings.getGetJobPriority().equalsIgnoreCase("BACKGROUND"))
            getJobPriority.getSelectionModel().select(Priority.BACKGROUND);
    }

    public void saveJobSettings() {

        LOG.info("Updating jobs settings");
        try {
            jobSettings.setSessionName("Default");
            jobSettings.setEndpoint("Default");
            jobSettings.setGetJobPriority(getJobPriority.getSelectionModel().getSelectedItem().name());
            jobSettings.setPutJobPriority(putJobPriority.getSelectionModel().getSelectedItem().name());
            jobPrioritiesStore.saveSavedJobPriorties(jobPrioritiesStore);
        } catch (Exception e) {
            e.printStackTrace();
        }
        closeDialog();
    }

    public void closeDialog() {
        final Stage popupStage = (Stage) putJobPriority.getScene().getWindow();
        popupStage.close();
    }
}
