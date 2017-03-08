package com.spectralogic.dsbrowser.gui;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.injector.providers.*;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.LoggingServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.LogService;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Session;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import javafx.scene.input.DataFormat;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class GuiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LoggingService.class).to(LoggingServiceImpl.class).in(Singleton.class);
        bind(ShutdownService.class).to(ShutdownServiceImpl.class).in(Singleton.class);

        bind(Ds3SessionStore.class).toProvider(Ds3SessionStoreProvider.class).in(Singleton.class);
        bind(SettingsStore.class).toProvider(SettingsProvider.class).in(Singleton.class);
        bind(SavedSessionStore.class).toProvider(SavedSessionProvider.class).in(Singleton.class);
        bind(SavedJobPrioritiesStore.class).toProvider(SavedJobPrioritiesProvider.class).in(Singleton.class);
        bind(JobInterruptionStore.class).toProvider(JobInterruptionStoreProvider.class).in(Singleton.class);
        bind(ResourceBundle.class).toProvider(ResourceBundleProvider.class).in(Singleton.class);

        bind(Workers.class).in(Singleton.class);
        bind(JobWorkers.class).in(Singleton.class);
        bind(LogService.class).in(Singleton.class);
        bind(DeepStorageBrowser.class).in(Singleton.class);

        bind(Ds3Common.class).in(Singleton.class);

        bind(DataFormat.class).toInstance(new DataFormat("Ds3TreeTableView"));

        loadPresenters(this::bind);

        binder().skipSources(Session.class);
    }

    @Provides
    protected LogSettings loadLogSettings(final SettingsStore settings) {
        return settings.getLogSettings();
    }

    @Provides @Named("jobWorkerThreadCount")
    protected int loadJobWorkerThreadCount(final SettingsStore settingsStore) {
        return settingsStore.getProcessSettings().getMaximumNumberOfParallelThreads();
    }

    private void loadPresenters(final Consumer<Class<?>> binder) {
        new FastClasspathScanner("com.spectralogic.dsbrowser.gui.components")
                .matchClassesWithAnnotation(Presenter.class, binder::accept)
                .scan();
    }
}
