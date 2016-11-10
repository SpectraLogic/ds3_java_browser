package com.spectralogic.dsbrowser.gui.components.modifyjobpriority;

import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.ModifyJobSpectraS3Response;
import com.spectralogic.ds3client.models.Priority;
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
                final ModifyJobSpectraS3Response modifyJobSpectraS3Response = value.getSession().getClient().modifyJobSpectraS3(new ModifyJobSpectraS3Request(value.getJobID()).withPriority(newPriority));
                final Stage popupStage = (Stage) modifyJobPriorityComboBox.getScene().getWindow();
                popupStage.close();
            } catch (final IOException e) {
                e.printStackTrace();
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
