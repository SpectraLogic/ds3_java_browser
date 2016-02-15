package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.services.SessionStore;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ResourceBundle;

public class Ds3PanelPresenter implements Initializable {
    private final static Logger LOG = LoggerFactory.getLogger(Ds3PanelPresenter.class);

    @FXML
    Button ds3Refresh;

    @FXML
    Button ds3NewFolder;

    @FXML
    Tab addNewTab;

    @Inject
    SessionStore store;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            initMenuItems();
            initTab();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3PanelPresenter", e);
            throw e;
        }
    }

    private void initTab() {
        addNewTab.setGraphic(Icon.getIcon(FontAwesomeIcon.PLUS));
    }

    private void initMenuItems() {
        ds3Refresh.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
        ds3NewFolder.setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));
    }
}


