package com.spectralogic.dsbrowser.gui.components.ds3panel;

import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
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
    Button ds3NewBucket;

    @FXML
    Button ds3DeleteButton;

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
                    final Ds3TreeTableView newTreeView = new Ds3TreeTableView(newSession);
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
                    disableMenu(true);
                }
            } else if(c.wasAdded()) {
                disableMenu(false);
            }
        });
    }

    private void initTabPane() {
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

        ds3SessionTabPane.getTabs().addListener((ListChangeListener<? super Tab>) c -> {
            if (c.next() && c.wasRemoved()) {
                // TODO prompt the user to save each session that was closed, if it is not already in the saved session store
            }
        });
    }

    public void deleteDialog() {
        // TODO get the currently selected tab, get the presenter for that tab, and then launch the delete dialog
        ds3SessionTabPane.getSelectionModel().getSelectedItem();
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
        ds3NewBucket.setGraphic(Icon.getIcon(FontAwesomeIcon.ARCHIVE));
        ds3DeleteButton.setGraphic(Icon.getIcon(FontAwesomeIcon.TRASH));
        if (ds3SessionTabPane.getTabs().size() == 1) {
            disableMenu(true);
        }
    }

    private void disableMenu(final boolean disable) {
        ds3Refresh.setDisable(disable);
        ds3NewFolder.setDisable(disable);
        ds3NewBucket.setDisable(disable);
        ds3DeleteButton.setDisable(disable);
    }
}


