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

package com.spectralogic.dsbrowser.gui.injector;

import com.airhacks.afterburner.injection.PresenterFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.spectralogic.dsbrowser.api.injector.ModelContext;
import com.spectralogic.dsbrowser.gui.GuiModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.function.Function;

public class GuicePresenterInjector implements PresenterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GuicePresenterInjector.class);

    public static final Injector injector;

    static {
        injector = Guice.createInjector(new GuiModule());
    }

    @Override
    public <T> T instantiatePresenter(final Class<T> clazz, final Function<String, Object> injectionContext) {
        try {
            // TODO figure out how to inject the values in injectionContext into the Guice injector

            final T newInstance = injector.getInstance(clazz);
            injectModelContext(newInstance, injectionContext);
            return newInstance;
        } catch (final Throwable t) {
            LOG.error("Failed to load class: " + t.getMessage(), t);
            throw t;
        }
    }

    private <T> void injectModelContext(final T newInstance, final Function<String, Object> injectionContext) {
        Arrays.stream(newInstance.getClass().getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(ModelContext.class))
            .forEach(field -> {
                final Object apply = injectionContext.apply(field.getName());
                if (apply != null) {
                    setField(field, newInstance, apply);
                } else {
                    LOG.warn("Could not find a value to inject into {} for an instance of {}", field.getName(), newInstance.getClass().getSimpleName());
                }
            });
    }

    private <T> void setField(final Field field, final T instance, final Object value) {
        AccessController.doPrivileged((PrivilegedAction<?>) () -> {
            final boolean wasAccessible = field.isAccessible();
            try {
                field.setAccessible(true);
                field.set(instance, value);
                return null; // return nothing...
            } catch (final IllegalArgumentException | IllegalAccessException ex) {
                throw new IllegalStateException("Cannot set field: " + field + " with value " + value, ex);
            } finally {
                field.setAccessible(wasAccessible);
            }
        });
    }
}