package com.spectralogic.dsbrowser.gui;

import com.spectralogic.ds3client.utils.Guard;
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
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.util.*;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import org.fxmisc.richtext.InlineCssTextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;

import com.spectralogic.dsbrowser.gui.util.CancelJobsWorker;

public class DeepStorageBrowserPresenter implements Initializable {

    private final static Logger LOG = LoggerFactory.getLogger(DeepStorageBrowserPresenter.class);
    private final ImageView INTERRUPTEDJOBIMAGEVIEW = new ImageView(ImageURLs.INTERRUPTED_JOB_IMAGE);
    private final ImageView CANCELALLJOBIMAGEVIEW = new ImageView(ImageURLs.CANCEL_ALL_JOB_IMAGE);

    private final Label lblCount = new Label();
    private final Button jobButton = new Button();
    private final Circle circle = new Circle();

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
    private MenuItem aboutMenuItem, closeMenuItem, sessionsMenuItem, settingsMenuItem, selectAllInFolderItem, selectAllInBucketItem;

    @FXML
    private Menu fileMenu, helpMenu, viewMenu, editMenu;

    @FXML
    private BorderPane borderPane;

    @Inject
    private JobWorkers jobWorkers;

    @Inject
    private ResourceBundle resourceBundle;

    @Inject
    private SavedJobPrioritiesStore savedJobPrioritiesStore;

    @Inject
    private Ds3Common ds3Common;

    @Inject
    private JobInterruptionStore jobInterruptionStore;

    @Inject
    private SettingsStore settingsStore;

    @Inject
    private SavedSessionStore savedSessionStore;

    @Inject
    private Workers workers;

    private DeepStorageBrowserTaskProgressView<Ds3JobTask> jobProgressView;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        try {
            initGUIElement(); //Setting up labels from resource file

            LOG.info("Loading Main view");
            logText(resourceBundle.getString("loadMainView"), LogType.INFO);

            final SetToolTipBehavior setToolTipBehavior = new SetToolTipBehavior();
            setToolTipBehavior.setToolTilBehaviors(Constants.OPEN_DELAY, Constants.DURATION, Constants.CLOSE_DELAY); //To set the time interval of tooltip

            final Tooltip jobToolTip = new Tooltip();
            jobToolTip.setText(resourceBundle.getString("interruptedJobs"));
            final Tooltip cancelAllToolTip = new Tooltip();
            cancelAllToolTip.setText(resourceBundle.getString("cancelAllRunningJobs"));

            jobProgressView = new DeepStorageBrowserTaskProgressView<>();
            jobProgressView.setPrefHeight(1000);

            final VBox jobProgressVBox = new VBox();
            //Implementation and design of Cancel button in bottom pane
            CANCELALLJOBIMAGEVIEW.setFitHeight(15);
            CANCELALLJOBIMAGEVIEW.setFitWidth(15);
            final Button cancelAll = new Button();
            cancelAll.setTranslateX(70);
            cancelAll.setTranslateY(4);
            cancelAll.setTooltip(cancelAllToolTip);
            cancelAll.setGraphic(CANCELALLJOBIMAGEVIEW);
            cancelAll.disableProperty().bind(Bindings.size(jobProgressView.getTasks()).lessThan(1));
            cancelAll.setOnAction(event -> {
                CancelJobsWorker.cancelAllRunningJobs(jobWorkers, jobInterruptionStore, LOG, workers, ds3Common);
            });

            INTERRUPTEDJOBIMAGEVIEW.setFitHeight(15);
            INTERRUPTEDJOBIMAGEVIEW.setFitWidth(15);
            jobButton.setTranslateX(20);
            jobButton.setTranslateY(4);
            jobButton.setTooltip(jobToolTip);
            jobButton.setGraphic(INTERRUPTEDJOBIMAGEVIEW);
            jobButton.setDisable(true);
            jobButton.setOnAction(event -> {
                if (ds3Common.getCurrentSession() != null) {
                    final Session session = ds3Common.getCurrentSession();
                    final String endpoint = session.getEndpoint() + StringConstants.COLON + session.getPortNo();
                    final ArrayList<Map<String, Map<String, FilesAndFolderMap>>> endpoints = jobInterruptionStore.getJobIdsModel().getEndpoints();

                    final Map<String, FilesAndFolderMap> jobIDMap = ParseJobInterruptionMap.getJobIDMap(endpoints, endpoint, jobProgressView, null);

                    if (!Guard.isMapNullOrEmpty(jobIDMap)) {
                        jobButton.setDisable(false);
                        final JobInfoView jobView = new JobInfoView(new EndpointInfo(endpoint, session.getClient(), jobIDMap, DeepStorageBrowserPresenter.this, ds3Common));
                        Popup.show(jobView.getView(), resourceBundle.getString("interruptedJobsPopUp") + endpoint);
                    } else {
                        jobButton.setDisable(true);
                    }

                } else {
                    jobButton.setDisable(true);
                    logText(resourceBundle.getString("noInterruptedJobs"), LogType.INFO);
                }
            });

            final StackPane stackpane = new StackPane();
            stackpane.setLayoutX(45);
            stackpane.setLayoutY(1);
            lblCount.setTextFill(Color.WHITE);
            circle.radiusProperty().setValue(8.0);
            circle.setStrokeType(StrokeType.INSIDE);
            circle.setVisible(false);
            circle.setFill(Color.RED);
            stackpane.getChildren().add(circle);
            stackpane.getChildren().add(lblCount);

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
                logsORJobsMenuItemAction(jobsMenuItem, jobProgressVBox, null, jobsTab);
            });

            logsMenuItem.setOnAction(event -> {
                logsORJobsMenuItemAction(logsMenuItem, null, scrollPane, logsTab);
            });

            closeMenuItem.setOnAction(event -> {
                final CloseConfirmationHandler closeConfirmationHandler = new CloseConfirmationHandler(PrimaryStageModel.getInstance().getPrimaryStage(), savedSessionStore, savedJobPrioritiesStore, jobInterruptionStore, settingsStore, jobWorkers, workers);
                closeConfirmationHandler.closeConfirmationAlert(event);
            });

            final LocalFileTreeTableView localTreeView = new LocalFileTreeTableView(this);
            final Ds3PanelView ds3PanelView = new Ds3PanelView(this);
            localTreeView.getViewAsync(fileSystem.getChildren()::add);
            ds3PanelView.getViewAsync(blackPearl.getChildren()::add);

        } catch (final Exception e) {
            LOG.error("Encountered an error when creating Main view", e);
            logText(resourceBundle.getString("errorWhileCreatingMainView"), LogType.ERROR);
        }
    }

    private void initGUIElement() {
        fileMenu.setText(resourceBundle.getString("fileMenu"));
        sessionsMenuItem.setText(resourceBundle.getString("sessionsMenuItem"));
        settingsMenuItem.setText(resourceBundle.getString("settingsMenuItem"));
        closeMenuItem.setText(resourceBundle.getString("closeMenuItem"));
        viewMenu.setText(resourceBundle.getString("viewMenu"));
        editMenu.setText(resourceBundle.getString("editMenu"));
        selectAllInBucketItem.setText(resourceBundle.getString("selectAllInBucketItem"));
        selectAllInFolderItem.setText(resourceBundle.getString("selectAllInFolderItem"));
        jobsMenuItem.setText(resourceBundle.getString("jobsMenuItem"));
        logsMenuItem.setText(resourceBundle.getString("logsMenuItem"));
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
        NewSessionPopup.show();
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
            if (!jobSplitter.getItems().stream().anyMatch(i -> i instanceof TabPane)) {
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

    public void logText(final String log, final LogType type) {
        if (inlineCssTextArea != null) {
            final int previousSize = inlineCssTextArea.getParagraphs().size() - 2;
            inlineCssTextArea.appendText(formattedString(log));
            final int size = inlineCssTextArea.getParagraphs().size() - 2;
            for (int i = previousSize + 1; i <= size; i++)
                switch (type) {
                    case SUCCESS:
                        inlineCssTextArea.setStyle(size,"-fx-fill: GREEN;");
                        break;
                    case ERROR:
                        inlineCssTextArea.setStyle(size, "-fx-fill: RED;");
                        break;
                    default:
                        inlineCssTextArea.setStyle(size, "-fx-fill: BLACK;");
                }
            scrollPane.setVvalue(1.0);
        }
    }

    private String formattedString(final String log) {
        return ">> " + log + "\n";
    }

    public Label getLblCount() {
        return lblCount;
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

    public DeepStorageBrowserTaskProgressView<Ds3JobTask> getJobProgressView() {
        return jobProgressView;
    }
}
