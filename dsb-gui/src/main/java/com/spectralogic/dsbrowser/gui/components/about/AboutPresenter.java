package com.spectralogic.dsbrowser.gui.components.about;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class AboutPresenter implements Initializable {
    @FXML
    private Label aboutDSBLabel1, aboutDSBLabel2;
    @FXML
    private Hyperlink hyperlink;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        hyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(new URI("http://www.apache.org/licenses/LICENSE-2.0"));
            } catch (IOException e1) {
                e1.printStackTrace();
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        });

        aboutDSBLabel1.setText("Copyright 2016 Spectra Logic Corporation\n\n" +
                "Licensed under the Apache License, Version 2.0\n" +
                "You may not use this application except in compliance with the License.\n" +
                "You may obtain a copy of the License at:-\n");

        aboutDSBLabel2.setText("Unless required by applicable law or agreed to in writing,\n" +
                "software distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either\n" +
                "express or implied.See the License for the specific language governing \n" +
                "permissions and limitations under the License.");
    }
}
