package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.GetUserSpectraS3Request;
import com.spectralogic.ds3client.commands.spectrads3.GetUserSpectraS3Response;
import com.spectralogic.dsbrowser.gui.components.validation.SessionValidation;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.PropertyItem;
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

    @FXML
    private Button saveSessionButton, openSessionButton, cancelSessionButton, deleteSessionButton;

    @FXML
    private Label selectExistingLabel, createNewLabel;

    @FXML
    private Tooltip saveSessionButtonTooltip, openSessionButtonTooltip, cancelSessionButtonTooltip, deleteSessionButtonTooltip;

    private final Alert ALERT = new Alert(Alert.AlertType.ERROR);

    private final Alert ALERTINFO = new Alert(Alert.AlertType.INFORMATION);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            ALERT.setHeaderText(null);
            ALERTINFO.setHeaderText(null);
            initGUIElement();
            initSessionList();
            initPropertySheet();
        } catch (final Exception e) {
            LOG.error("Failed to load NewSessionPresenter", e);
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
        model.setPortno("80");
        savedSessions.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                model.setSessionName(newSelection.getName());
                model.setEndpoint(newSelection.getEndpoint());
                model.setAccessKey(newSelection.getCredentials().getAccessId());
                model.setPortno(newSelection.getPortNo());
                model.setSecretKey(newSelection.getCredentials().getSecretKey());
                model.setProxyServer(newSelection.getProxyServer());
            } else {
                clearFields();
            }
        });

        savedSessions.setRowFactory(tv -> {
            final TableRow<SavedSession> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SavedSession rowData = row.getItem();
                    if (store.getObservableList().size() == 0) {
                        final Session connection = createConnection(rowData);
                        if (connection != null) {
                            store.addSession(connection);
                            closeDialog();
                        } else {
                            ALERT.setContentText("Authentication problem. Please check your credentials");
                            ALERT.showAndWait();
                        }
                    } else if (!savedSessionStore.containsNewSessionName(store.getObservableList(), rowData.getName())) {
                        final Session connection = createConnection(rowData);
                        if (connection != null) {
                            store.addSession(connection);
                            closeDialog();
                        } else {
                            ALERT.setContentText("Authentication problem. Please check your credentials");
                            ALERT.showAndWait();
                        }

                    } else {
                        ALERT.setTitle("New User Session");
                        ALERT.setContentText("Session name already in use. Please use a different name.");
                        ALERT.showAndWait();
                    }
                }
            });
            return row;
        });

        savedSessions.setEditable(false);
        savedSessions.setItems(savedSessionStore.getSessions());
    }

    private Session createConnection(final SavedSession rowData) {
        try {
            final Ds3Client client = Ds3ClientBuilder.create(rowData.getEndpoint() + ":" + rowData.getPortNo(), rowData.getCredentials().toCredentials()).withHttps(false).withProxy(rowData.getProxyServer()).build();
            final GetUserSpectraS3Response userSpectraS3 = client.getUserSpectraS3(new GetUserSpectraS3Request(client.getConnectionDetails().getCredentials().getClientId()));
            return new Session(rowData.getName(), rowData.getEndpoint(), rowData.getPortNo(), rowData.getProxyServer(), client);
        } catch (final Exception e) {
            return null;
        }
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        closeDialog();
    }

    public void deleteSession() {
        LOG.info("Deleting the saved session");
        if (savedSessions.getSelectionModel().getSelectedItem() == null) {
            ALERT.setTitle("Information !!");
            ALERT.setContentText("Select saved session to delete !!");
            ALERT.showAndWait();
        } else {
            savedSessionStore.removeSession(savedSessions.getSelectionModel().getSelectedItem());
            ALERTINFO.setContentText("Session deleted successfully.");
            ALERTINFO.showAndWait();
        }
    }

    public void clearFields() {
        model.setEndpoint(null);
        model.setSecretKey(null);
        model.setAccessKey(null);
        model.setPortno("80");
        model.setProxyServer(null);
        model.setSessionName(null);
    }

    public void createSession() {
        LOG.info("Performing session validation");
        ALERT.setTitle("New User Session");

        if (store.getObservableList().size() == 0) {
            if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
                ALERT.setContentText("Please Enter name for the session.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
                ALERT.setContentText("Please Enter valid Data Path address.");
                ALERT.showAndWait();
            } else if (!SessionValidation.validatePort(model.getPortNo())) {
                ALERT.setContentText("Please Enter valid port number for the session.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
                ALERT.setContentText("Please Enter Spectra S3 Data Path Access Key.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
                ALERT.setContentText("Please Enter Spectra S3 Data Path Secret Key.");
                ALERT.showAndWait();
            } else {
                final Session session = model.toSession();
                if (session != null) {
                    store.addSession(session);
                    closeDialog();
                } else {
                    ALERT.setContentText("Authentication problem. Please check your credentials");
                    ALERT.showAndWait();
                }
            }
        } else if (!savedSessionStore.containsNewSessionName(store.getObservableList(), model.getSessionName())) {
            if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
                ALERT.setContentText("Please Enter name for the session.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
                ALERT.setContentText("Please Enter Data Path address.");
                ALERT.showAndWait();
            } else if (!SessionValidation.validatePort(model.getPortNo())) {
                ALERT.setContentText("Please Enter valid port number for the session.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
                ALERT.setContentText("Please Enter Spectra S3 Data Path Access Key.");
                ALERT.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
                ALERT.setContentText("Please Enter Spectra S3 Data Path Secret Key.");
                ALERT.showAndWait();
            } else {
                final Session session = model.toSession();
                if (session != null) {
                    store.addSession(session);
                    closeDialog();
                } else {
                    ALERT.setContentText("Authentication problem. Please check your credentials");
                    ALERT.showAndWait();
                }
            }
        } else {
            ALERT.setContentText("Session name already in use. Please use a different name.");
            ALERT.showAndWait();
        }
    }

    public void saveSession() {
        LOG.info("Creating new session");
        ALERT.setTitle("New User Session");
        if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
            ALERT.setContentText("Please Enter name for the session. !!");
            ALERT.showAndWait();
        } else if (!SessionValidation.checkStringEmptyNull(model.getEndpoint())) {
            ALERT.setContentText("Please Enter the Data Path address. !!");
            ALERT.showAndWait();
        } else if (!SessionValidation.validatePort(model.getPortNo())) {
            ALERT.setContentText("Please Enter valid port number for the session. !!");
            ALERT.showAndWait();
        } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
            ALERT.setContentText("Please Enter Spectra S3 Data Path Access Key. !!");
            ALERT.showAndWait();
        } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
            ALERT.setContentText("Please Enter Spectra S3 Data Path Secret Key. !!");
            ALERT.showAndWait();
        } else {
            final Session session = model.toSession();
            if (session != null) {
                final int previousSize = savedSessionStore.getSessions().size();
                final int i = savedSessionStore.saveSession(session);
                if (i == -1) {
                    ALERT.setContentText("No new changes to update");
                    ALERT.showAndWait();
                } else if (i == -2) {
                    ALERT.setContentText("Session name already in use. Please use a different name.");
                    ALERT.showAndWait();
                } else {
                    savedSessions.getSelectionModel().select(i);
                    try {
                        SavedSessionStore.saveSavedSessionStore(savedSessionStore);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                    if (i <= previousSize) {
                        ALERTINFO.setContentText("Session updated successfully.");
                    } else {
                        ALERTINFO.setContentText("Session saved successfully.");
                    }
                    ALERTINFO.showAndWait();
                }
            } else {
                ALERT.setContentText("Authentication problem. Please check your credentials");
                ALERT.showAndWait();
            }
        }
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) propertySheetAnchor.getScene().getWindow();
        popupStage.close();
    }

    private void initPropertySheet() {

        final ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();

        items.add(new PropertyItem(resourceBundle.getString("nameLabel"), model.sessionNameProperty(), "Access Credentials", "The name for the session", String.class));
        items.add(new PropertyItem(resourceBundle.getString("endpointLabel"), model.endpointProperty(), "Access Credentials", "The Spectra S3 BlackPearl data path address. Can be a DNS entry or IP address", String.class));
        items.add(new PropertyItem(resourceBundle.getString("portNo"), model.portNoProperty(), "Access Credentials", "The port number for the session", String.class));
        items.add(new PropertyItem(resourceBundle.getString("proxyServer"), model.proxyServerProperty(), "Access Credentials", "Provide in format http(s)://server:port, e.g. \"http://localhost:8080\"", String.class));
        items.add(new PropertyItem(resourceBundle.getString("accessIDLabel"), model.accessKeyProperty(), "Access Credentials", "The Spectra S3 Data Path Access ID", String.class));
        items.add(new PropertyItem(resourceBundle.getString("secretIDLabel"), model.secretKeyProperty(), "Access Credentials", "The Spectra S3 Data Path Secret Key", String.class));

        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);

        propertySheetAnchor.getChildren().add(propertySheet);
    }
}
