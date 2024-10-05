package io.github.chyohn.terse.cluster.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClusterThread extends Thread {

    private UncaughtExceptionHandler uncaughtExceptionalHandler = (t, e) -> handleException(t.getName(), e);

    public ClusterThread(String threadName) {
        super(threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    /**
     * This will be used by the uncaught exception handler and just log a
     * warning message and return.
     *
     * @param thName thread name
     * @param e exception object
     */
    protected void handleException(String thName, Throwable e) {
        log.warn("Exception occurred from thread {}", thName, e);
    }
}
