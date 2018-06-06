/*
 * ******************************************************************************
 *    Copyright 2016-2018 Spectra Logic Corporation. All Rights Reserved.
 *    Licensed under the Apache License, Version 2.0 (the "License"). You may not use
 *    this file except in compliance with the License. A copy of the License is located at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    or in the "license" file accompanying this file.
 *    This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 *    CONDITIONS OF ANY KIND, either express or implied. See the License for the
 *    specific language governing permissions and limitations under the License.
 * ******************************************************************************
 */

package com.spectralogic.dsbrowser.gui;

import com.spectralogic.ds3client.utils.Guard;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.JobInfoView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionView;
import com.spectralogic.dsbrowser.gui.components.settings.SettingsView;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.settings.ShowCachedJobSettings;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import com.spectralogic.dsbrowser.gui.util.treeItem.SafeHandler;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import org.controlsfx.control.TaskProgressView;
import org.fxmisc.richtext.InlineCssTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.scene.text.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.*;

@Singleton
@Presenter
public class DeepStorageBrowserPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);
    private final TaskProgressView<Ds3JobTask> jobProgressView = new TaskProgressView<>();
    private final ImageView interruptedJobImageView = new ImageView(ImageURLs.INTERRUPTED_JOB_IMAGE);
    private final ImageView cancelAllJobsImageView = new ImageView(ImageURLs.CANCEL_ALL_JOB_IMAGE);
    private final ImageView showCachedJobsImageView = new ImageView(ImageURLs.NO_BLACKPEARL_CACHE);
    private final ImageView showPersistedJobsImageView = new ImageView(ImageURLs.BLACKPEARL_CACHE);

    private final StackPane stackpane = new StackPane();
    private final AnchorPane anchorPane = new AnchorPane();
    private final VBox jobProgressVBox = new VBox();
    private final Circle numInterruptedJobsCircle = new Circle();
    private final Label numInterruptedJobsLabel = new Label();
    private final Button recoverInterruptedJobsButton = new Button();
    private final Button cancelInterruptedJobsButton = new Button();
    private final Button toggleShowCachedJobsButton = new Button();

    @FXML
    private AnchorPane fileSystem, blackPearl;

    @FXML
    private SplitPane jobSplitter;

    @FXML
    private CheckMenuItem jobsMenuItem, logsMenuItem;

    @FXML
    private TabPane bottomTabPane;

    @FXML
    private Tab jobsTab, logsTab;

    @FXML
    private ListView<Text> logView;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private MenuItem aboutMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem, selectAllItem;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu, editMenu;

    private final JobWorkers jobWorkers;
    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final JobInterruptionStore jobInterruptionStore;
    private final ShowCachedJobSettings showCachedJobSettings;
    private final LoggingService loggingService;
    private final CancelJobsWorker cancelJobsWorker;
    private final Popup popup;
    private final Ds3Alert ds3Alert;
    private final CloseConfirmationHandler closeConfirmationHandler;

    @Inject
    public DeepStorageBrowserPresenter(final JobWorkers jobWorkers,
            final ResourceBundle resourceBundle,
            final Ds3Common ds3Common,
            final JobInterruptionStore jobInterruptionStore,
            final SettingsStore settingsStore,
            final LoggingService loggingService,
            final CancelJobsWorker cancelJobsWorker,
            final Popup popup,
            final Ds3Alert ds3Alert,
            final CloseConfirmationHandler closeConfirmationHandler) {
        this.jobWorkers = jobWorkers;
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.jobInterruptionStore = jobInterruptionStore;
        this.showCachedJobSettings = settingsStore.getShowCachedJobSettings();
        this.loggingService = loggingService;
        this.cancelJobsWorker = cancelJobsWorker;
        this.popup = popup;
        this.ds3Alert = ds3Alert;
        this.closeConfirmationHandler = closeConfirmationHandler;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            registerLoggingServiceListener();

            LOG.info("Loading Main view");
            loggingService.logInternationalMessage("loadMainView", LogType.INFO);

            initMenus(); //Setting up labels from resource file

            final SetToolTipBehavior setToolTipBehavior = new SetToolTipBehavior();
            setToolTipBehavior.setToolTipBehaviors(Constants.OPEN_DELAY, Constants.DURATION, Constants.CLOSE_DELAY); //To set the time interval of tooltip

            initLocalTreeTableView();

            initRecoverInterruptedJobsButton();
            initCancelInterruptedJobsButton();
            initToggleShowCachedJobsButton();
            initJobsPane();
            initDs3TreeTableView();

            logView.focusedProperty().addListener((observable, oldValue, newValue) -> selectAllItem.setDisable(oldValue));
            logView.setFocusTraversable(false);
            logView.getItems().addListener((ListChangeListener<? super Text>) c -> {
                final ObservableList<? extends Text> cList = c.getList();
                final int size = cList.size();
                logView.scrollTo(size);
                if (size >= 1000) {
                    cList.remove(0, 100);
                }
            });
        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating Main view", e);
            loggingService.logInternationalMessage("errorWhileCreatingMainView", LogType.ERROR);
        }
    }

    private void initLocalTreeTableView() {
        final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView();
        localTreeView.getViewAsync(fileSystem.getChildren()::add);
    }

    private void initDs3TreeTableView() {
        final Ds3PanelView ds3PanelView = new Ds3PanelView();
        ds3PanelView.getViewAsync(blackPearl.getChildren()::add);
    }

    private void initJobsPane() {
        numInterruptedJobsCircle.radiusProperty().setValue(8.0);
        numInterruptedJobsCircle.setStrokeType(StrokeType.INSIDE);
        numInterruptedJobsCircle.setVisible(false);
        numInterruptedJobsCircle.setFill(Color.RED);

        numInterruptedJobsLabel.setTextFill(Color.WHITE);

        stackpane.setLayoutX(45);
        stackpane.setLayoutY(1);
        stackpane.getChildren().add(numInterruptedJobsCircle);
        stackpane.getChildren().add(numInterruptedJobsLabel);

        anchorPane.getChildren().addAll(recoverInterruptedJobsButton, stackpane, cancelInterruptedJobsButton, toggleShowCachedJobsButton);
        anchorPane.setMinHeight(35);

        Bindings.bindContentBidirectional(jobWorkers.getTasks(), jobProgressView.getTasks());
        jobProgressView.setSkin(new DeepStorageTaskProgressViewSkin<>(jobProgressView, showCachedJobSettings.showCachedJobEnableProperty(), ds3Alert));

        jobProgressView.setPrefHeight(1000);
        jobProgressVBox.getChildren().add(anchorPane);
        jobProgressVBox.getChildren().add(jobProgressView);

        jobsTab.setContent(jobProgressVBox);
        logsTab.setContent(scrollPane);

        jobSplitter.getItems().add(bottomTabPane);
        jobSplitter.setDividerPositions(0.95);
    }

    //Implementation and design of "Recover Interrupted Jobs" button in bottom pane
    private void initRecoverInterruptedJobsButton() {

        interruptedJobImageView.setFitHeight(15);
        interruptedJobImageView.setFitWidth(15);
        recoverInterruptedJobsButton.setTranslateX(20);
        recoverInterruptedJobsButton.setTranslateY(4);
        final Tooltip jobToolTip = new Tooltip();
        jobToolTip.setText(resourceBundle.getString("interruptedJobs"));
        recoverInterruptedJobsButton.setTooltip(jobToolTip);
        recoverInterruptedJobsButton.setGraphic(interruptedJobImageView);
        recoverInterruptedJobsButton.setDisable(true);
        recoverInterruptedJobsButton.setOnAction(SafeHandler.logHandle(event -> {
            final Session session = ds3Common.getCurrentSession();
            if (session != null) {
                final String endpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                final List<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, jobProgressView, null);
                if (!Guard.isMapNullOrEmpty(jobIDMap)) {
                    recoverInterruptedJobsButton.setDisable(false);
                    final JobInfoView jobView = new JobInfoView(new EndpointInfo(endpoint, session.getClient(), jobIDMap, this, ds3Common));
                    popup.show(jobView.getView(), resourceBundle.getString("interruptedJobsPopUp") + StringConstants.SPACE + endpoint);
                } else {
                    recoverInterruptedJobsButton.setDisable(true);
                }

            } else {
                recoverInterruptedJobsButton.setDisable(true);
                loggingService.logInternationalMessage("noInterruptedJobs", LogType.INFO);
            }
        }));
    }

    //Implementation and design of "Cancel All Interrupted Jobs" button in bottom pane
    private void initCancelInterruptedJobsButton() {
        cancelAllJobsImageView.setFitHeight(15);
        cancelAllJobsImageView.setFitWidth(15);
        cancelInterruptedJobsButton.setTranslateX(70);
        cancelInterruptedJobsButton.setTranslateY(4);
        final Tooltip cancelAllToolTip = new Tooltip();
        cancelAllToolTip.setText(resourceBundle.getString("cancelAllRunningJobs"));
        cancelInterruptedJobsButton.setTooltip(cancelAllToolTip);
        cancelInterruptedJobsButton.setGraphic(cancelAllJobsImageView);
        cancelInterruptedJobsButton.disableProperty().bind(Bindings.size(jobProgressView.getTasks()).lessThan(1));
        cancelInterruptedJobsButton.setOnAction(SafeHandler.logHandle(event -> {
            final Optional<ButtonType> closeResponse = ds3Alert.showConfirmationAlert(
                    resourceBundle.getString("confirmation"), jobWorkers.getTasks().size()
                            + StringConstants.SPACE + resourceBundle.getString("jobsWillBeCancelled"),
                    Alert.AlertType.CONFIRMATION, resourceBundle.getString("reallyWantToCancel"),
                    resourceBundle.getString("exitBtnJobCancelConfirm"),
                    resourceBundle.getString("cancelBtnJobCancelConfirm"));
            closeResponse.ifPresent(cR -> {
                if (cR.equals(ButtonType.OK)) {
                    cancelJobsWorker.cancelAllRunningJobs(jobWorkers, jobInterruptionStore);
                }
            });
            event.consume();
        }));
    }

    private void initToggleShowCachedJobsButton() {
        showCachedJobsImageView.setFitHeight(15);
        showCachedJobsImageView.setFitWidth(15);

        showPersistedJobsImageView.setFitHeight(15);
        showPersistedJobsImageView.setFitWidth(15);

        toggleShowCachedJobsButton.setTranslateX(120);
        toggleShowCachedJobsButton.setTranslateY(4);

        final Tooltip toggleShowCachedJobsTooltip = new Tooltip();
        toggleShowCachedJobsTooltip.setText(resourceBundle.getString("toggleShowCachedJobs"));
        toggleShowCachedJobsButton.setTooltip(toggleShowCachedJobsTooltip);
        updateToggleShowCachedJobsButtonGraphic();

        toggleShowCachedJobsButton.setOnAction(SafeHandler.logHandle(event -> {
            LOG.debug("Click ToggleShowCachedJobsButton");

            showCachedJobSettings.setShowCachedJob(!showCachedJobSettings.getShowCachedJob());
            updateToggleShowCachedJobsButtonGraphic();
        }));
    }

    private void updateToggleShowCachedJobsButtonGraphic() {
        if (showCachedJobSettings.getShowCachedJob()) {
            toggleShowCachedJobsButton.setGraphic(showPersistedJobsImageView);
        } else {
            toggleShowCachedJobsButton.setGraphic(showCachedJobsImageView);
        }
    }

    private void initMenus() {
        fileMenu.setText(resourceBundle.getString("fileMenu"));
        sessionsMenuItem.setText(resourceBundle.getString("sessionsMenuItem"));
        settingsMenuItem.setText(resourceBundle.getString("settingsMenuItem"));
        closeMenuItem.setText(resourceBundle.getString("closeMenuItem"));
        closeMenuItem.setOnAction(SafeHandler.logHandle(event -> {
            closeConfirmationHandler.closeConfirmationAlert(event);
        }));

        editMenu.setText(resourceBundle.getString("editMenu"));
        selectAllItem.setText(resourceBundle.getString("selectAllItem"));
        selectAllItem.setDisable(true);

        viewMenu.setText(resourceBundle.getString("viewMenu"));
        logsMenuItem.setText(resourceBundle.getString("logsMenuItem"));
        logsMenuItem.setOnAction(SafeHandler.logHandle(event -> logsORJobsMenuItemAction(logsMenuItem, null, scrollPane, logsTab)));
        jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
        jobsMenuItem.selectedProperty().setValue(true);
        logsMenuItem.selectedProperty().setValue(true);
        jobsMenuItem.setOnAction(SafeHandler.logHandle(event -> {
            logsORJobsMenuItemAction(jobsMenuItem, jobProgressVBox, null, jobsTab);
        }));

        helpMenu.setText(resourceBundle.getString("helpMenu"));
        aboutMenuItem.setText(resourceBundle.getString("aboutMenuItem"));
    }

    public void showSettingsPopup() {
        final SettingsView settingsView = new SettingsView();
        popup.show(settingsView.getView(), resourceBundle.getString("settingsMenuItem"), true);
    }

    public void showAboutPopup() {
        final AboutView aboutView = new AboutView();
        popup.show(aboutView.getView(), resourceBundle.getString("aboutMenuItem"));
    }

    public void showSessionPopup() {
        popup.show(new NewSessionView().getView(), resourceBundle.getString("sessionsMenuItem"));
    }

    public void selectAllItemsInPane() {
        final Node focused = this.getBlackPearl().getScene().getFocusOwner();
        if (focused instanceof TreeTableView) {
            ((TreeTableView) focused).getSelectionModel().selectAll();
        } else if (focused instanceof InlineCssTextArea) {
            ((InlineCssTextArea) focused).selectAll();
        }
    }

    private void logsORJobsMenuItemAction(final CheckMenuItem menuItem, final VBox vBox, final ScrollPane pane, final Tab tab) {
        final ObservableList<Node> items = jobSplitter.getItems();
        final ObservableList<Tab> tabs = bottomTabPane.getTabs();
        if (menuItem.isSelected()) {
            if (menuItem.getId().contains("jobsMenuItem")) {
                tab.setContent(vBox);
                tabs.add(0, tab);
            } else {
                tab.setContent(pane);
                tabs.add(tab);
            }
            bottomTabPane.getSelectionModel().select(tab);
            if (items.stream().noneMatch(i -> i instanceof TabPane)) {
                items.add(bottomTabPane);
                jobSplitter.setDividerPositions(0.75);
            }
        } else {
            if (!menuItem.isSelected())
                tabs.remove(tab);
        }
        if (tabs.size() == 0) {
            items.remove(bottomTabPane);
        }
    }

    private void registerLoggingServiceListener() {
        LOG.info("Registering loggingService observable");
        final Observable<LoggingService.LogEvent> observable = loggingService.getLoggerObservable();
        observable.observeOn(JavaFxScheduler.platform())
                .doOnNext(logEvent -> this.logTextForParagraph(logEvent.getMessage(), logEvent.getLogType()))
                .subscribe();
    }

    //set the same color for all the lines of string log separated by \n
    private void logTextForParagraph(final String log, final LogType type) {
        final Text area = new Text(log);
        area.setWrappingWidth(logView.getWidth());
        setStyleForLogMessage(area, type);
        logView.getItems().add(area);
        scrollPane.setVvalue(1.0);
    }

    private void setStyleForLogMessage(final Text logItem, final LogType type) {
        switch (type) {
            case SUCCESS:
                logItem.setFill(Color.GREEN);
                break;
            case ERROR:
                logItem.setFill(Color.RED);
                break;
            default:
                logItem.setFill(Color.BLACK);
        }
    }


    public MenuItem getSelectAllMenuItem() {
        return selectAllItem;
    }

    public Label getNumInterruptedJobsLabel() {
        return numInterruptedJobsLabel;
    }

    public Button getRecoverInterruptedJobsButton() {
        return recoverInterruptedJobsButton;
    }

    public Circle getNumInterruptedJobsCircle() {
        return numInterruptedJobsCircle;
    }

    private AnchorPane getBlackPearl() {
        return blackPearl;
    }

    public TaskProgressView<Ds3JobTask> getJobProgressView() {
        return jobProgressView;
    }
}
