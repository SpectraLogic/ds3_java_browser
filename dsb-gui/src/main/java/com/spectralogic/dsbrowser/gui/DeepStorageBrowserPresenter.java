/*
 * ******************************************************************************
 *    Copyright 2016-2017 Spectra Logic Corporation. All Rights Reserved.
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
import com.spectralogic.dsbrowser.api.services.ShutdownService;
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
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.settings.ShowCachedJobSettings;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.beans.binding.Bindings;
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

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Singleton
@Presenter
public class DeepStorageBrowserPresenter implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);
    private final TaskProgressView<Ds3JobTask> jobProgressView = new TaskProgressView<>();
    private final ImageView interruptedJobImageView = new ImageView(ImageURLs.INTERRUPTED_JOB_IMAGE);
    private final ImageView cancelAllJobsImageView = new ImageView(ImageURLs.CANCEL_ALL_JOB_IMAGE);
    private final ImageView showCachedJobsImageView = new ImageView(ImageURLs.BLACKPEARL_CACHE);
    private final ImageView showPersistedJobsImageView = new ImageView(ImageURLs.STORAGE_TAPES);

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
    private InlineCssTextArea inlineCssTextArea;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private MenuItem aboutMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem, selectAllItem;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu, editMenu;

    private final JobWorkers jobWorkers;
    private final ResourceBundle resourceBundle;
    private final SavedJobPrioritiesStore savedJobPrioritiesStore;
    private final Ds3Common ds3Common;
    private final JobInterruptionStore jobInterruptionStore;
    private final SettingsStore settingsStore;
    private final ShowCachedJobSettings showCachedJobSettings;
    private final SavedSessionStore savedSessionStore;
    private final Workers workers;
    private final DateTimeUtils dateTimeUtils;
    private final LoggingService loggingService;
    private final ShutdownService shutdownService;

    @Inject
    public DeepStorageBrowserPresenter(final JobWorkers jobWorkers,
                                       final ResourceBundle resourceBundle,
                                       final SavedJobPrioritiesStore savedJobPrioritiesStore,
                                       final Ds3Common ds3Common,
                                       final JobInterruptionStore jobInterruptionStore,
                                       final SettingsStore settingsStore,
                                       final SavedSessionStore savedSessionStore,
                                       final Workers workers,
                                       final LoggingService loggingService,
                                       final DateTimeUtils dateTimeUtils,
                                       final ShutdownService shutdownService) {
        this.jobWorkers = jobWorkers;
        this.resourceBundle = resourceBundle;
        this.savedJobPrioritiesStore = savedJobPrioritiesStore;
        this.ds3Common = ds3Common;
        this.jobInterruptionStore = jobInterruptionStore;
        this.settingsStore = settingsStore;
        this.showCachedJobSettings = settingsStore.getShowCachedJobSettings();
        this.savedSessionStore = savedSessionStore;
        this.workers = workers;
        this.loggingService = loggingService;
        this.dateTimeUtils = dateTimeUtils;
        this.shutdownService = shutdownService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            registerLoggingServiceListener();

            LOG.info("Loading Main view");
            loggingService.logMessage(resourceBundle.getString("loadMainView"), LogType.INFO);

            initMenus(); //Setting up labels from resource file

            final SetToolTipBehavior setToolTipBehavior = new SetToolTipBehavior();
            setToolTipBehavior.setToolTilBehaviors(Constants.OPEN_DELAY, Constants.DURATION, Constants.CLOSE_DELAY); //To set the time interval of tooltip

            initLocalTreeTableView();
            initDs3TreeTableView();

            initRecoverInterruptedJobsButton();
            initCancelInterruptedJobsButton();
            initToggleShowCachedJobsButton();
            initJobsPane();

            inlineCssTextArea.focusedProperty().addListener((observable, oldValue, newValue) -> {
                selectAllItem.setDisable(oldValue);
            });

        } catch (final Exception e) {
            LOG.error("Encountered an error when creating Main view", e);
            loggingService.logMessage(resourceBundle.getString("errorWhileCreatingMainView"), LogType.ERROR);
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
        jobProgressView.setSkin(new DeepStorageTaskProgressViewSkin<>(jobProgressView));

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
        recoverInterruptedJobsButton.setOnAction(event -> {
            if (ds3Common.getCurrentSession() != null) {
                final Session session = ds3Common.getCurrentSession();
                final String endpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                final List<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
                final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, jobProgressView, null);
                if (!Guard.isMapNullOrEmpty(jobIDMap)) {
                    recoverInterruptedJobsButton.setDisable(false);
                    final JobInfoView jobView = new JobInfoView(new EndpointInfo(endpoint, session.getClient(), jobIDMap, this, ds3Common));
                    Popup.show(jobView.getView(), resourceBundle.getString("interruptedJobsPopUp") +
                            StringConstants.SPACE + endpoint);
                } else {
                    recoverInterruptedJobsButton.setDisable(true);
                }

            } else {
                recoverInterruptedJobsButton.setDisable(true);
                loggingService.logMessage(resourceBundle.getString("noInterruptedJobs"), LogType.INFO);
            }
        });
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
        cancelInterruptedJobsButton.setOnAction(event -> {
            final Optional<ButtonType> closeResponse = Ds3Alert.showConfirmationAlert(
                    resourceBundle.getString("confirmation"), jobWorkers.getTasks().size()
                            + StringConstants.SPACE + resourceBundle.getString("jobsWillBeCancelled"),
                    Alert.AlertType.CONFIRMATION, resourceBundle.getString("reallyWantToCancel"),
                    resourceBundle.getString("exitBtnJobCancelConfirm"),
                    resourceBundle.getString("cancelBtnJobCancelConfirm"));
            if (closeResponse.get().equals(ButtonType.OK)) {
                CancelJobsWorker.cancelAllRunningJobs(jobWorkers, jobInterruptionStore, workers, ds3Common, dateTimeUtils, loggingService);
                event.consume();
            }
            if (closeResponse.get().equals(ButtonType.CANCEL)) {
                event.consume();
            }
        });
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

        toggleShowCachedJobsButton.setOnAction(event -> {
            loggingService.logMessage("Click ToggleShowCachedJobsButton", LogType.INFO);
            LOG.info("Click ToggleShowCachedJobsButton");

            showCachedJobSettings.setShowCachedJob(!showCachedJobSettings.getShowCachedJob());
            updateToggleShowCachedJobsButtonGraphic();
        });
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
        closeMenuItem.setOnAction(event -> {
            final CloseConfirmationHandler closeConfirmationHandler = new CloseConfirmationHandler(resourceBundle, jobWorkers, shutdownService);
            closeConfirmationHandler.closeConfirmationAlert(event);
        });

        editMenu.setText(resourceBundle.getString("editMenu"));
        selectAllItem.setText(resourceBundle.getString("selectAllItem"));
        selectAllItem.setDisable(true);

        viewMenu.setText(resourceBundle.getString("viewMenu"));
        logsMenuItem.setText(resourceBundle.getString("logsMenuItem"));
        logsMenuItem.setOnAction(event -> {
            logsORJobsMenuItemAction(logsMenuItem, null, scrollPane, logsTab);
        });
        jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
        jobsMenuItem.selectedProperty().setValue(true);
        logsMenuItem.selectedProperty().setValue(true);
        jobsMenuItem.setOnAction(event -> {
            logsORJobsMenuItemAction(jobsMenuItem, jobProgressVBox, null, jobsTab);
        });

        helpMenu.setText(resourceBundle.getString("helpMenu"));
        aboutMenuItem.setText(resourceBundle.getString("aboutMenuItem"));
    }

    public void showSettingsPopup() {
        final SettingsView settingsView = new SettingsView();
        Popup.show(settingsView.getView(), resourceBundle.getString("settingsMenuItem"));
    }

    public void showAboutPopup() {
        final AboutView aboutView = new AboutView();
        Popup.show(aboutView.getView(), resourceBundle.getString("aboutMenuItem"));
    }

    public void showSessionPopup() {
        Popup.show(new NewSessionView().getView(), resourceBundle.getString("sessionsMenuItem"));
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
        if (menuItem.isSelected()) {
            if (menuItem.getId().contains("jobsMenuItem")) {
                tab.setContent(vBox);
                bottomTabPane.getTabs().add(0, tab);
            } else {
                tab.setContent(pane);
                bottomTabPane.getTabs().add(tab);
            }
            bottomTabPane.getSelectionModel().select(tab);
            if (jobSplitter.getItems().stream().noneMatch(i -> i instanceof TabPane)) {
                jobSplitter.getItems().add(bottomTabPane);
                jobSplitter.setDividerPositions(0.75);
            }
        } else {
            if (!menuItem.isSelected())
                bottomTabPane.getTabs().remove(tab);
        }
        if (bottomTabPane.getTabs().size() == 0) {
            jobSplitter.getItems().remove(bottomTabPane);
        }
    }

    private void registerLoggingServiceListener() {
        LOG.info("Registering loggingService observable");
        final Observable<LoggingService.LogEvent> observable = loggingService.getLoggerObservable();
        observable.observeOn(JavaFxScheduler.platform())
                .doOnNext(logEvent -> this.logTextForParagraph(logEvent.getMessage(), logEvent.getLogType()))
                .subscribe();
    }

    private void logText(final String log, final LogType type) {
        if (inlineCssTextArea != null) {
            inlineCssTextArea.appendText(formattedString(log));
            final int size = inlineCssTextArea.getParagraphs().size() - 2;

            setStyleForLogMessage(type, size);
            scrollPane.setVvalue(1.0);
        }
    }

    //set the same color for all the lines of string log separated by \n
    private void logTextForParagraph(final String log, final LogType type) {
        final int previousSize = inlineCssTextArea.getParagraphs().size() - 2;
        inlineCssTextArea.appendText(formattedString(log));
        final int size = inlineCssTextArea.getParagraphs().size() - 2;

        for (int i = previousSize + 1; i <= size; i++) {
            setStyleForLogMessage(type, i);
        }
        scrollPane.setVvalue(1.0);
    }

    private void setStyleForLogMessage(final LogType type, final int i) {
        switch (type) {
            case SUCCESS:
                inlineCssTextArea.setStyle(i, "-fx-fill: GREEN;");
                break;
            case ERROR:
                inlineCssTextArea.setStyle(i, "-fx-fill: RED;");
                break;
            default:
                inlineCssTextArea.setStyle(i, "-fx-fill: BLACK;");
        }
    }

    private String formattedString(final String log) {
        return StringConstants.FORWARD_OPR + StringConstants.SPACE + log + "\n";
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

    public AnchorPane getFileSystem() {
        return fileSystem;
    }

    public AnchorPane getBlackPearl() {
        return blackPearl;
    }

    public TaskProgressView<Ds3JobTask> getJobProgressView() {
        return jobProgressView;
    }
}
