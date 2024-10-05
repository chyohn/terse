package io.github.chyohn.terse.cluster.remote.channel.nio;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class WorkerPool {

    private ExecutorService worker;
    private volatile boolean stopped = true;
    private final int numThreads;
    private final String threadPrefixName;

    public WorkerPool(String name, int numThreads) {
        this.threadPrefixName = name;
        this.numThreads = numThreads;
    }

    public void start() {
        if (numThreads > 0) {
            worker = Executors.newFixedThreadPool(numThreads, new DaemonThreadFactory(threadPrefixName));
        }
        stopped = false;
    }

    public void schedule(WorkRequest workRequest) {
        if (stopped) {
            workRequest.cleanup();
            return;
        }

        ScheduledWorkRequest scheduledWorkRequest = new ScheduledWorkRequest(workRequest);
        if (worker == null) {
            // When there is no worker thread pool, do the work directly
            // and wait for its completion
            scheduledWorkRequest.run();
            return;
        }
        try {
            // make sure to map negative ids as well to [0, size-1]
            worker.execute(scheduledWorkRequest);
        } catch (RejectedExecutionException e) {
            log.warn("ExecutorService rejected execution", e);
            workRequest.cleanup();
        }
    }


    public void stop() {
        stopped = true;
        // Signal for graceful shutdown
        worker.shutdown();
    }

    public abstract static class WorkRequest {

        /**
         * Must be implemented. Is called when the work request is run.
         */
        public abstract void doWork() throws Exception;

        /**
         * (Optional) If implemented, is called if the service is stopped or unable to schedule the request.
         */
        public void cleanup() {
        }

    }

    private class ScheduledWorkRequest implements Runnable {

        private final WorkRequest workRequest;

        ScheduledWorkRequest(WorkRequest workRequest) {
            this.workRequest = workRequest;
        }

        @Override
        public void run() {
            try {
                // Check if stopped while request was on queue
                if (stopped) {
                    workRequest.cleanup();
                    return;
                }
                workRequest.doWork();
            } catch (Exception e) {
                log.warn("Unexpected exception", e);
                workRequest.cleanup();
            }
        }

    }

    /**
     * ThreadFactory for the worker thread pool. We don't use the default
     * thread factory because (1) we want to give the worker threads easier
     * to identify names; and (2) we want to make the worker threads daemon
     * threads so they don't block the server from shutting down.
     */
    private static class DaemonThreadFactory implements ThreadFactory {

        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DaemonThreadFactory(String name) {
            this(name, 1);
        }

        DaemonThreadFactory(String name, int firstThreadNum) {
            threadNumber.set(firstThreadNum);
            group = Thread.currentThread().getThreadGroup();
            namePrefix = name + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }

    }

}
