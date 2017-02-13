package com.spectralogic.dsbrowser.gui.util;

import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Created by Suman on 2/8/2017.
 */
public class SetToolTipBehavior {
    private final static Logger LOG = LoggerFactory.getLogger(SetToolTipBehavior.class);

    /**
     * This function is to set the tooltip behaviour. You can set initial delay, duration of the tooltip and close delay.
     *
     * @param openDelay  delay in displaying toltip
     * @param duration   tooltip display time
     * @param closeDelay tooltip closing time
     */
    public void setToolTilBehaviors(final int openDelay, final int duration, final int closeDelay) {
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
