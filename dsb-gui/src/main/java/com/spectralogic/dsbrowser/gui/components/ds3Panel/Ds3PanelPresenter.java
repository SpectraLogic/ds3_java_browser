package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.Session;
import com.spectralogic.dsbrowser.util.Icon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.List;
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
            initListeners();
        } catch (final Throwable e) {
            LOG.error("Encountered error when creating Ds3PanelPresenter", e);
            throw e;
        }
    }

    private void initListeners() {
        store.getObservableList().addListener((ListChangeListener<Session>) c -> {
            if (c.next() && c.wasAdded()) {
                final List<? extends Session> newItems = c.getAddedSubList();
                newItems.stream().forEach(newSession -> {
                    final Ds3TreeTableView newTreeView = new Ds3TreeTableView();
                    final Tab treeTab = new Tab(newSession.getSessionName(), newTreeView.getView());
                    final int totalTabs = ds3SessionTabPane.getTabs().size();
                    ds3SessionTabPane.getTabs().add(totalTabs - 1, treeTab);
                    ds3SessionTabPane.getSelectionModel().select(treeTab);
                });

            }
        });

        ds3SessionTabPane.getTabs().addListener((ListChangeListener<Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                if (ds3SessionTabPane.getTabs().size() == 1) {
                    ds3Refresh.setDisable(true);
                    ds3NewFolder.setDisable(true);
                }
            } else if(c.wasAdded()) {
                ds3Refresh.setDisable(false);
                ds3NewFolder.setDisable(false);
            }
        });
    }

    private void initTabPane() {
        //TODO create a default tab for now.  In the future, get any existing tabs from the session store

        ds3SessionTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (ds3SessionTabPane.getTabs().size() > 1 && newValue == addNewTab) {
                // popup new session dialog box
                final int sessionCount = store.size();
                newSessionDialog();

                if (sessionCount == store.size()) {
                    // Do not select the new value if NewSessionDialog fails
                    ds3SessionTabPane.getSelectionModel().select(oldValue);
                }
            }
        });
    }

    public void newSessionDialog() {
        NewSessionPopup.show();
    }

    private void initTab() {
        addNewTab.setGraphic(Icon.getIcon(FontAwesomeIcon.PLUS));
    }

    private void initMenuItems() {
        ds3Refresh.setGraphic(Icon.getIcon(FontAwesomeIcon.REFRESH));
        ds3NewFolder.setGraphic(Icon.getIcon(FontAwesomeIcon.FOLDER));
        if (ds3SessionTabPane.getTabs().size() == 1) {
            ds3Refresh.setDisable(true);
            ds3NewFolder.setDisable(true);
        }
    }
}


