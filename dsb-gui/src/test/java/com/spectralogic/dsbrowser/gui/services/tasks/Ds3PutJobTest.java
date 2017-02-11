package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class Ds3PutJobTest {

    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static Session session;
    private static File file;
    private static Ds3PutJob ds3PutJob;
    private final static String bucketName = "TEST1";
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
            final ClassLoader classLoader = Ds3PutJobTest.class.getClassLoader();
            final URL url = classLoader.getResource("files/SampleFiles.txt");
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
            } catch (final Exception io) {
                io.printStackTrace();
            }
        });
    }

    @Test
    public void executeJob() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                jobWorkers.execute(ds3PutJob);
                ds3PutJob.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();
                });
                ds3PutJob.setOnFailed(event -> {
                    latch.countDown();
                });
                ds3PutJob.setOnCancelled(event -> {
                    latch.countDown();
                });
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void createFileMap() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
                final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
                final ImmutableMap.Builder<String, Path> fileMap = ImmutableMap.builder();
                final List<File> filesList = new ArrayList<>();
                filesList.add(file);
                final ImmutableList<Path> paths = filesList.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> files = paths.stream().filter(path -> !Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                ds3PutJob.createFileMap(files, directories, partOfDirBuilder, expandedPaths, fileMap);
                if (fileMap.build().size() == 1) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void createFolderMap() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final ImmutableMap.Builder<String, Path> folderMap = ImmutableMap.builder();
        Platform.runLater(() -> {
            try {
                final ImmutableSet.Builder<Path> partOfDirBuilder = ImmutableSet.builder();
                final ImmutableMultimap.Builder<Path, Path> expandedPaths = ImmutableMultimap.builder();
                final List<File> filesList = new ArrayList<>();
                filesList.add(new File(file.getParent()));
                final ImmutableList<Path> paths = filesList.stream().map(File::toPath).collect(GuavaCollectors.immutableList());
                final ImmutableList<Path> directories = paths.stream().filter(path -> Files.isDirectory(path)).collect(GuavaCollectors.immutableList());
                ds3PutJob.createFolderMap(directories, expandedPaths, folderMap, partOfDirBuilder);
                if (folderMap.build().size() == 1) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void getDs3ObjectPath() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String fileName = ds3PutJob.getDs3ObjectPath(new File(file.getParent()).toPath(), file.toPath(), false, 8, 0);
                if (fileName.equals(file.getName())) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void getTransferRateString() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String transferRateString = ds3PutJob.getTransferRateString(100, 100, new AtomicLong(20L), 1000, "demo", 0);
                final String newTransferRateString = " Transfer Rate 100 Bytes/s Time remaining 1 minute 10 Bytes/1000 Bytes Transferred file -> demo to TEST1/";
                if (newTransferRateString.equals(transferRateString)) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void getTransferRateStringWithZero() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String transferRateStringWithTransferRateZero = ds3PutJob.getTransferRateString(0, 100, new AtomicLong(20L), 1000, "demo", 0);
                final String newTransferRateStringWithTransferRateZero = " Transfer Rate --/s Time remaining :calculating.. 10 Bytes/1000 Bytes Transferred file -> demo to TEST1/";
                if (newTransferRateStringWithTransferRateZero.equals(transferRateStringWithTransferRateZero)) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void setPutJobTransferrCacheEisabledString() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String dateOfTransfer = DateFormat.formatDate(new Date());
                final String cacheEnabledString = ds3PutJob.setPutJobTransferString(1000L, true, dateOfTransfer);
                final String newCacheEnabledString = "PUT job [Size: 1000 Bytes]  completed. File transferred to storage location bucket " + bucketName + " at location (BlackPearl cache) at " + dateOfTransfer + ". Waiting for job to complete...";
                if (newCacheEnabledString.equals(cacheEnabledString)) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }

    @Test
    public void setPutJobTransferCacheDisabledString() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final String dateOfTransfer = DateFormat.formatDate(new Date());
                final String cacheDisabledString = ds3PutJob.setPutJobTransferString(1000L, false, dateOfTransfer);
                final String newCacheDisabledString = "PUT job [Size: 1000 Bytes] transferred to bucket" + bucketName + " at location (BlackPearl cache)  at " + dateOfTransfer + ".";
                if (newCacheDisabledString.equals(cacheDisabledString)) {
                    successFlag = true;
                }
                latch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        });
        latch.await();
        assertTrue(successFlag);
    }
}