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

package io.github.chyohn.terse.command;

import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.anotations.Internal;
import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.exception.CommandExecuteException;
import io.github.chyohn.terse.command.threadpool.CallableCommand;
import io.github.chyohn.terse.command.threadpool.RunnableCommand;
import io.github.chyohn.terse.spi.SPI;
import io.github.chyohn.terse.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * command invoker, submit the commands to receiver.
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
@SPI(allowMultiInstance = false)
public interface ICommandInvoker {

    long NONE_TIMEOUT = -1;


    void asyncInvoke(List<ICommand> commands, long timeout, RunningWay runningWay, Consumer<IResult<?>> callback);


    default <R> void asyncRun(List<ICommandX<R>> commands, long timeout, RunningWay runningWay, Consumer<IResult<R>> callback) {
        List<ICommand> list = new ArrayList<>(commands);
        asyncInvoke(list, timeout, runningWay, r -> {
            callback.accept((IResult<R>) r);
        });
    }

    /**
     * blocking execute command
     *
     * @param command command
     * @param <R>     result type
     * @param <C>     command type
     * @return result
     */
    default <R, C extends ICommandX<R>> R run(C command) {
        if (command == null) {
            return null;
        }
        AtomicReference<R> reference = new AtomicReference<>();
        run(Collections.singletonList(command), r -> {
            reference.set(r.getValue());
        });
        return reference.get();
    }

    /**
     * async execute command
     *
     * @param command  command
     * @param callback the result handler
     * @param <R>      result type
     * @param <C>      command type
     */
    default <R, C extends ICommandX<R>> void asyncRun(C command, Consumer<R> callback) {
        if (command == null) {
            return;
        }
        asyncRun(Collections.singletonList(command), result -> callback.accept(result.getValue()));
    }

    /**
     * block and batch execute command
     *
     * @param commands command
     * @param callback the result handler
     * @param <R>      result type
     * @param <C>      command type
     */
    default <R, C extends ICommandX<R>> void run(List<C> commands, Consumer<IResult<R>> callback) {

        if (ObjectUtils.isEmpty(commands)) {
            return;
        }

        LinkedBlockingQueue<IResult<R>> resultQueue = new LinkedBlockingQueue<>(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            ICommandX<R> command = commands.get(i);
            command.setId(i);
            RunningWay runningWay = i < commands.size() - 1 ? RunningWay.CLUSTER : RunningWay.SERIAL;
            this.asyncRun(Collections.singletonList(command), NONE_TIMEOUT, runningWay,
                    response -> {
                        try {
                            resultQueue.put(response);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new CommandExecuteException(e);
                        }
                    });
        }

        try {
            int commandSize = commands.size();
            while (commandSize > 0) {
                // 等到响应结果
                IResult<R> result = resultQueue.take();
                commandSize--;
                if (callback != null) {
                    callback.accept(result);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommandExecuteException(e);
        }
    }

    /**
     * async and batch execute command
     *
     * @param commands command
     * @param callback the result handler
     * @param <R>      result type
     * @param <C>      command type
     */
    default <R, C extends ICommandX<R>> void asyncRun(List<C> commands, Consumer<IResult<R>> callback) {

        if (ObjectUtils.isEmpty(commands)) {
            return;
        }

        this.asyncRun((List<ICommandX<R>>) commands, NONE_TIMEOUT, RunningWay.CLUSTER, callback);
    }

    /**
     * async execute task use pool executor with special name
     *
     * @param poolName name of pool executor. see {@link Terse#registerExecutor(String, Executor)}
     * @param runnable task
     */
    default void asyncRun(String poolName, Runnable runnable) {
        List<ICommandX<Boolean>> requests = Collections.singletonList(new RunnableCommand(poolName, runnable));
        this.asyncRun(requests, NONE_TIMEOUT, RunningWay.PARALLEL, null);
    }

    /**
     * async execute task use pool executor with special name
     *
     * @param poolName        name of pool executor. se{@link Terse#registerExecutor(String, Executor)}
     * @param callable        task with return result
     * @param responseHandler result data handler
     * @param <R>             return data type
     */
    default <R> void asyncRun(String poolName, Callable<R> callable, Consumer<R> responseHandler) {
        List<ICommandX<R>> requests = Collections.singletonList(new CallableCommand<R>(poolName, callable));
        this.asyncRun(requests, NONE_TIMEOUT, RunningWay.PARALLEL, result -> {
            if (result != null && result.getValue() != null) {
                responseHandler.accept(result.getValue());
            }
        });
    }
}
