package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.gui.services.newSessionService.NewSessionModelValidation;
import com.spectralogic.dsbrowser.gui.services.newSessionService.SessionModelService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.tasks.CreateConnectionTask;
import com.spectralogic.dsbrowser.gui.util.Constants;
import com.spectralogic.dsbrowser.gui.util.Ds3Alert;
import com.spectralogic.dsbrowser.gui.util.PropertyItem;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class NewSessionPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(NewSessionPresenter.class);

    private final NewSessionModel model = new NewSessionModel();

    @FXML
    private AnchorPane propertySheetAnchor;

    @FXML
    private TableView<SavedSession> savedSessions;

    @Inject
    private Ds3SessionStore store;

    @Inject
    private SavedSessionStore savedSessionStore;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private CreateConnectionTask createConnectionTask;

    @FXML
    private Button saveSessionButton, openSessionButton, cancelSessionButton, deleteSessionButton;

    @FXML
    private Label selectExistingLabel, createNewLabel;

    @FXML
    private Tooltip saveSessionButtonTooltip, openSessionButtonTooltip, cancelSessionButtonTooltip, deleteSessionButtonTooltip;


    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElement();
            initSessionList();
            initPropertySheet();
        } catch (final Exception e) {
            LOG.error("Failed to load NewSessionPresenter: {}", e);
        }
    }

    private void initGUIElement() {
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
                model.setSessionName(newSelection.getName());
                model.setEndpoint(newSelection.getEndpoint());
                model.setAccessKey(newSelection.getCredentials().getAccessId());
                model.setPortno(newSelection.getPortNo());
                model.setSecretKey(newSelection.getCredentials().getSecretKey());
                model.setProxyServer(newSelection.getProxyServer());
                if (newSelection.getDefaultSession() == null) {
                    model.setDefaultSession(false);
                } else {
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
                    if (store.getObservableList().size() == 0 || !savedSessionStore.containsNewSessionName(store.getObservableList(), rowData.getName())) {
                        final Session connection = createConnectionTask.createConnection(SessionModelService.setSessionModel(rowData, rowData.getDefaultSession()));
                        sessionValidates(connection);
                    } else {
                        Ds3Alert.show(resourceBundle.getString("newSession"), resourceBundle.getString("alreadyExistSession"), Alert.AlertType.ERROR);
                    }
                }
            });
            return row;
        });
        savedSessions.setEditable(false);
        savedSessions.setItems(savedSessionStore.getSessions());
    }

    private void initPropertySheet() {
        final ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();
        items.add(new PropertyItem(resourceBundle.getString("nameLabel"), model.sessionNameProperty(), "Access Credentials", resourceBundle.getString("nameDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("endpointLabel"), model.endpointProperty(), "Access Credentials", resourceBundle.getString("endPointDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("portNo"), model.portNoProperty(), "Access Credentials", resourceBundle.getString("portNumberDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("proxyServer"), model.proxyServerProperty(), "Access Credentials", resourceBundle.getString("proxyDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("accessIDLabel"), model.accessKeyProperty(), "Access Credentials", resourceBundle.getString("accessKeyDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("secretIDLabel"), model.secretKeyProperty(), "Access Credentials", resourceBundle.getString("secretKeyDescription"), String.class));
        items.add(new PropertyItem(resourceBundle.getString("defaultSession"), model.defaultSessionProperty(), "Access Credentials", resourceBundle.getString("defaultSessionDescription"), Boolean.class));
        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);
        propertySheetAnchor.getChildren().add(propertySheet);
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        closeDialog();
    }

    public void deleteSession() {
        LOG.info("Deleting the saved session");
        if (savedSessions.getSelectionModel().getSelectedItem() == null) {
            Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("selectToDeleteSession"), Alert.AlertType.ERROR);
        } else {
            savedSessionStore.removeSession(savedSessions.getSelectionModel().getSelectedItem());
            Ds3Alert.show(resourceBundle.getString("information"), resourceBundle.getString("sessionDeletedSuccess"), Alert.AlertType.INFORMATION);
        }
    }

    public void clearFields() {
        model.setEndpoint(null);
        model.setSecretKey(null);
        model.setAccessKey(null);
        model.setPortno(Constants.PORT_NUMBER);
        model.setProxyServer(null);
        model.setSessionName(null);
        model.setDefaultSession(false);
    }

    public void openSession() {
        LOG.info("Performing session validation");
        if (Guard.isNullOrEmpty(store.getObservableList()) || !savedSessionStore.containsNewSessionName(store.getObservableList(), model.getSessionName())) {
            if (!NewSessionModelValidation.validationNewSession(model)) {
                final Session session = createConnectionTask.createConnection(model);
                sessionValidates(session);
            }
        } else {
            Ds3Alert.show(resourceBundle.getString("newSession"), resourceBundle.getString("alreadyExistSession"), Alert.AlertType.ERROR);
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
                        // SessionModelService.setSessionModel(defaultSession.stream().findFirst().orElse(null),false);
                        final Optional<SavedSession> first = defaultSession.stream().findFirst();
                        if (first.isPresent()) {
                            final Session session = createConnectionTask.createConnection(SessionModelService.setSessionModel(first.get(), false));
                            if (session != null) {
                                savedSessionStore.saveSession(session);
                                try {
                                    SavedSessionStore.saveSavedSessionStore(savedSessionStore);
                                } catch (final Exception e) {
                                    LOG.error("Unable to save saved session:{} ", e);
                                }
                            }
                        }
                    } else {
                        newSessionModel.setDefaultSession(false);
                        model.setDefaultSession(false);
                    }

                }
            }
            final Session session = createConnectionTask.createConnection(newSessionModel);
            if (session != null) {
                final int previousSize = savedSessionStore.getSessions().size();
                final int i = savedSessionStore.saveSession(session);
                if (i == -1) {
                    Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("noNewChanges"), Alert.AlertType.ERROR);
                } else if (i == -2) {
                    Ds3Alert.show(resourceBundle.getString("error"), resourceBundle.getString("alreadyExistSession"), Alert.AlertType.ERROR);
                } else {
                    savedSessions.getSelectionModel().select(i);
                    try {
                        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
                    } catch (final IOException e) {
                        LOG.error("Failed to save session:{} ", e);
                    }
                    if (i <= previousSize) {
                        Ds3Alert.show(resourceBundle.getString("information"), resourceBundle.getString("sessionUpdatedSuccessfully"), Alert.AlertType.INFORMATION);
                    } else {
                        Ds3Alert.show(resourceBundle.getString("information"), resourceBundle.getString("sessionSavedSuccessfully"), Alert.AlertType.INFORMATION);
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
            store.addSession(session);
            closeDialog();
        }
    }
}
