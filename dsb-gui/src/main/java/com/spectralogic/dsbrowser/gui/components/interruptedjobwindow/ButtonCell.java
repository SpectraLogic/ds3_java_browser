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

package com.spectralogic.dsbrowser.gui.components.interruptedjobwindow;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3CancelSingleJobTask;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * The far right column in the JobInfoPresenter contains a "ButtonCell" per row, with a "Recover" green check
 * or a "Cancel" red x
 */
public class ButtonCell extends TreeTableCell<JobInfoModel, Boolean> {
    private final static Logger LOG = LoggerFactory.getLogger(ButtonCell.class);
    private final HBox hbox;
    private final Button recoverButton = new Button();
    private final Button cancelButton = new Button();

    @Inject
    public ButtonCell(final ResourceBundle resourceBundle) {
        this.hbox = createHBox(resourceBundle, recoverButton, cancelButton);
    }

    //Display button if the row is not empty
    @Override
    protected void updateItem(final Boolean t, final boolean empty) {
        super.updateItem(t, empty);
        if (getTreeTableRow().getTreeItem() != null) {
            if (getTreeTableRow().getTreeItem().getValue().getType().equals(JobInfoModel.Type.JOBID)) {
                setGraphic(hbox);
            } else {
                setGraphic(null);
            }
        } else {
            setGraphic(null);
        }
    }

    Button getRecoverButton() {
        return this.recoverButton;
    }

    Button getCancelButton() {
        return this.cancelButton;
    }

    private static Button createRecoverButton(final ResourceBundle resourceBundle,
                                              final Button recoverButton) {
        final ImageView recoverImageView = new ImageView(ImageURLs.RECOVER_IMAGE);
        recoverImageView.setFitHeight(15);
        recoverImageView.setFitWidth(15);
        recoverButton.setGraphic(recoverImageView);
        recoverButton.setStyle("-fx-background-color: transparent;");
        recoverButton.setTooltip(new Tooltip(resourceBundle.getString("recoverJob")));

        return recoverButton;
    }

    private static Button createCancelButton(final ResourceBundle resourceBundle,
                                             final Button cancelButton) {
        final ImageView cancelImageView = new ImageView(ImageURLs.CANCEL_RECOVER);
        cancelImageView.setFitHeight(15);
        cancelImageView.setFitWidth(15);
        cancelButton.setGraphic(cancelImageView);
        cancelButton.setStyle("-fx-background-color: transparent;");
        cancelButton.setTooltip(new Tooltip(resourceBundle.getString("cancelJob")));

        return cancelButton;
    }

    private static HBox createHBox(final ResourceBundle resourceBundle,
                                   final Button recoverButton,
                                   final Button cancelButton) {
        final HBox hbox = new HBox();
        hbox.setSpacing(3.0);
        hbox.setAlignment(Pos.CENTER);

        hbox.getChildren().addAll(
                createRecoverButton(resourceBundle, recoverButton),
                createCancelButton(resourceBundle, cancelButton));
        return hbox;
    }

    public interface ButtonCellFactory {
        ButtonCell createButtonCell(final EndpointInfo endpointInfo);
    }
}
