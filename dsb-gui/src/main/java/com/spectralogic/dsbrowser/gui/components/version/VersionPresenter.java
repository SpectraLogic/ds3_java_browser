package com.spectralogic.dsbrowser.gui.components.version;

import com.google.common.collect.ImmutableList;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;

import java.net.URL;
import java.util.*;

public class VersionPresenter implements Initializable {
    @FXML
    private Button download;

    @FXML
    private TableView<VersionItem> versions;

    @ModelContext
    private VersionModel versionModel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        versions.getItems().addAll(versionModel.getVersionItems());
        download.setOnMouseClicked(event -> {

        });
    }
}
