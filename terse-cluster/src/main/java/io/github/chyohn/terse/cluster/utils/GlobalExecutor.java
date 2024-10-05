package io.github.chyohn.terse.cluster.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GlobalExecutor {
    
    private static final ScheduledExecutorService COMMON_EXECUTOR = Executors.newScheduledThreadPool(4,
        new NameThreadFactory("CLUSTER-COMMON-SCHEDULE"));

    public static void executeByCommon(Runnable runnable) {
        if (COMMON_EXECUTOR.isShutdown()) {
            return;
        }
        COMMON_EXECUTOR.execute(runnable);
    }
    
    public static void scheduleByCommon(Runnable runnable, long delayMs) {
        if (COMMON_EXECUTOR.isShutdown()) {
            return;
        }
        COMMON_EXECUTOR.schedule(runnable, delayMs, TimeUnit.MILLISECONDS);
    }

}

