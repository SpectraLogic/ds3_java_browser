package com.spectralogic.dsbrowser.gui.services.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.models.Priority;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValue;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.components.newsession.NewSessionPresenter;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedCredentials;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSession;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.util.DeepStorageBrowserTaskProgressView;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

public class Ds3GetJob_Test {

    private static final JobWorkers jobWorkers = new JobWorkers(10);
    private static Session session;
    private static Ds3GetJob ds3GetJob;
    private boolean successFlag = false;

    @BeforeClass
    public static void setConnection() {
        new JFXPanel();
        Platform.runLater(() -> {
            final SavedSession savedSession = new SavedSession("Test1", "192.168.6.164", "8080", null, new SavedCredentials("c3VsYWJoamFpbg==", "yVBAvWTG"), false);
            session = new NewSessionPresenter().createConnection(savedSession);
            final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom("TEST1", "SampleFiles.txt", Ds3TreeTableValue.Type.File, 3718, "2/07/2017 10:28:17", "spectra", false);
            final ArrayList<Ds3TreeTableValueCustom> listTreeTable = new ArrayList();
            listTreeTable.add(ds3TreeTableValueCustom);
            final ClassLoader classLoader = Ds3GetJob_Test.class.getClassLoader();
            final URL url = classLoader.getResource("files/");
            Path path = null;
            if (url != null) {
                path = new File(url.getFile()).toPath();
            }
            final Ds3Client ds3Client = session.getClient();
            final DeepStorageBrowserPresenter deepStorageBrowserPresenter = Mockito.mock(DeepStorageBrowserPresenter.class);
            final Ds3Common ds3Common = Mockito.mock(Ds3Common.class);
            Mockito.when(ds3Common.getCurrentSession()).thenReturn(session);
            final DeepStorageBrowserTaskProgressView<Ds3JobTask> taskProgressView = new DeepStorageBrowserTaskProgressView<>();
            Mockito.when(deepStorageBrowserPresenter.getJobProgressView()).thenReturn(taskProgressView);
            try {
                ds3GetJob = new Ds3GetJob(listTreeTable, path, ds3Client, deepStorageBrowserPresenter, Priority.URGENT.toString(), 5, JobInterruptionStore.loadJobIds(), ds3Common);
                taskProgressView.getTasks().add(ds3GetJob);
            } catch (final IOException io) {
                io.printStackTrace();
            }
        });
    }

    @Test
    public void executeJob() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                jobWorkers.execute(ds3GetJob);
                ds3GetJob.setOnSucceeded(event -> {
                    successFlag = true;
                    latch.countDown();

                });
                ds3GetJob.setOnFailed(event -> {
                    latch.countDown();
                });
                ds3GetJob.setOnCancelled(event -> {
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
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom("TEST1", "SampleFiles.txt", Ds3TreeTableValue.Type.File, 3718, "2/07/2017 10:28:17", "spectra", false);
                final ImmutableList listItems = ImmutableList.builder().add(ds3TreeTableValueCustom).build();
                final ImmutableMap fileMap = ds3GetJob.createFileMap(listItems).build();
                if (fileMap.size() == 1) {
                    successFlag = true;
                }
                countDownLatch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

    @Test
    public void createFolderMap() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom("TEST1", "files/", Ds3TreeTableValue.Type.Directory, 3718, "2/07/2017 10:28:17", "spectra", false);
                final ImmutableList listItems = ImmutableList.builder().add(ds3TreeTableValueCustom).build();
                final ImmutableMap fileMap = ds3GetJob.createFolderMap(listItems).build();
                if (fileMap.size() == 1) {
                    successFlag = true;
                }
                countDownLatch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

    @Test
    public void getDs3Object() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValueCustom ds3TreeTableValueCustom = new Ds3TreeTableValueCustom("TEST1", "files/", Ds3TreeTableValue.Type.Directory, 3718, "2/07/2017 10:28:17", "spectra", false);
                final ImmutableList listItems = ImmutableList.builder().add(ds3TreeTableValueCustom).build();
                final ImmutableList ds3ObjectList = ds3GetJob.getDS3Object(listItems);
                if (ds3ObjectList.size() == 1) {
                    successFlag = true;
                }
                countDownLatch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

    @Test
    public void addAllDescendants() throws Exception {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                final Ds3TreeTableValueCustom ds3TreeTableValueCustomFolder = new Ds3TreeTableValueCustom("TEST1", "files/", Ds3TreeTableValue.Type.Directory, 3718, "2/07/2017 10:28:17", "spectra", false);
                final Ds3TreeTableValueCustom ds3TreeTableValueCustomFile = new Ds3TreeTableValueCustom("TEST1", "Sample.txt", Ds3TreeTableValue.Type.File, 3718, "2/07/2017 10:28:17", "spectra", false);
                final ArrayList<Ds3TreeTableValueCustom> itemList = new ArrayList();
                itemList.add(ds3TreeTableValueCustomFolder);
                itemList.add(ds3TreeTableValueCustomFile);
                final Map<Path, Path> childMap = ds3GetJob.addAllDescendants(ds3TreeTableValueCustomFolder, itemList, null);
                if (childMap.size() == 1) {
                    successFlag = true;
                }
                countDownLatch.countDown();
            } catch (final Exception e) {
                e.printStackTrace();
                countDownLatch.countDown();
            }
        });
        countDownLatch.await();
        assertTrue(successFlag);
    }

}
