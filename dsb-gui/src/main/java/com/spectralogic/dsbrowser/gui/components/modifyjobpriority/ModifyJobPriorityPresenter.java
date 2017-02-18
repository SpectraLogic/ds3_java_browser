package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.ModifyJobPriorityTask;
import com.spectralogic.dsbrowser.gui.util.LogType;
import com.spectralogic.dsbrowser.gui.util.PriorityFilter;
import com.spectralogic.dsbrowser.gui.util.StringConstants;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class ModifyJobPriorityPresenter implements Initializable {

    private final Logger LOG = LoggerFactory.getLogger(ModifyJobPriorityPresenter.class);

    @FXML
    private ComboBox<Priority> modifyJobPriorityComboBox;

    @FXML
    private Label modifyJobPriorityTopLabel, modifyJobPriorityComboBoxLabel;

    @FXML
    private Button yesButton, noButton;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private Workers worker;

    @Inject
    private ModifyJobPriorityModel value;

    @Inject
    private Ds3Common ds3Common;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initGUIElement();
    }

    public void saveModifyJobPriority() {
        final Priority newPriority = modifyJobPriorityComboBox.getValue();

        if (newPriority.equals(Priority.valueOf(value.getCurrentPriority()))) {
            closeModifyJobPriorityPopup();
        } else {
            try {
                final ModifyJobPriorityTask modifyJobPriorityTask = new ModifyJobPriorityTask(value,
                        newPriority);
                worker.execute(modifyJobPriorityTask);

                modifyJobPriorityTask.setOnSucceeded(event -> printLog(LogType.INFO, StringConstants.EMPTY_STRING));
                modifyJobPriorityTask.setOnFailed(event -> printLog(LogType.ERROR, StringConstants.EMPTY_STRING));
            } catch (final Exception e) {
                LOG.error("Failed to modify the job: {}", e);
                printLog(LogType.ERROR, e.getMessage());
            }
            closeModifyJobPriorityPopup();
        }
    }

    public void closeModifyJobPriorityPopup() {
        final Stage popupStage = (Stage) modifyJobPriorityComboBox.getScene().getWindow();
        popupStage.close();
    }

    public void initGUIElement() {
        modifyJobPriorityTopLabel.setText(resourceBundle.getString("modifyJobPriorityTopLabel"));
        modifyJobPriorityComboBoxLabel.setText(resourceBundle.getString("modifyJobPriorityComboBoxLabel"));
        yesButton.setText(resourceBundle.getString("yesButton"));
        noButton.setText(resourceBundle.getString("noButton"));
        modifyJobPriorityComboBox.getItems().addAll(PriorityFilter.filterPriorities(Priority.values()));
        modifyJobPriorityComboBox.getSelectionModel().select(Priority.valueOf(value.getCurrentPriority()));
    }

    private void printLog(final LogType type, final String errMsg) {
        if (type.equals(LogType.ERROR)) {
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("failedToModifyPriority") + errMsg, LogType.ERROR);
        } else {
            ds3Common.getDeepStorageBrowserPresenter().logText(
                    resourceBundle.getString("priorityModified"), LogType.INFO);
        }
    }
}
