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

package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.tasks.ModifyJobPriorityTask;
import com.spectralogic.dsbrowser.gui.util.PriorityFilter;
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

@Presenter
public class ModifyJobPriorityPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(ModifyJobPriorityPresenter.class);

    @FXML
    private ComboBox<Priority> modifyJobPriorityComboBox;

    @FXML
    private Label modifyJobPriorityTopLabel, modifyJobPriorityComboBoxLabel;

    @FXML
    private Button yesButton, noButton;

    private final ResourceBundle resourceBundle;
    private final Workers workers;
    private final Ds3Common ds3Common;
    private final LoggingService loggingService;

    @ModelContext
    private ModifyJobPriorityModel value;

    @Inject
    public ModifyJobPriorityPresenter(final ResourceBundle resourceBundle,
                                      final Workers workers,
                                      final Ds3Common ds3Common,
                                      final LoggingService loggingService) {
        this.resourceBundle = resourceBundle;
        this.workers = workers;
        this.ds3Common = ds3Common;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElement();
        } catch (final Throwable t) {
            LOG.error("Encountered an error when initializing ModifyJobPriorityPresenter", t);
        }
    }

    public void saveModifyJobPriority() {
        final Priority newPriority = modifyJobPriorityComboBox.getValue();

        if (newPriority.equals(Priority.valueOf(value.getCurrentPriority()))) {
            closeModifyJobPriorityPopup();
        } else {
            try {
                final ModifyJobPriorityTask modifyJobPriorityTask = new ModifyJobPriorityTask(value,
                        newPriority);

                modifyJobPriorityTask.setOnSucceeded(event -> loggingService.logMessage(
                    resourceBundle.getString("priorityModified"), LogType.INFO));
                modifyJobPriorityTask.setOnFailed(event -> loggingService.logMessage(
                    resourceBundle.getString("failedToModifyPriority"), LogType.ERROR));
                workers.execute(modifyJobPriorityTask);
            } catch (final Exception e) {
                LOG.error("Failed to modify the job:", e);
                loggingService.logMessage(
                    resourceBundle.getString("failedToModifyPriority"), LogType.ERROR);
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
        modifyJobPriorityComboBox.getItems().addAll(PriorityFilter.filterPriorities());
        modifyJobPriorityComboBox.getSelectionModel().select(Priority.valueOf(value.getCurrentPriority()));
    }

}
