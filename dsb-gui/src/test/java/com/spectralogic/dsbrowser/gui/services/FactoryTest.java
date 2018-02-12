/*
 *****************************************************************************
 *Copyright 2014-2017Spectra Logic Corporation.All Rights Reserved.
 *Licensed under the Apache License,Version2.0(the"License").You may not use
 *this file except in compliance with the License.A copy of the License is located at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *or in the"license"file accompanying this file.
 *This file is distributed on an"AS IS"BASIS,WITHOUT WARRANTIES OR
 *CONDITIONS OF ANY KIND,either express or implied.See the License for the
 *specific language governing permissions and limitations under the License.
 *****************************************************************************
*/
package com.spectralogic.dsbrowser.gui.services;

import com.google.common.collect.ImmutableList;
import com.google.inject.*;
import com.google.inject.util.Modules;
import com.spectralogic.ds3client.Ds3Client;
import com.spectralogic.ds3client.Ds3ClientImpl;
import com.spectralogic.dsbrowser.gui.DeepStorageBrowserPresenter;
import com.spectralogic.dsbrowser.gui.GuiModule;
import com.spectralogic.dsbrowser.gui.components.ds3panel.ds3treetable.Ds3TreeTableValueCustom;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.ButtonCell;
import com.spectralogic.dsbrowser.gui.components.interruptedjobwindow.EndpointInfo;
import com.spectralogic.dsbrowser.gui.services.settings.SettingsStore;
import com.sun.javafx.application.PlatformImpl;
import javafx.scene.control.TreeItem;
import javafx.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

public class FactoryTest {
    private static Injector injector;
    private static TestModlue testModlue;
    private static GuiModule guiModule;
    private final static EndpointInfo endpointInfo = mock(EndpointInfo.class);

    private static class TestModlue implements Module {
        public void configure(final Binder binder) {
        }

        @Provides
        public BuildInfoServiceImpl getBuildService() {
            return mock(BuildInfoServiceImpl.class);
        }

        @Provides
        public DeepStorageBrowserPresenter getDSP() {
            return mock(DeepStorageBrowserPresenter.class);
        }

        @Provides
        protected Ds3Client providesDs3Client() {
            return mock(Ds3ClientImpl.class);
        }

        @Singleton
        @Provides
        protected SettingsStore providesSettingsStore() {
            return SettingsStore.createDefaultSettingStore();
        }
    }

    /*
        install(new FactoryModuleBuilder().build(ButtonCell.ButtonCellFactory.class));
     */

    @BeforeClass
    public static void prep() {
        testModlue = new TestModlue();
        guiModule = new GuiModule();
        injector = Guice.createInjector(Modules.override(guiModule).with(testModlue));
    }

    @Test
    public void buildButtonCell() {
        PlatformImpl.startup(new Runnable() {
            @Override
            public void run() {

            }
        });
        final ButtonCell.ButtonCellFactory f = injector.getInstance(ButtonCell.ButtonCellFactory.class);
        ButtonCell bc = null;
        try {
            bc = f.createButtonCell(endpointInfo);
        } catch (final ProvisionException pe) {
            fail("Provision Exception was thrown");
        }
        assert(bc != null);
    }

}
