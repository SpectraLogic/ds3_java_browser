package com.spectralogic.dsbrowser.gui.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Workers {

    private final ExecutorService workers;

    public Workers() {
        this(10);
    }

    public Workers(final int num) {
        workers = Executors.newFixedThreadPool(num);
    }

    public Future<?> execute(final Runnable run) {
        return workers.submit(run);
    }

    public void shutdown() {
        workers.shutdown();
    }
}