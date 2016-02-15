package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionView;
import com.spectralogic.dsbrowser.gui.services.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.util.Popup;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

    @FXML
    TabPane ds3SessionTabPane;

    @Inject
    Ds3SessionStore store;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            LOG.info("Loading Ds3PanelPresenter");
            initMenuItems();
            initTab();
            initTabPane();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3PanelPresenter", e);
            throw e;
        }
    }

    private void initTabPane() {
        //TODO create a default tab for now.  In the future, get any existing tabs from the session store

        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ds3SessionTabPane.getTabs().size() > 1 && newValue == addNewTab) {
                // popup new session dialog box
                newSessionDialog();
            }
        });

        final Ds3TreeTableView treeView = new Ds3TreeTableView();
        final Tab treeTab = new Tab("192.168.56.101", treeView.getView());
        treeTab.setClosable(true);
        ds3SessionTabPane.getTabs().add(0, treeTab);
        ds3SessionTabPane.getSelectionModel().select(0);
    }

    public void newSessionDialog() {
        final NewSessionView newSessionView = new NewSessionView();
        Popup.show(newSessionView.getView(), "New Session");
        // this will end up returning a new session.
        // create a new tab
        final Ds3TreeTableView newTreeView = new Ds3TreeTableView();
        final Tab treeTab = new Tab("192.168.56.102", newTreeView.getView());
        final int totalTabs = ds3SessionTabPane.getTabs().size();
        ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
        ds3SessionTabPane.getSelectionModel().select(treeTab);

    }

    private void initTab() {
        addNewTab.setGraphic(Icon.getIcon(FontAwesomeIcon.PLUS));
    }

    private void initMenuItems() {
        ds3Refresh.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
        ds3NewFolder.setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));
    }
}


