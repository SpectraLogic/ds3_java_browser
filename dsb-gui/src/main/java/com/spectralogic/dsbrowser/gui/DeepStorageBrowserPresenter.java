/*
 * ****************************************************************************
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
 *  ****************************************************************************
 */

package com.spectralogic.dsbrowser.gui;

import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.about.AboutView;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3PanelView;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.JobInfoView;
import com.spectralogic.dsbrowser.gui.components.localfiletreetable.LocalFileTreeTableView;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPopup;
import com.spectralogic.dsbrowser.gui.components.settings.SettingsView;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.FilesAndFolderMap;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.ImageURLs;
import com.spectralogic.dsbrowser.api.services.logging.LogType;
import com.spectralogic.dsbrowser.gui.util.ParseJobInterruptionMap;
import com.spectralogic.dsbrowser.gui.util.Popup;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.TaskProgressView;
import org.fxmisc.richtext.InlineCssTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

@Singleton
@Presenter
public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);
    private final ImageView INTERRUPTEDJOBIMAGEVIEW = new ImageView(ImageURLs.INTERRUPTEDJOBIMAGE);
    private final ImageView CANCELALLJOBIMAGEVIEW = new ImageView(ImageURLs.CANCELALLJOBIMAGE);
    private final Alert CLOSECONFIRMATIONALERT = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Are you sure you want to exit?"
    );

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
    private MenuItem aboutMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem, fileProperties;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu;

    @FXML
    private BorderPane borderPane;

    private final JobWorkers jobWorkers;
    private final ResourceBundle resourceBundle;
    private final Ds3Common ds3Common;
    private final JobInterruptionStore jobInterruptionStore;
    private final Workers workers;
    private final ShutdownService shutdownService;
    private final LoggingService loggingService;

    private TaskProgressView<Ds3JobTask> jobProgressView;

    private final Label count = new Label();

    private final Button jobButton = new Button();

    private final Circle circle = new Circle();

    @Inject
    public DeepStorageBrowserPresenter(final JobWorkers jobWorkers, final ResourceBundle resourceBundle, final Ds3Common ds3Common, final JobInterruptionStore jobInterruptionStore, final Workers workers, final ShutdownService shutdownService, final LoggingService loggingService) {
        this.jobWorkers = jobWorkers;
        this.resourceBundle = resourceBundle;
        this.ds3Common = ds3Common;
        this.jobInterruptionStore = jobInterruptionStore;
        this.workers = workers;
        this.shutdownService = shutdownService;
        this.loggingService = loggingService;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {

        try {

            loggingService.registerLogListener(this::logTextForParagraph);

            setToolTipBehavior(200, 5000, 0); //To set the time interval of tooltip
            initGUIElement(); //Setting up labels from resource file

            LOG.info("Loading Main view");
            logText("Loading main view", LogType.INFO);

            final Stage stage = (Stage) CLOSECONFIRMATIONALERT.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(ImageURLs.DEEPSTORAGEBROWSER));

            jobProgressView = new TaskProgressView<>();
            jobProgressView.setPrefHeight(1000);

            final VBox jobProgressVBox = new VBox();

            INTERRUPTEDJOBIMAGEVIEW.setFitHeight(15);
            INTERRUPTEDJOBIMAGEVIEW.setFitWidth(15);

            CANCELALLJOBIMAGEVIEW.setFitHeight(15);
            CANCELALLJOBIMAGEVIEW.setFitWidth(15);

            final Tooltip jobToolTip = new Tooltip();
            jobToolTip.setText("Interrupted Jobs");

            final Tooltip cancelAllToolTip = new Tooltip();
            cancelAllToolTip.setText("Cancel all running jobs");

            final Button cancelAll = new Button();
            cancelAll.setTranslateX(70);
            cancelAll.setTranslateY(4);
            cancelAll.setTooltip(cancelAllToolTip);
            cancelAll.setGraphic(CANCELALLJOBIMAGEVIEW);
            cancelAll.disableProperty().bind(Bindings.size(jobProgressView.getTasks()).lessThan(1));
            cancelAll.setOnAction(event -> {
                ParseJobInterruptionMap.cancelAllRunningJobs(jobWorkers, jobInterruptionStore, LOG, workers, ds3Common, loggingService);
            });

            jobButton.setTranslateX(20);
            jobButton.setTranslateY(4);
            jobButton.setTooltip(jobToolTip);
            jobButton.setGraphic(INTERRUPTEDJOBIMAGEVIEW);
            jobButton.setDisable(true);
            jobButton.setOnAction(event -> {
                if (ds3Common.getCurrentSessions().stream().findFirst().isPresent()) {
                    final Session session = ds3Common.getCurrentSessions().stream().findFirst().get();
                    final String endpoint = session.getEndpoint() + ":" + session.getPortNo();
                    final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();
                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, jobProgressView, null);

                    if (jobIDMap != null && jobIDMap.size() > 0) {
                        jobButton.setDisable(false);
                        final JobInfoView jobView = new JobInfoView(new EndpointInfo(endpoint, session.getClient(), jobIDMap, DeepStorageBrowserPresenter.this, ds3Common));
                        Popup.show(jobView.getView(), "Interrupted Jobs of " + endpoint);
                    } else {
                        jobButton.setDisable(true);
                    }

                } else {
                    jobButton.setDisable(true);
                    logText("No interrupted jobs", LogType.INFO);
                }
            });

            final StackPane stackpane = new StackPane();
            stackpane.setLayoutX(45);
            stackpane.setLayoutY(1);

            count.setTextFill(Color.WHITE);

            circle.radiusProperty().setValue(8.0);
            circle.setStrokeType(StrokeType.INSIDE);

            circle.setVisible(false);
            circle.setFill(Color.RED);
            stackpane.getChildren().add(circle);
            stackpane.getChildren().add(count);

            final AnchorPane anchorPane = new AnchorPane();
            anchorPane.getChildren().addAll(jobButton, stackpane, cancelAll);
            anchorPane.setMinHeight(35);

            Bindings.bindContentBidirectional(jobWorkers.getTasks(), jobProgressView.getTasks());
            jobsMenuItem.selectedProperty().setValue(true);
            logsMenuItem.selectedProperty().setValue(true);

            jobProgressVBox.getChildren().add(anchorPane);
            jobProgressVBox.getChildren().add(jobProgressView);

            jobsTab.setContent(jobProgressVBox);
            logsTab.setContent(scrollPane);
            jobSplitter.getItems().add(bottomTabPane);
            jobSplitter.setDividerPositions(0.95);

            jobsMenuItem.setOnAction(event -> {
                if (jobsMenuItem.isSelected()) {
                    jobsTab.setContent(jobProgressVBox);
                    bottomTabPane.getTabs().add(0, jobsTab);
                    bottomTabPane.getSelectionModel().select(jobsTab);
                    if (jobSplitter.getItems().stream().noneMatch(i -> i instanceof TabPane)) {
                        jobSplitter.getItems().add(bottomTabPane);
                        jobSplitter.setDividerPositions(0.75);
                    }
                } else {
                    if (!logsMenuItem.isSelected())
                        jobSplitter.getItems().remove(bottomTabPane);
                    bottomTabPane.getTabs().remove(jobsTab);
                }
            });

            logsMenuItem.setOnAction(event -> {
                if (logsMenuItem.isSelected()) {
                    logsTab.setContent(scrollPane);
                    bottomTabPane.getTabs().add(logsTab);
                    bottomTabPane.getSelectionModel().select(logsTab);
                    if (jobSplitter.getItems().stream().noneMatch(i -> i instanceof TabPane)) {
                        jobSplitter.getItems().add(bottomTabPane);
                        jobSplitter.setDividerPositions(0.75);
                    }
                } else {
                    if (!jobsMenuItem.isSelected())
                        jobSplitter.getItems().remove(bottomTabPane);
                    bottomTabPane.getTabs().remove(logsTab);
                }
            });

            closeMenuItem.setOnAction(event -> {
                if (jobWorkers.getTasks().isEmpty()) {
                    event.consume();
                    shutdownService.shutdown();

                } else {

                    final Button exitButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                            ButtonType.OK
                    );
                    final Button cancelButton = (Button) CLOSECONFIRMATIONALERT.getDialogPane().lookupButton(
                            ButtonType.CANCEL
                    );
                    exitButton.setText("Exit");
                    cancelButton.setText("Cancel");

                    if (jobWorkers.getTasks().size() == 1) {
                        CLOSECONFIRMATIONALERT.setHeaderText(jobWorkers.getTasks().size() + " job is still running.");
                    } else {
                        CLOSECONFIRMATIONALERT.setHeaderText(jobWorkers.getTasks().size() + " jobs are still running.");
                    }

                    final Optional<ButtonType> closeResponse = CLOSECONFIRMATIONALERT.showAndWait();

                    if (closeResponse.get().equals(ButtonType.OK)) {
                        event.consume();
                        shutdownService.shutdown();
                    }
                    if (closeResponse.get().equals(ButtonType.CANCEL)) {
                        event.consume();
                    }
                }

            });

            final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView();
            final Ds3PanelView ds3PanelView = new Ds3PanelView();
            localTreeView.getViewAsync(fileSystem.getChildren()::add);
            ds3PanelView.getViewAsync(blackPearl.getChildren()::add);

        } catch (final Throwable e) {
            LOG.error("Encountered an error when creating Main view", e);
            logText("Encountered an error when creating Main view", LogType.ERROR);
        }
    }


    private void initGUIElement() {
        fileMenu.setText(resourceBundle.getString("fileMenu"));
        sessionsMenuItem.setText(resourceBundle.getString("sessionsMenuItem"));
        settingsMenuItem.setText(resourceBundle.getString("settingsMenuItem"));
        closeMenuItem.setText(resourceBundle.getString("closeMenuItem"));
        viewMenu.setText(resourceBundle.getString("viewMenu"));
        jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
        logsMenuItem.setText(resourceBundle.getString("logsMenuItem"));
        helpMenu.setText(resourceBundle.getString("helpMenu"));
        aboutMenuItem.setText(resourceBundle.getString("aboutMenuItem"));
    }

    public Label getCount() {
        return count;
    }

    public Button getJobButton() {
        return jobButton;
    }

    public Circle getCircle() {
        return circle;
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

    /**
     * This function is to set the tooltip behaviour. You can set initial delay, duration of the tooltip and close delay.
     *
     * @param openDelay  delay in displaying toltip
     * @param duration   tooltip display time
     * @param closeDelay tooltip closing time
     */
    private void setToolTipBehavior(final int openDelay, final int duration, final int closeDelay) {
        try {
            final Field field = Tooltip.class.getDeclaredField("BEHAVIOR");
            field.setAccessible(true);

            final Class[] classes = Tooltip.class.getDeclaredClasses();
            for (final Class clazz : classes) {
                if (clazz.getName().equals("javafx.scene.control.Tooltip$TooltipBehavior")) {
                    @SuppressWarnings("unchecked")
                    final Constructor constructor = clazz.getDeclaredConstructor(Duration.class, Duration.class, Duration.class, boolean.class);
                    constructor.setAccessible(true);
                    final Object tooltipBehavior = constructor.newInstance(new Duration(openDelay), new Duration(duration), new Duration(closeDelay), false);
                    field.set(null, tooltipBehavior);
                    break;
                }
            }
        } catch (final Exception e) {
            LOG.error("Unable to set tooltip behaviour", e);
        }
    }

    public void showSettingsPopup() {
        final SettingsView settingsView = new SettingsView();
        Popup.show(settingsView.getView(), "Settings");
    }

    public void showAboutPopup() {
        final AboutView aboutView = new AboutView();
        Popup.show(aboutView.getView(), "About");
    }

    public void showSessionPopup() {
        NewSessionPopup.show();
    }

    public void logText(final String log, final LogType type) {
        if (inlineCssTextArea != null) {
            inlineCssTextArea.appendText(formattedString(log));
            final int size = inlineCssTextArea.getParagraphs().size() - 2;

            setStyleForLogMessage(type, size);
            scrollPane.setVvalue(1.0);
        }
    }

    //set the same color for all the lines of string log separated by \n
    public void logTextForParagraph(final String log, final LogType type) {
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
        return ">> " + log + "\n";
    }
}
