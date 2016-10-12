package com.spectralogic.dsbrowser.gui.components.deletefiles;

import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTablePresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.util.Ds3Task;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class DeleteFilesPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeleteFilesPresenter.class);

    @FXML
    private TextField deleteField;

    @FXML
    private Button deleteButton;

    @FXML
    private Label deleteLabel, deleteConfirmationInfoLabel;

    @Inject
    private Workers workers;

    @Inject
    private Ds3Task deleteTask;

    @Inject
    private Ds3PanelPresenter ds3PanelPresenter;

    @Inject
    private Ds3TreeTablePresenter ds3TreeTablePresenter;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            if (ds3PanelPresenter.getDs3TreeTableView() != null) {
                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedPanelItems = ds3PanelPresenter.getDs3TreeTableView().getSelectionModel().getSelectedItems();
                changeLabelText(selectedPanelItems);
            } else if (ds3TreeTablePresenter.ds3TreeTable != null) {
                final ObservableList<TreeItem<Ds3TreeTableValue>> selectedMenuItems = ds3TreeTablePresenter.ds3TreeTable.getSelectionModel().getSelectedItems();
                changeLabelText(selectedMenuItems);
            }
            deleteField.textProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.equals("DELETE")) {
                    deleteButton.setDisable(false);
                } else {
                    deleteButton.setDisable(true);
                }
            });

            deleteField.setOnKeyReleased(event -> {
                if (!deleteButton.isDisabled() && event.getCode().equals(KeyCode.ENTER)) {
                    deleteFiles();
                }
            });

        } catch (final Throwable e) {
            LOG.error("Encountered an error making the delete file presenter", e);
        }
    }

    public void changeLabelText(final ObservableList<TreeItem<Ds3TreeTableValue>> selectedItems) {
        if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.File)) {
            deleteLabel.setText("DELETE FILE(S)");
            deleteConfirmationInfoLabel.setText("To confirm the deletion of the selected files please type 'DELETE'");
        } else if (selectedItems.get(0).getValue().getType().equals(Ds3TreeTableValue.Type.Directory)) {
            deleteLabel.setText("DELETE FOLDER");
            deleteConfirmationInfoLabel.setText("To confirm the deletion of the selected folder please type 'DELETE'");
        } else {
            deleteLabel.setText("DELETE BUCKET");
            deleteConfirmationInfoLabel.setText("To confirm the deletion of the selected Bucket please type 'DELETE'");
        }
    }

    public void deleteFiles() {
        deleteTask.setOnCancelled(this::handle);
        deleteTask.setOnFailed(this::handle);
        deleteTask.setOnSucceeded(this::handle);

        workers.execute(deleteTask);
    }

    private void handle(final Event event) {
        closeDialog();
    }

    public void cancelDelete() {
        LOG.info("Cancelling delete files");
        closeDialog();
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) deleteField.getScene().getWindow();
        popupStage.close();
    }
}
