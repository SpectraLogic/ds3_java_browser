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

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.dsbrowser.api.injector.Presenter;
import com.spectralogic.dsbrowser.api.services.BuildInfoService;
import com.spectralogic.dsbrowser.api.services.ShutdownService;
import com.spectralogic.dsbrowser.api.services.logging.LoggingService;
import com.spectralogic.dsbrowser.gui.components.ds3panel.Ds3Common;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.ButtonCell;
import com.spectralogic.dsbrowser.gui.injector.providers.*;
import com.spectralogic.dsbrowser.gui.services.BuildInfoServiceImpl;
import com.spectralogic.dsbrowser.gui.services.JobWorkers;
import com.spectralogic.dsbrowser.gui.services.LoggingServiceImpl;
import com.spectralogic.dsbrowser.gui.services.Workers;
import com.spectralogic.dsbrowser.gui.services.jobinterruption.JobInterruptionStore;
import com.spectralogic.dsbrowser.gui.services.jobprioritystore.SavedJobPrioritiesStore;
import com.spectralogic.dsbrowser.gui.services.logservice.ApplicationLoggerSettings;
import com.spectralogic.dsbrowser.gui.services.savedSessionStore.SavedSessionStore;
import com.spectralogic.dsbrowser.gui.services.sessionStore.Ds3SessionStore;
import com.spectralogic.dsbrowser.gui.services.settings.LogSettings;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3GetJob;
import com.spectralogic.dsbrowser.gui.services.tasks.Ds3PutJob;
import com.spectralogic.dsbrowser.gui.services.tasks.RecoverInterruptedJob;
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
        bind(BuildInfoService.class).to(BuildInfoServiceImpl.class).in(Singleton.class);

        bind(Ds3SessionStore.class).toProvider(Ds3SessionStoreProvider.class).in(Singleton.class);
        bind(SettingsStore.class).toProvider(SettingsProvider.class).in(Singleton.class);
        bind(SavedSessionStore.class).toProvider(SavedSessionProvider.class).in(Singleton.class);
        bind(SavedJobPrioritiesStore.class).toProvider(SavedJobPrioritiesProvider.class).in(Singleton.class);
        bind(JobInterruptionStore.class).toProvider(JobInterruptionStoreProvider.class).in(Singleton.class);
        bind(ResourceBundle.class).toProvider(ResourceBundleProvider.class).in(Singleton.class);

        bind(Workers.class).in(Singleton.class);
        bind(JobWorkers.class).in(Singleton.class);
        bind(ApplicationLoggerSettings.class).in(Singleton.class);
        bind(DeepStorageBrowser.class).in(Singleton.class);

        bind(Ds3Common.class).in(Singleton.class);

        bind(DataFormat.class).toInstance(new DataFormat("Ds3TreeTableView"));

        loadPresenters(this::bind);

        install(new FactoryModuleBuilder().build(Ds3GetJob.Ds3GetJobFactory.class));
        install(new FactoryModuleBuilder().build(Ds3PutJob.Ds3PutJobFactory.class));
        install(new FactoryModuleBuilder().build(RecoverInterruptedJob.RecoverInterruptedJobFactory.class));
        install(new FactoryModuleBuilder().build(ButtonCell.ButtonCellFactory.class));
    }

    @Provides
    protected Ds3Client providesDs3Client(final Ds3Common ds3Common) {
        return ds3Common.getCurrentSession().getClient();
    }

    @Provides
    @Named("jobPriority")
    protected String providesJobPriority(final SavedJobPrioritiesStore savedJobPrioritiesStore, final ResourceBundle resourceBundle) {
       return  (!savedJobPrioritiesStore.getJobSettings().getPutJobPriority().equals(resourceBundle.getString("defaultPolicyText"))) ? savedJobPrioritiesStore.getJobSettings().getPutJobPriority() : null;
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
