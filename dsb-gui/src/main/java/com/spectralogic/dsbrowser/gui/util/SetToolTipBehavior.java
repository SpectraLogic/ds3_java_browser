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

package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class SetToolTipBehavior {
    private final static Logger LOG = LoggerFactory.getLogger(SetToolTipBehavior.class);

    /**
     * This function is to set the tooltip behaviour. You can set initial delay, duration of the tooltip and close delay.
     *
     * @param openDelay  delay in displaying toltip
     * @param duration   tooltip display time
     * @param closeDelay tooltip closing time
     */
    public void setToolTipBehaviors(final int openDelay, final int duration, final int closeDelay) {
        try {
            final Field field = Tooltip.class.getDeclaredField("BEHAVIOR");
            field.setAccessible(true);
            final Class[] classes = Tooltip.class.getDeclaredClasses();
            for (final Class clazz : classes) {
                if (clazz.getName().equals("javafx.scene.control.Tooltip$TooltipBehavior")) {
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
}
