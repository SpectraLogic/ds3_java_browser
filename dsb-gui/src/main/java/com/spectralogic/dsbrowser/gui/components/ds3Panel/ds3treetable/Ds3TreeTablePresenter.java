package com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable;

import javafx.fxml.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class Ds3TreeTablePresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3TreeTablePresenter.class);

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3TreeTablePresenter");
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3TreeTablePresenter", e);
            throw e;
        }
    }
}
