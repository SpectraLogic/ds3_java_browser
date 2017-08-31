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

package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import com.spectralogic.dsbrowser.gui.services.newSessionService.NewSessionModelValidation;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Presenter
public class NewSessionPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(NewSessionPresenter.class);

    private final LazyAlert alert = new LazyAlert("Error");

    private final NewSessionModel model = new NewSessionModel();

    @FXML
    private AnchorPane propertySheetAnchor;

    @FXML
    private TableView<SavedSession> savedSessions;

    @FXML
    private Button saveSessionButton, openSessionButton, cancelSessionButton, deleteSessionButton;

    @FXML
    private Label selectExistingLabel, createNewLabel, sessionNameLabel, endpointLabel, accessKeyLabel, secretKeyLabel, portNoLabel, proxyServerLabel, defaultSessionLabel, useSSLLabel;

    @FXML
    private Tooltip saveSessionButtonTooltip, openSessionButtonTooltip, cancelSessionButtonTooltip, deleteSessionButtonTooltip;

    @FXML
    private TextField sessionName, endpoint, accessKey, portNo, proxyServer;

    @FXML
    private CheckBox defaultSession, useSSL;

    @FXML
    private CustomPasswordTextControl secretKey;

    private final ResourceBundle resourceBundle;
    private final Ds3SessionStore ds3SessionStore;
    private final SavedSessionStore savedSessionStore;
    private final CreateConnectionTask createConnectionTask;
    private final BuildInfoService buildInfoService;

    @Inject
    public NewSessionPresenter(final ResourceBundle resourceBundle,
                               final Ds3SessionStore ds3SessionStore,
                               final SavedSessionStore savedSessionStore,
                               final CreateConnectionTask createConnectionTask,
                               final BuildInfoService buildInfoService) {
        this.resourceBundle = resourceBundle;
        this.ds3SessionStore = ds3SessionStore;
        this.savedSessionStore = savedSessionStore;
        this.createConnectionTask = createConnectionTask;
        this.buildInfoService = buildInfoService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElement();
            initSessionList();
        } catch (final Throwable t) {
            LOG.error("Failed to initialize NewSessionPresenter", t);
        }
    }

    private void initGUIElement() {
        sessionNameLabel.setText(resourceBundle.getString("nameLabel"));
        sessionName.textProperty().bindBidirectional(model.sessionNameProperty());

        endpointLabel.setText(resourceBundle.getString("endpointLabel"));
        endpoint.textProperty().bindBidirectional(model.endpointProperty());

        accessKeyLabel.setText(resourceBundle.getString("accessIDLabel"));
        accessKey.textProperty().bindBidirectional(model.accessKeyProperty());

        secretKeyLabel.setText(resourceBundle.getString("secretIDLabel"));
        secretKey.getTextField().textProperty().bindBidirectional(model.secretKeyProperty());

        portNoLabel.setText(resourceBundle.getString("portNo"));
        portNo.textProperty().bindBidirectional(model.portNoProperty());

        proxyServerLabel.setText(resourceBundle.getString("proxyServer"));
        proxyServer.textProperty().bindBidirectional(model.proxyServerProperty());

        defaultSessionLabel.setText(resourceBundle.getString("defaultSession"));
        defaultSession.selectedProperty().bindBidirectional(model.defaultSessionProperty());

        useSSLLabel.setText(resourceBundle.getString("useSSL"));
        useSSL.selectedProperty().bindBidirectional(model.useSSLProperty());

        saveSessionButton.setText(resourceBundle.getString("saveSessionButton"));
        openSessionButton.setText(resourceBundle.getString("openSessionButton"));
        cancelSessionButton.setText(resourceBundle.getString("cancelSessionButton"));
        deleteSessionButton.setText(resourceBundle.getString("deleteSessionButton"));
        selectExistingLabel.setText(resourceBundle.getString("selectExistingLabel"));
        createNewLabel.setText(resourceBundle.getString("createNewLabel"));

        saveSessionButtonTooltip.setText(resourceBundle.getString("saveSessionButtonTooltip"));
        cancelSessionButtonTooltip.setText(resourceBundle.getString("cancelSessionButtonTooltip"));
        openSessionButtonTooltip.setText(resourceBundle.getString("openSessionButtonTooltip"));
        deleteSessionButtonTooltip.setText(resourceBundle.getString("deleteSessionTooltip"));
    }

    private void initSessionList() {
        model.setPortno(Constants.PORT_NUMBER);
        savedSessions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                final Boolean defaultSession = newSelection.isDefaultSession();
                final Boolean useSSL = newSelection.isUseSSL();
                model.setSessionName(newSelection.getName());
                model.setEndpoint(newSelection.getEndpoint());
                model.setAccessKey(newSelection.getCredentials().getAccessId());
                model.setSecretKey(newSelection.getCredentials().getSecretKey());
                model.setPortno(newSelection.getPortNo());
                model.setProxyServer(newSelection.getProxyServer());
                model.setDefaultSession(defaultSession == null ? false : defaultSession);
                model.setUseSSL(useSSL == null ? false : useSSL);
            } else {
                clearFields();
            }
        });
        savedSessions.setRowFactory(tableView -> {
            final TableRow<SavedSession> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SavedSession rowData = row.getItem();
                    if (ds3SessionStore.getObservableList().size() == 0 || !SavedSessionStore.containsNewSessionName(ds3SessionStore.getObservableList(), rowData.getName())) {
                        final Session connection = CreateConnectionTask.createConnection(SessionModelService.setSessionModel(rowData, rowData.isDefaultSession()), resourceBundle, buildInfoService);
                        sessionValidates(connection);
                    } else {
                        alert.showAlert(resourceBundle.getString("alreadyExistSession"));
                    }
                }
            });
            return row;
        });
        savedSessions.setEditable(false);
        savedSessions.setItems(savedSessionStore.getSessions());
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        closeDialog();
    }

    public void deleteSession() {
        LOG.info("Deleting the saved session");
        if (savedSessions.getSelectionModel().getSelectedItem() == null) {
            alert.showAlert(resourceBundle.getString("selectToDeleteSession"));
        } else {
            if (Guard.isNotNullAndNotEmpty(ds3SessionStore.getObservableList())) {
                ds3SessionStore.getObservableList().forEach(openSession -> {
                    if (savedSessions.getSelectionModel().getSelectedItem().getName().equals(openSession.getSessionName())) {
                        alert.showAlert(resourceBundle.getString("cannotdeletesession"));
                    } else {
                        savedSessionStore.removeSession(savedSessions.getSelectionModel().getSelectedItem());
                        alert.showAlert(resourceBundle.getString("sessionDeletedSuccess"));
                    }
                });
            } else {
                savedSessionStore.removeSession(savedSessions.getSelectionModel().getSelectedItem());
                alert.showAlert(resourceBundle.getString("sessionDeletedSuccess"));
            }
        }
    }

    public void clearFields() {
        endpoint.textProperty().setValue("");
        model.setEndpoint(null);
        secretKey.getTextField().textProperty().setValue("");
        model.setSecretKey(null);
        accessKey.textProperty().setValue("");
        model.setAccessKey(null);
        portNo.textProperty().setValue("");
        model.setPortno(Constants.PORT_NUMBER);
        proxyServer.textProperty().setValue("");
        model.setProxyServer(null);
        sessionName.textProperty().setValue("");
        model.setSessionName(null);
        defaultSession.selectedProperty().setValue(false);
        model.setDefaultSession(false);
        model.setUseSSL(false);
    }

    public void openSession() {
        LOG.info("Performing session validation");
        if (Guard.isNullOrEmpty(ds3SessionStore.getObservableList())
            || !SavedSessionStore.containsNewSessionName(ds3SessionStore.getObservableList(), model.getSessionName())) {
            if (!NewSessionModelValidation.validationNewSession(model)) {
                final Session session = CreateConnectionTask.createConnection(model, resourceBundle, buildInfoService);
                sessionValidates(session);
            }
        } else {
            alert.showAlert(resourceBundle.getString("alreadyExistSession"));
        }
    }

    public void saveSession() {
        LOG.info("Creating new session");
        final NewSessionModel newSessionModel = SessionModelService.copy(model);
        if (!NewSessionModelValidation.validationNewSession(newSessionModel)) {
            if (newSessionModel.getDefaultSession()) {
                final List<SavedSession> defaultSession = savedSessionStore.getSessions().stream().filter(item ->
                        item.isDefaultSession() != null && item.isDefaultSession().equals(true)).collect(GuavaCollectors.immutableList());
                if (Guard.isNotNullAndNotEmpty(defaultSession) && !model.getSessionName().equals(defaultSession.get(0).getName())) {
                    final Optional<ButtonType> closeResponse = Ds3Alert.showConfirmationAlert(resourceBundle.getString("defaultSession"),
                            resourceBundle.getString("alreadyExistDefaultSession"), Alert.AlertType.CONFIRMATION, null,
                            resourceBundle.getString("yesButton"), resourceBundle.getString("noButton"));
                    if (closeResponse.get().equals(ButtonType.OK)) {
                        newSessionModel.setDefaultSession(true);
                        final Optional<SavedSession> first = defaultSession.stream().findFirst();
                        if (first.isPresent()) {
                            final Session session = CreateConnectionTask.createConnection(
                                    SessionModelService.setSessionModel(first.get(), false),
                                    resourceBundle, buildInfoService);
                            if (session != null) {
                                savedSessionStore.addSession(session);
                                try {
                                    SavedSessionStore.saveSavedSessionStore(savedSessionStore);
                                } catch (final Exception e) {
                                    LOG.error("Unable to save saved session:", e);
                                }
                            }
                        }
                    } else {
                        newSessionModel.setDefaultSession(false);
                        model.setDefaultSession(false);
                    }

                }
            }
            final Session session = CreateConnectionTask.createConnection(newSessionModel, resourceBundle, buildInfoService);
            if (session != null) {
                final int previousSize = savedSessionStore.getSessions().size();
                final int i = savedSessionStore.addSession(session);
                if (i == -1) {
                    alert.showAlert(resourceBundle.getString("noNewChanges"));
                } else if (i == -2) {
                    alert.showAlert(resourceBundle.getString("alreadyExistSession"));
                } else {
                    savedSessions.getSelectionModel().select(i);
                    try {
                        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
                    } catch (final IOException e) {
                        LOG.error("Failed to save session: ", e);
                    }
                    if (i <= previousSize) {
                        alert.showAlert(resourceBundle.getString("sessionUpdatedSuccessfully"));
                    } else {
                        alert.showAlert(resourceBundle.getString("sessionUpdatedSuccessfully"));
                    }
                }
            }
        }
    }

    public void closeDialog() {
        final Stage popupStage = (Stage) propertySheetAnchor.getScene().getWindow();
        popupStage.close();
    }

    public void sessionValidates(final Session session) {
        if (session != null) {
            ds3SessionStore.addSession(session);
            closeDialog();
        }
    }
}
