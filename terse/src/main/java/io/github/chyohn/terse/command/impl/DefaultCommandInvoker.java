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

package io.github.chyohn.terse.command.impl;

import io.github.chyohn.terse.cluster.IClusterClient;
import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.exception.CommandExecuteException;
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiver;
import io.github.chyohn.terse.command.ICommandInvoker;
import io.github.chyohn.terse.command.IReceiverRegistry;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.command.result.FailureResult;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.utils.ObjectUtils;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Slf4j
class DefaultCommandInvoker implements ICommandInvoker {

    private static final int TIMEOUT_CORES = 2;//Math.max(4, SysUtils.CPU_COUNT / 2);
    public static final ScheduledExecutorService TIMEOUT_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(
        TIMEOUT_CORES);


    private final IReceiverRegistry receiverRegistry = ISpiFactory.get(IReceiverRegistry.class);
    private final IClusterClient clusterClient = ISpiFactory.get(IClusterClient.class, false);

    @Override
    public void asyncInvoke(List<ICommand> commands,
                            long timeout, RunningWay runningWay,
                            Consumer<IResult<?>> callback) {

        ICommand command = null;
        boolean serial = false;
        try {
            if (ObjectUtils.isEmpty(commands)) {
                return;
            }
            for (int i = 0; i < commands.size(); i++) {
                command = commands.get(i);
                if (command.getId() == 0) {
                    command.setId(i);
                }

                switch (runningWay) {
                    case CLUSTER:
                        asyncRemote(command, timeout, callback);
                        break;
                    case PARALLEL:
                        asyncLocal(command, timeout, callback);
                        break;
                    default:
                        serial = true;
                        serial(command, callback);
                        break;
                }
            }
        } catch (Throwable e) {
            if (serial) {
                throw e;
            }
            log.error("{}execute error：", commands, e);
            doResponse(command, null, e, callback);
        }
    }

    private void asyncRemote(ICommand command, long timeoutMills, Consumer<IResult<?>> callback) {

        if (clusterClient == null || !clusterClient.isInit()) {
            log.warn("cluster mode is closed，use parallel mode");
            asyncLocal(command, timeoutMills, callback);
            return;
        }

        try {
            ResultHandlerCancellableTask task = new ResultHandlerCancellableTask(
                (r, throwable) -> doResponse(command, r, throwable, callback));
            clusterClient.request(command, task);

            String timeoutMsg = "Message of type " + command.getClass()
                + " Ask timed out on remote Node after [" + timeoutMills + "ms]. "
                + " A typical reason for `TimeoutException` is that the recipient actor didn't send a reply.";
            timeoutSchedule(task, timeoutMills, timeoutMsg);
        } catch (Exception throwable) {
            doResponse(command, null, throwable, callback);
        }
    }

    private void asyncLocal(ICommand command, long timeoutMills, Consumer<IResult<?>> callback) {

        try {
            ResultHandlerCancellableTask task = new ResultHandlerCancellableTask(
                (r, throwable) -> doResponse(command, r, throwable, callback));

            IReceiver<ICommand> receiver = receiverRegistry.getAsyncReceiver(command);
            receiver.async(command, r -> task.apply((IResult<?>) r, null));

            String timeoutMsg = "Message of type " + command.getClass()
                + " Ask timed out on Local Executor after [" + timeoutMills + "ms]. "
                + " A typical reason for `TimeoutException` is that the executor didn't send a reply.";
            timeoutSchedule(task, timeoutMills, timeoutMsg);
        } catch (Exception throwable) {
            doResponse(command, null, throwable, callback);
        }
    }

    private void serial(ICommand command, Consumer<IResult<?>> callback) {
        IReceiver<ICommand> receiver = receiverRegistry.getSyncReceiver(command);
        doResponse(command, receiver.sync(command), null, callback);
    }

    private void doResponse(ICommand command, IResult<?> response, Throwable throwable,
        Consumer<IResult<?>> callback) {

        if (callback == null) {
            return;
        }

        IResult<?> result;
        if (response != null) {
            if (response instanceof FailureResult) {
                throwable = new CommandExecuteException("command execute error， " + ((FailureResult) response).getValue());
            }
            result = new ResultWrapper<>(response.getValue(), response.getId(), throwable);
        } else {
            result = new ResultWrapper<>(null, command == null ? -1 : command.getId(), throwable);
        }

        callback.accept(result);
    }

    private void timeoutSchedule(ResultHandlerCancellableTask task, long timeoutMills, String msg) {
        if (timeoutMills <= 0) {
            return;
        }
        ScheduledFuture<?> future = TIMEOUT_EXECUTOR_SERVICE.schedule(() -> task.apply(null, new TimeoutException(msg)),
            timeoutMills, TimeUnit.MILLISECONDS);
        task.setTimeoutFuture(future);
    }

    private static final class ResultWrapper<R> implements IResult<R> {

        R r;
        Throwable throwable;
        int id;

        ResultWrapper(R r, int id, Throwable e) {
            this.r = r;
            this.id = id;
            this.throwable = e;
        }

        @Override
        public int getId() {
            return id;
        }


        @Override
        public R getValue() {
            if (throwable != null) {
                if (throwable instanceof CommandExecuteException) {
                    throw (CommandExecuteException) throwable;
                }
                throw new CommandExecuteException(throwable);
            }
            return r;
        }

    }

    private static final class ResultHandlerCancellableTask implements Callback2<IResult<?>, Throwable> {

        private final Callback2<IResult<?>, Throwable> callable;
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private Future<?> timeoutFuture;

        ResultHandlerCancellableTask(Callback2<IResult<?>, Throwable> callable) {
            this.callable = callable;
        }

        public void apply(IResult<?> response, Throwable throwable) {
            if (!finished.get() && !finished.getAndSet(true)) {
                callable.apply(response, throwable);
                if (timeoutFuture != null) {
                    timeoutFuture.cancel(false);
                    timeoutFuture = null;
                }
            }
        }


        public void setTimeoutFuture(Future<?> timeoutFuture) {
            if (finished.get()) {
                timeoutFuture.cancel(false);
            } else {
                this.timeoutFuture = timeoutFuture;
            }
        }
    }


}
