package com.spectralogic.dsbrowser.gui;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Ds3JobTask extends Task<Boolean> {

    private final static Logger LOG = LoggerFactory.getLogger(Ds3JobTask.class);

    @Override
    protected final Boolean call() throws Exception {
        LOG.info("Starting DS3 Job");

        try {
            executeJob();
        } catch (final Exception e) {
            LOG.error("Job failed with an exception", e);
            return false;
        }

        LOG.info("Job finished successfully");

        return true;
    }

    public abstract void executeJob() throws Exception;

    public void updateProgressPutJob(){
        updateProgress(0.1,100);
    }


    }
