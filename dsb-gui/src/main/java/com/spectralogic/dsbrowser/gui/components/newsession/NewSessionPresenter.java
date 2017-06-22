package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.gui.services.newSessionService.NewSessionModelValidation;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.awt.*;
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
    private Label selectExistingLabel, createNewLabel, sessionNameLabel, endpointLabel, accessKeyLabel, secretKeyLabel, portNoLabel, proxyServerLabel;

    @FXML
    private Tooltip saveSessionButtonTooltip, openSessionButtonTooltip, cancelSessionButtonTooltip, deleteSessionButtonTooltip;

    @FXML
    private TextField sessionName, endpoint, accessKey, portNo, proxyServer;

    @FXML
    private CheckBox defaultSession;

    @FXML
    private CustomPasswordTextControl secretKey;

    private final ResourceBundle resourceBundle;
    private final Ds3SessionStore ds3SessionStore;
    private final SavedSessionStore savedSessionStore;
    private final CreateConnectionTask createConnectionTask;

    @Inject
    public NewSessionPresenter(final ResourceBundle resourceBundle,
                               final Ds3SessionStore ds3SessionStore,
                               final SavedSessionStore savedSessionStore,
                               final CreateConnectionTask createConnectionTask) {
        this.resourceBundle = resourceBundle;
        this.ds3SessionStore = ds3SessionStore;
        this.savedSessionStore = savedSessionStore;
        this.createConnectionTask = createConnectionTask;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElement();
            initSessionList();
        } catch (final Exception e) {
            LOG.error("Failed to load NewSessionPresenter: ", e);
        }
    }

    private void initGUIElement() {
        saveSessionButton.setText(resourceBundle.getString("saveSessionButton"));
        openSessionButton.setText(resourceBundle.getString("openSessionButton"));
        cancelSessionButton.setText(resourceBundle.getString("cancelSessionButton"));
        deleteSessionButton.setText(resourceBundle.getString("deleteSessionButton"));
        selectExistingLabel.setText(resourceBundle.getString("selectExistingLabel"));
        createNewLabel.setText(resourceBundle.getString("createNewLabel"));
        sessionNameLabel.setText(resourceBundle.getString("nameLabel"));
        sessionName.textProperty().addListener((obs, oldName, newName) -> {
            model.setSessionName(newName);
        });
        endpointLabel.setText(resourceBundle.getString("endpointLabel"));
        endpoint.textProperty().addListener((obs, oldEndpoint, newEndpoint) -> {
            model.setEndpoint(newEndpoint);
        });
        accessKeyLabel.setText(resourceBundle.getString("accessIDLabel"));
        accessKey.textProperty().addListener((obs, oldAccessKey, newAccessKey) -> {
            model.setAccessKey(newAccessKey);
        });
        secretKeyLabel.setText(resourceBundle.getString("secretIDLabel"));
        secretKey.getTextField().textProperty().addListener((obs, oldSecretKey, newSecretKey) -> {
            model.setSecretKey(newSecretKey);
        });
        portNoLabel.setText(resourceBundle.getString("portNo"));
        portNo.textProperty().addListener((obs, oldPortNo, newPortNo) -> {
            model.setPortno(newPortNo);
        });
        proxyServerLabel.setText(resourceBundle.getString("proxyServer"));
        proxyServer.textProperty().addListener((obs, oldProxy, newProxy) -> {
            model.setProxyServer(newProxy);
        });
        saveSessionButtonTooltip.setText(resourceBundle.getString("saveSessionButtonTooltip"));
        cancelSessionButtonTooltip.setText(resourceBundle.getString("cancelSessionButtonTooltip"));
        openSessionButtonTooltip.setText(resourceBundle.getString("openSessionButtonTooltip"));
        deleteSessionButtonTooltip.setText(resourceBundle.getString("deleteSessionTooltip"));
    }

    private void initSessionList() {
        model.setPortno(Constants.PORT_NUMBER);
        savedSessions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                sessionName.textProperty().setValue(newSelection.getName());
                model.setSessionName(newSelection.getName());
                endpoint.textProperty().setValue(newSelection.getEndpoint());
                model.setEndpoint(newSelection.getEndpoint());
                accessKey.textProperty().setValue(newSelection.getCredentials().getAccessId());
                model.setAccessKey(newSelection.getCredentials().getAccessId());
                secretKey.getTextField().textProperty().setValue(newSelection.getCredentials().getSecretKey());
                model.setSecretKey(newSelection.getCredentials().getSecretKey());
                portNo.textProperty().setValue(newSelection.getPortNo());
                model.setPortno(newSelection.getPortNo());
                proxyServer.textProperty().setValue(newSelection.getProxyServer());
                model.setProxyServer(newSelection.getProxyServer());
                if (newSelection.getDefaultSession() == null) {
                    defaultSession.selectedProperty().setValue(false);
                    model.setDefaultSession(false);
                } else {
                    defaultSession.selectedProperty().setValue(newSelection.getDefaultSession());
                    model.setDefaultSession(newSelection.getDefaultSession());
                }
            } else {
                clearFields();
            }
        });
        savedSessions.setRowFactory(tableView -> {
            final TableRow<SavedSession> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SavedSession rowData = row.getItem();
                    if (ds3SessionStore.getObservableList().size() == 0 || !savedSessionStore.containsNewSessionName(ds3SessionStore.getObservableList(), rowData.getName())) {
                        final Session connection = createConnectionTask.createConnection(SessionModelService.setSessionModel(rowData, rowData.getDefaultSession()), resourceBundle);
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
        endpoint.textProperty().setValue(null);
        model.setEndpoint(null);
        secretKey.getTextField().textProperty().setValue(null);
        model.setSecretKey(null);
        accessKey.textProperty().setValue(null);
        model.setAccessKey(null);
        portNo.textProperty().setValue(null);
        model.setPortno(Constants.PORT_NUMBER);
        proxyServer.textProperty().setValue(null);
        model.setProxyServer(null);
        sessionName.textProperty().setValue(null);
        model.setSessionName(null);
        defaultSession.selectedProperty().setValue(false);
        model.setDefaultSession(false);
    }

    public void openSession() {
        LOG.info("Performing session validation");
        if (Guard.isNullOrEmpty(ds3SessionStore.getObservableList())
            || !savedSessionStore.containsNewSessionName(ds3SessionStore.getObservableList(), model.getSessionName())) {
            if (!NewSessionModelValidation.validationNewSession(model)) {
                final Session session = createConnectionTask.createConnection(model, resourceBundle);
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
                        item.getDefaultSession() != null && item.getDefaultSession().equals(true)).collect(GuavaCollectors.immutableList());
                if (Guard.isNotNullAndNotEmpty(defaultSession) && !model.getSessionName().equals(defaultSession.get(0).getName())) {
                    final Optional<ButtonType> closeResponse = Ds3Alert.showConfirmationAlert(resourceBundle.getString("defaultSession"), resourceBundle.getString("alreadyExistDefaultSession"), Alert.AlertType.CONFIRMATION, null, resourceBundle.getString("yesButton"), resourceBundle.getString("noButton"));
                    if (closeResponse.get().equals(ButtonType.OK)) {
                        newSessionModel.setDefaultSession(true);
                        final Optional<SavedSession> first = defaultSession.stream().findFirst();
                        if (first.isPresent()) {
                            final Session session = createConnectionTask.createConnection(SessionModelService.setSessionModel(first.get(), false), resourceBundle);
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
            final Session session = createConnectionTask.createConnection(newSessionModel, resourceBundle);
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
