package com.spectralogic.dsbrowser.gui.components.newsession;

import java.net.URL;
import java.util.ResourceBundle;

import javax.inject.Inject;

import javafx.beans.property.StringProperty;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

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

    private Alert alert = new Alert(Alert.AlertType.ERROR);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            alert.setHeaderText(null);
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
        savedSessions.setRowFactory(tv -> {
            final TableRow<SavedSession> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && (!row.isEmpty())) {
                    model.setSessionName(savedSessions.getSelectionModel().getSelectedItem().getName());
                    model.setEndpoint(savedSessions.getSelectionModel().getSelectedItem().getEndpoint());
                    model.setPortno(savedSessions.getSelectionModel().getSelectedItem().getPortNo());
                    model.setAccessKey(savedSessions.getSelectionModel().getSelectedItem().getCredentials().getAccessId());
                    model.setSecretKey(savedSessions.getSelectionModel().getSelectedItem().getCredentials().getSecretKey());
                } else if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    final SavedSession rowData = row.getItem();
                    if (store.getObservableList().size() == 0) {
                        store.addSession(createConnection(rowData));
                        closeDialog();
                    } else if (!savedSessionStore.containsNewSessionName(store.getObservableList(), rowData.getName())) {
                        store.addSession(createConnection(rowData));
                        closeDialog();
                    } else {
                        alert.setTitle("New User Session");
                        alert.setContentText("Session name already in use. Please use a different name.");
                        alert.showAndWait();
                    }
                }
            });
            return row;
        });

        savedSessions.setEditable(false);
        savedSessions.setItems(savedSessionStore.getSessions());
    }

    private Session createConnection(final SavedSession rowData) {
        final Ds3Client client = Ds3ClientBuilder.create(rowData.getEndpoint() + ":" + rowData.getPortNo(), rowData.getCredentials().toCredentials()).withHttps(false).withProxy(rowData.getProxyServer()).build();
        return new Session(rowData.getName(), rowData.getEndpoint(), rowData.getPortNo(), rowData.getProxyServer(), client);
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        closeDialog();
    }

    public void deleteSession() {
        LOG.info("Deleting the saved session");
        if (savedSessions.getSelectionModel().getSelectedItem() == null) {
            alert.setTitle("Information !!");
            alert.setContentText("Select saved session to delete !!");
            alert.showAndWait();
        } else {
            savedSessionStore.removeSession(savedSessions.getSelectionModel().getSelectedItem());
        }
    }

    public void createSession() {
        LOG.info("Performing session validation");
        alert.setTitle("New User Session");

        if (store.getObservableList().size() == 0) {
            if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
                alert.setContentText("Please Enter name for the session.");
                alert.showAndWait();
            } else if (!SessionValidation.validateIPAddress(model.getEndpoint())) {
                alert.setContentText("Please Enter valid IP address.");
                alert.showAndWait();
            } else if (!SessionValidation.validatePort(model.getPortNo())) {
                alert.setContentText("Please Enter valid port number for the session.");
                alert.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
                alert.setContentText("Please Enter Spectra S3 Endpoint Access Key.");
                alert.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
                alert.setContentText("Please Enter Spectra S3 Endpoint Secret Key.");
                alert.showAndWait();
            } else {
                store.addSession(model.toSession());
                closeDialog();
            }
        } else if (!savedSessionStore.containsNewSessionName(store.getObservableList(), model.getSessionName())) {
            if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
                alert.setContentText("Please Enter name for the session.");
                alert.showAndWait();
            } else if (!SessionValidation.validateIPAddress(model.getEndpoint())) {
                alert.setContentText("Please Enter valid IP address.");
                alert.showAndWait();
            } else if (!SessionValidation.validatePort(model.getPortNo())) {
                alert.setContentText("Please Enter valid port number for the session.");
                alert.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
                alert.setContentText("Please Enter Spectra S3 Endpoint Access Key.");
                alert.showAndWait();
            } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
                alert.setContentText("Please Enter Spectra S3 Endpoint Secret Key.");
                alert.showAndWait();
            } else {
                store.addSession(model.toSession());
                closeDialog();
            }
        } else {
            alert.setContentText("Session name already in use. Please use a different name.");
            alert.showAndWait();
        }
    }

    public void saveSession() {
        LOG.info("Creating new session");
        alert.setTitle("New User Session");

        if (!SessionValidation.checkStringEmptyNull(model.getSessionName())) {
            alert.setContentText("Please Enter name for the session. !!");
            alert.showAndWait();
        } else if (!SessionValidation.validateIPAddress(model.getEndpoint())) {
            alert.setContentText("Please Enter valid IP address. !!");
            alert.showAndWait();
        } else if (!SessionValidation.validatePort(model.getPortNo())) {
            alert.setContentText("Please Enter valid port number for the session. !!");
            alert.showAndWait();
        } else if (!SessionValidation.checkStringEmptyNull(model.getAccessKey())) {
            alert.setContentText("Please Enter Spectra S3 Endpoint Access Key. !!");
            alert.showAndWait();
        } else if (!SessionValidation.checkStringEmptyNull(model.getSecretKey())) {
            alert.setContentText("Please Enter Spectra S3 Endpoint Secret Key. !!");
            alert.showAndWait();
        } else
            savedSessionStore.saveSession(model.toSession());
    }

    private void closeDialog() {
        final Stage popupStage = (Stage) propertySheetAnchor.getScene().getWindow();
        popupStage.close();
    }

    private void initPropertySheet() {

        final ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();

        items.add(new PropertyItem(resourceBundle.getString("nameLabel"), model.sessionNameProperty(), "Access Credentials", "The name for the session", String.class));
        items.add(new PropertyItem(resourceBundle.getString("endpointLabel"), model.endpointProperty(), "Access Credentials", "The Spectra S3 Endpoint", String.class));
        items.add(new PropertyItem(resourceBundle.getString("portNo"), model.portNoProperty(), "Access Credentials", "The port number for the session", String.class));
        items.add(new PropertyItem(resourceBundle.getString("proxyServer"), model.proxyServerProperty(), "Access Credentials", "The Proxy Server for the session", String.class));
        items.add(new PropertyItem(resourceBundle.getString("accessIDLabel"), model.accessKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Access ID", String.class));
        items.add(new PropertyItem(resourceBundle.getString("secretIDLabel"), model.secretKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Secret Key", String.class));

        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);
        propertySheetAnchor.getChildren().add(propertySheet);
    }


}
