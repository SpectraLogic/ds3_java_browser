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

package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.api.injector.Presenter;
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
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Presenter
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

    @ModelContext
    private ModifyJobPriorityModel value;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initGUIElement();
    }

    public void saveModifyJobPriority() {
        final Priority newPriority = modifyJobPriorityComboBox.getValue();
        if (newPriority.equals(Priority.valueOf(value.getCurrentPriority()))) {
            cancelModifyJobPriority();
        } else {
            try {
                value.getSession().getClient().modifyJobSpectraS3(new ModifyJobSpectraS3Request(value.getJobID()).withPriority(newPriority));
                final Stage popupStage = (Stage) modifyJobPriorityComboBox.getScene().getWindow();
                popupStage.close();
            } catch (final IOException e) {
                LOG.error("Failed to modify the job", e);
            }
        }
    }

    public void cancelModifyJobPriority() {
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

}
