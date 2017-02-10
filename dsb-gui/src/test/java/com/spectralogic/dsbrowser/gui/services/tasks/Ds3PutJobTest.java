package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.Ds3JobTask;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.util.DateFormat;
import com.spectralogic.dsbrowser.gui.util.MyTaskProgressView;
import com.spectralogic.dsbrowser.util.GuavaCollectors;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Ds3PutJobTest {

    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static Session session;
    private static File file;
    private static Ds3PutJob ds3PutJob;
    private final static String bucketName = "TEST1";

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
            final ClassLoader classLoader = Ds3PutJobTest.class.getClassLoader();
            final URL url = classLoader.getResource("files/Sample.txt");
            if (url != null) {
                Ds3PutJobTest.file = new File(url.getFile());
            }
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);

            Mockito.when(deepStorageBrowserPresenter.getCircle()).thenReturn(Mockito.mock(Circle.class));
            Mockito.when(deepStorageBrowserPresenter.getCount()).thenReturn(Mockito.mock(Label.class));
            Mockito.when(deepStorageBrowserPresenter.getJobButton()).thenReturn(Mockito.mock(Button.class));
            final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);

            Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
            final MyTaskProgressView<Ds3JobTask> taskProgressView = new MyTaskProgressView<>();
            Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);

            try {
                final SettingsStore settingsStore = SettingsStore.loadSettingsStore();
                ds3PutJob = new Ds3PutJob(ds3Client, filesList, bucketName, "", deepStorageBrowserPresenter, Priority.URGENT.toString(), 5, JobInterruptionStore.loadJobIds(), ds3Common, settingsStore);
                taskProgressView.getTasks().add(ds3PutJob);
            } catch (final IOException io) {
                io.printStackTrace();
            }
        });
    }

    @Test
    public void executeJob() throws Exception {
        Platform.runLater(() -> {
            jobWorkers.execute(ds3PutJob);
            ds3PutJob.setOnSucceeded(event -> Assert.assertFalse(ds3PutJob.isJobFailed()));
            ds3PutJob.setOnFailed(event -> Assert.assertEquals(true, false));
        });

        Thread.sleep(10000);
    }

    @Test
    public void createFileMap() throws Exception {
        Platform.runLater(() -> {
            final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
            final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
            final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
            final List<File> filesList = new ArrayList<>();
            filesList.add(file);
            final ImmutableList<Path> paths = filesList.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            ds3PutJob.createFileMap(files, directories, partOfDirBuilder, expandedPaths, fileMap);
            Assert.assertEquals(fileMap.build().size(), 1);
        });
        Thread.sleep(2000);

    }

    @Test
    public void createFolderMap() throws Exception {
        Platform.runLater(() -> {
            final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
            final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
            final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
            final List<File> filesList = new ArrayList<>();
            filesList.add(new File(file.getParent()));
            final ImmutableList<Path> paths = filesList.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
            final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
            ds3PutJob.createFolderMap(directories, expandedPaths, folderMap, partOfDirBuilder);
            Assert.assertEquals(folderMap.build().size(), 1);
        });
        Thread.sleep(2000);
    }

    @Test
    public void getDs3ObjectPath() throws Exception {
        Platform.runLater(() -> {
            final String fileName = ds3PutJob.getDs3ObjectPath(new File(file.getParent()).toPath(), file.toPath(), false, 1, 0);
            Assert.assertEquals(fileName, file.getName());
        });
        Thread.sleep(2000);

    }

    @Test
    public void getTransferRateString() throws Exception {
        Platform.runLater(() -> {
            final String transferRateString = ds3PutJob.getTransferRateString(100, 100, new AtomicLong(20L), 1000, "demo", 0);
            Assert.assertEquals(" Transfer Rate 100 Bytes/s Time remaining 1 minute 10 Bytes/1000 Bytes Transferred file -> demo to TEST1/", transferRateString);

            final String transferRateStringWithTransferRateZero = ds3PutJob.getTransferRateString(0, 100, new AtomicLong(20L), 1000, "demo", 0);
            Assert.assertEquals(" Transfer Rate --/s Time remaining :calculating.. 10 Bytes/1000 Bytes Transferred file -> demo to TEST1/", transferRateStringWithTransferRateZero);
        });
        Thread.sleep(5000);
    }

    @Test
    public void setPutJobTransferString() throws Exception {
        Platform.runLater(() -> {
            final String dateOfTransfer = DateFormat.formatDate(new Date());
            final String cacheEnabledString = ds3PutJob.setPutJobTransferString(1000L, true, dateOfTransfer);
            Assert.assertEquals("PUT job [Size: 1000 Bytes]  completed. File transferred to storage location bucket " + bucketName + " at location (BlackPearl cache) at " + dateOfTransfer + ". Waiting for job to complete...", cacheEnabledString);

            final String cacheDisabledString = ds3PutJob.setPutJobTransferString(1000L, false, dateOfTransfer);
            Assert.assertEquals("PUT job [Size: 1000 Bytes] transferred to bucket" + bucketName + " at location (BlackPearl cache)  at " + dateOfTransfer + ".", cacheDisabledString);
        });
        Thread.sleep(5000);
    }
}