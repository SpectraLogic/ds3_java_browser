package com.spectralogic.dsbrowser.gui.components.newsession;

import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientBuilder;
import com.spectralogic.ds3client.commands.spectrads3.GetUserSpectraS3Request;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.PropertyItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class NewSessionPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(NewSessionPresenter.class);

    private final NewSessionModel model = new NewSessionModel();

    @FXML
    AnchorPane propertySheetAnchor;

    @FXML
    TableView<SavedSession> savedSessions;

    @Inject
    Ds3SessionStore store;

    @Inject
    SavedSessionStore savedSessionStore;

    @Inject
    ResourceBundle resourceBundle;

    @FXML
    Button saveSessionButton, openSessionButton, cancelSessionButton, deleteSessionButton;

    @FXML
    Label selectExistingLabel, createNewLabel;

    @FXML
    Tooltip saveSessionButtonTooltip, openSessionButtonTooltip, cancelSessionButtonTooltip, deleteSessionButtonTooltip;

    Alert alert = new Alert(Alert.AlertType.ERROR);

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
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
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
        final Ds3Client client = Ds3ClientBuilder.create(rowData.getEndpoint() + ":" + rowData.getPortNo(), rowData.getCredentials().toCredentials()).withHttps(false).build();
        return new Session(rowData.getName(), rowData.getEndpoint(), rowData.getPortNo(), client);
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
        if (store.getObservableList().size() == 0) {
            if (((model.getSessionName() == null) || (model.getSessionName().equalsIgnoreCase(""))) ||
                    ((model.getEndpoint() == null) || (model.getEndpoint().equalsIgnoreCase("")))
                    || ((model.getPortNo() == null) || (model.getPortNo().equalsIgnoreCase(""))) ||
                    ((model.getAccessKey() == null) || (model.getAccessKey().equalsIgnoreCase("")))
                    || ((model.getSecretKey() == null) || (model.getSecretKey().equalsIgnoreCase("")))) {
                alert.setTitle("Information Dialog");
                alert.setContentText("All field required !!");
                alert.showAndWait();
            } else {
                store.addSession(model.toSession());
                closeDialog();
            }
        } else if (!savedSessionStore.containsNewSessionName(store.getObservableList(), model.getSessionName())) {
            if (((model.getSessionName() == null) || (model.getSessionName().equalsIgnoreCase(""))) ||
                    ((model.getEndpoint() == null) || (model.getEndpoint().equalsIgnoreCase("")))
                    || ((model.getPortNo() == null) || (model.getPortNo().equalsIgnoreCase(""))) ||
                    ((model.getAccessKey() == null) || (model.getAccessKey().equalsIgnoreCase("")))
                    || ((model.getSecretKey() == null) || (model.getSecretKey().equalsIgnoreCase("")))) {
                alert.setTitle("Information Dialog");
                alert.setContentText("All field required !!");
                alert.showAndWait();
            } else {
                store.addSession(model.toSession());
                closeDialog();
            }
        } else {
            alert.setTitle("New User Session");
            alert.setContentText("Session name already in use. Please use a different name.");
            alert.showAndWait();
        }
    }

    public void saveSession() {
        LOG.info("Creating new session");
        if (((model.getSessionName() == null) || (model.getSessionName().equalsIgnoreCase(""))) ||
                ((model.getEndpoint() == null) || (model.getEndpoint().equalsIgnoreCase("")))
                || ((model.getPortNo() == null) || (model.getPortNo().equalsIgnoreCase(""))) ||
                ((model.getAccessKey() == null) || (model.getAccessKey().equalsIgnoreCase("")))
                || ((model.getSecretKey() == null) || (model.getSecretKey().equalsIgnoreCase("")))) {
            alert.setTitle("Information Dialog");
            alert.setContentText("All field required !!");
            alert.showAndWait();
        } else {
            savedSessionStore.saveSession(model.toSession());
        }

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
        items.add(new PropertyItem(resourceBundle.getString("accessIDLabel"), model.accessKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Access ID", String.class));
        items.add(new PropertyItem(resourceBundle.getString("secretIDLabel"), model.secretKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Secret Key", String.class));

        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);
        propertySheetAnchor.getChildren().add(propertySheet);
    }


}
