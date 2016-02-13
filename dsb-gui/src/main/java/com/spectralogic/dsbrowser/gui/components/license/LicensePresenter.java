package com.spectralogic.dsbrowser.gui.components.license;

import com.google.common.collect.ImmutableList;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.ResourceBundle;

public class LicensePresenter implements Initializable {
    @FXML
    TableView<LicenseModel> licenseTable;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        licenseTable.setItems(FXCollections.observableList(getModels()));
    }

    private ImmutableList<LicenseModel> getModels() {
        return ImmutableList.of(new LicenseModel("ds3_java_sdk", "Apache 2"));
    }
}
