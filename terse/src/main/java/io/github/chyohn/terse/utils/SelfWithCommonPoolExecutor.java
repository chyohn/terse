/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.chyohn.terse.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.*;

/**
 * 任务私有和公共线程池执行对象。当公共线程池耗尽，则使用私有线程池，这样保证任务一直都有资源来执行。
 * <p>
 * 如果任务之间有依赖，则不能使用同一个公共线程池，否则会发生线程池死锁。
 *
 * @author qiang.shao
 * @since 1.0.0
 */
public class SelfWithCommonPoolExecutor extends AbstractExecutorService {

    // 私有线程执行器
    private final Executor selfExecutor;
    // 公共线程池
    private final ThreadPoolExecutor commonPool;

    private boolean shutdown;

    /**
     * @param selfExecutor 私有线程执行器
     * @param commonPool   公共线程池
     */
    public SelfWithCommonPoolExecutor(Executor selfExecutor, ThreadPoolExecutor commonPool) {
        this.selfExecutor = selfExecutor;
        this.commonPool = commonPool;
        // 设置common线程池的拒绝策略
        resetCommonPoolRejectHandler();
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown()) {
            throw new IllegalStateException("executor is shutdown");
        }
        // 先使用公共线程池执行
        commonPool.execute(new SelfRunnable(selfExecutor, command));
    }

    private void resetCommonPoolRejectHandler() {
        RejectedExecutionHandler handler = commonPool.getRejectedExecutionHandler();
        if (handler instanceof CommonPoolRejectedExecutionHandler) {
            return;
        }

        this.commonPool.setRejectedExecutionHandler(new CommonPoolRejectedExecutionHandler(handler));
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return null;
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    @Override
    public boolean isTerminated() {
        return isShutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        shutdown();
        return isShutdown();
    }

    private static class CommonPoolRejectedExecutionHandler implements RejectedExecutionHandler {

        final RejectedExecutionHandler originalHandler;

        private CommonPoolRejectedExecutionHandler(RejectedExecutionHandler originalHandler) {
            this.originalHandler = originalHandler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            Executor selfExecutor = getSelfExecutor(r);
            if (executor != null && !executor.equals(selfExecutor)) {
                // 使用私有执行器执行
                selfExecutor.execute(r);
                return;
            }
            if (originalHandler != null) {
                originalHandler.rejectedExecution(r, executor);
            }
        }

        private Executor getSelfExecutor(Runnable runnable) {

            if (runnable == null) {
                return null;
            }

            if (runnable instanceof SelfRunnable) {
                return ((SelfRunnable) runnable).getSelfExecutor();
            }

            Field[] fields = runnable.getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!Runnable.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Runnable target = null;
                try {
                    field.setAccessible(true);
                    target = (Runnable) field.get(runnable);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (target != null) {
                    return getSelfExecutor(target);
                }
            }
            return null;
        }
    }

    private static class SelfRunnable implements Runnable {

        private final Executor selfExecutor;
        private final Runnable target;

        SelfRunnable(Executor selfExecutor, Runnable target) {
            this.selfExecutor = selfExecutor;
            this.target = target;
        }

        @Override
        public void run() {
            target.run();
        }

        Executor getSelfExecutor() {
            return selfExecutor;
        }
    }
}
