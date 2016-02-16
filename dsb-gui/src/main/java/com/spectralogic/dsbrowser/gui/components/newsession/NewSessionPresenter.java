package com.spectralogic.dsbrowser.gui.components.newsession;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class NewSessionPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(NewSessionPresenter.class);

    private NewSessionModel result = null;
    private final NewSessionModel model = new NewSessionModel();

    @FXML
    AnchorPane propertySheetAnchor;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initPropertySheet();
        } catch (final Exception e) {
             LOG.error("Failed to load NewSessionPresenter", e);
        }
    }

    public void cancelSession() {
        LOG.info("Cancelling session");
        close();
    }

    public void createSession() {
        LOG.info("Performing session validation");
        // perform any validation
        this.result = model;
        close();
    }

    private void close() {
        final Stage popupStage = (Stage) propertySheetAnchor.getScene().getWindow();
        popupStage.close();
    }

    private void initPropertySheet() {

        final ObservableList<PropertySheet.Item> items = FXCollections.observableArrayList();

        items.add(new PropertyItem("Endpoint", model.endpointProperty(), "Access Credentials", "The Spectra S3 Endpoint", String.class));
        items.add(new PropertyItem("Access ID", model.accessKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Access ID", String.class));
        items.add(new PropertyItem("Secret Key", model.secretKeyProperty(), "Access Credentials", "The Spectra S3 Endpoint Secret Key", String.class));

        final PropertySheet propertySheet = new PropertySheet(items);
        propertySheet.setMode(PropertySheet.Mode.NAME);
        propertySheet.setModeSwitcherVisible(false);
        propertySheet.setSearchBoxVisible(false);
        propertySheetAnchor.getChildren().add(propertySheet);
    }

    public NewSessionModel getResult() {
        return result;
    }

}
