package com.spectralogic.dsbrowser.gui.services;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Workers {

    private final ExecutorService workers;

    public Workers() {
        this(5);
    }

    public Workers(final int num) {
        workers = Executors.newFixedThreadPool(num);
    }

    public void execute(final Runnable run) {
        workers.execute(run);
    }

    public void shutdown() {
        workers.shutdown();
    }
}
