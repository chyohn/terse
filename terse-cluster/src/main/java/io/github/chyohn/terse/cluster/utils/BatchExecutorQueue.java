package io.github.chyohn.terse.cluster.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BatchExecutorQueue<T> {

    private static final int DEFAULT_QUEUE_SIZE = 128;
    private final Queue<T> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private final int chunkSize;

    public BatchExecutorQueue() {
        this(DEFAULT_QUEUE_SIZE);
    }

    public BatchExecutorQueue(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    protected abstract void prepare(T item);

    protected abstract void flush();

    public void enqueue(T message, Executor executor) {
        queue.add(message);
        scheduleFlush(executor);
    }

    private void scheduleFlush(Executor executor) {
        if (scheduled.compareAndSet(false, true)) {
            executor.execute(() -> this.run(executor));
        }
    }

    private void run(Executor executor) {
        try {
            Queue<T> snapshot = new LinkedList<>();
            T item;
            while ((item = queue.poll()) != null) {
                snapshot.add(item);
            }
            int i = 0;
            while ((item = snapshot.poll()) != null) {
                prepare(item);
                i++;
                if (i == chunkSize || snapshot.isEmpty()) {
                    i = 0;
                    flush();
                }
            }
        } finally {
            scheduled.set(false);
            if (!queue.isEmpty()) {
                scheduleFlush(executor);
            }
        }
    }

}
