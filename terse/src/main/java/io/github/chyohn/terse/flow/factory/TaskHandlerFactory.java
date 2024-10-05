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

package io.github.chyohn.terse.flow.factory;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.flow.ITaskHandler;
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
public final class TaskHandlerFactory {

    /**
     * the task run int a thread of indicate pool
     *
     * @param poolName the thread pool, registry by {@link Terse#registerExecutor(String, Executor)}
     * @param runnable run in thread
     * @return new handler
     */
    public static ITaskHandler newTaskHandler(String poolName, Runnable runnable) {
        return new RunnableRequestHandler(poolName, Collections.singletonList(runnable));
    }

    /**
     * the task run in another threads of indicate pool
     *
     * @param poolName     the thread pool, registry by {@link Terse#registerExecutor(String, Executor)}
     * @param runnableList all run in a thread pool
     * @return new handler
     */
    public static ITaskHandler newTaskHandler(String poolName, List<Runnable> runnableList) {
        return new RunnableRequestHandler(poolName, runnableList);
    }

    /**
     * the task run int a thread of indicate pool
     *
     * @param poolName        the thread pool, registry by {@link Terse#registerExecutor(String, Executor)}
     * @param callable        run in a thread
     * @param responseHandler result handler. is executed in same thread with invoker.
     * @param <R>             result type
     * @return handler
     */
    public static <R> ITaskHandler newTaskHandler(String poolName, Callable<R> callable, Consumer<R> responseHandler) {
        return newTaskHandler(poolName, Collections.singletonList(callable), responseHandler);
    }

    /**
     * the task run in threads of indicate pool
     *
     * @param poolName        the thread pool, registry by {@link Terse#registerExecutor(String, Executor)}
     * @param callables       all run in another threads
     * @param responseHandler result handler. is executed in same thread with invoker.
     * @param <R>             result type
     * @return handler
     */
    public static <R> ITaskHandler newTaskHandler(String poolName, List<Callable<R>> callables, Consumer<R> responseHandler) {
        return new CallableRequestHandler<>(poolName, callables, responseHandler);
    }

    /**
     * create handler with one or more command.
     * <p>note: the commands supplier and result handler are executed in same thread with invoker.</p>
     *
     * @param commandXSupplier commands supplier
     * @param responseHandler  command result handler
     * @param <R>              command result type
     * @return handler
     */
    public static <R> ITaskHandler newTaskHandler(
            Supplier<List<ICommandX<R>>> commandXSupplier, Callback2<IResult<R>, Boolean> responseHandler) {
        return new SimpleTaskHandler<>(commandXSupplier, responseHandler);
    }

    /**
     * create handler with one command
     * <p>note: the command supplier and result handler are executed in same thread with invoker.</p>
     *
     * @param commandXSupplier command supplier
     * @param responseHandler  command result handler
     * @param <R>              command result type
     * @return handler
     */
    public static <R> ITaskHandler newTaskHandler(Supplier<ICommandX<R>> commandXSupplier, Consumer<R> responseHandler) {
        return new SimpleTaskHandler<>(() -> {
            ICommandX<R> commandX = commandXSupplier.get();
            if (commandX == null) return null;
            return Collections.singletonList(commandX);
        }, (response, finished) -> {
            responseHandler.accept(response.getValue());
        });
    }

}
