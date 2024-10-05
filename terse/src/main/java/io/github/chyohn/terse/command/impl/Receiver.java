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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import io.github.chyohn.terse.exception.CommandExecuteException;
import io.github.chyohn.terse.command.result.ResultUtils;
import io.github.chyohn.terse.command.AsyncMethod;
import io.github.chyohn.terse.command.BlockingMethod;
import io.github.chyohn.terse.command.ExecutorFactory;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiver;
import io.github.chyohn.terse.command.IResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import io.github.chyohn.terse.utils.ObjectUtils;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Slf4j
class Receiver<C extends ICommand> implements IReceiver<C> {

    private final Map<Class<? extends C>, BlockingMethod<? extends C>> blockingMethodMap;
    private final Map<Class<? extends C>, AsyncMethod<? extends C>> asyncMethodMap;
    private final Executor defaultExecutor;
    private final Map<Class<? extends C>, ExecutorFactory<? extends C>> requestExecutors;

    Receiver(Map<Class<? extends C>, BlockingMethod<? extends C>> blockingMethodMap
        , Map<Class<? extends C>, AsyncMethod<? extends C>> asyncMethodMap
        , Executor defaultExecutor
        , Map<Class<? extends C>, ExecutorFactory<? extends C>> requestExecutors) {

        if (ObjectUtils.isEmpty(blockingMethodMap) && ObjectUtils.isEmpty(asyncMethodMap)) {
            throw new IllegalArgumentException("no command handler defined for this receiver");
        }

        this.blockingMethodMap = blockingMethodMap;
        this.asyncMethodMap = asyncMethodMap;
        this.defaultExecutor = defaultExecutor;
        this.requestExecutors = requestExecutors;
    }

    @Override
    public <T extends C> boolean canSync(T command) {
        return blockingMethodMap.containsKey(command.getClass());
    }

    @Override
    public <T extends C> boolean canAsync(T command) {
        return asyncMethodMap.containsKey(command.getClass());
    }

    @Override
    public <T extends C> IResult<?> sync(T command) {
        BlockingMethod<T> method = getBlockingMethod(command);
        if (method != null) {
            return ResultUtils.returnData(command, method.invoke(command));
        }

        AsyncMethod<T> asyncMethod = getAsyncMethod(command);
        if (asyncMethod == null) {
            throw new CommandExecuteException("没有找到" + command.getClass() + "的处理方法");
        }
        return receiveWithAsyncMethod(command, asyncMethod);
    }

    @Override
    public <T extends C> void async(T command, Consumer<IResult<?>> callback) {
        AsyncMethod<T> method = getAsyncMethod(command);
        if (method != null) {
            method.invoke(command, data -> {
                callback.accept(ResultUtils.returnData(command, data));
            });
            return;
        }

        BlockingMethod<T> blockingMethod = getBlockingMethod(command);
        if (blockingMethod == null) {
            throw new CommandExecuteException("没有找到" + command.getClass() + "的处理方法");
        }
        asyncWithBlockMethod(command, blockingMethod, callback);

    }

    /**
     * run asynchronous method by blocking
     */
    private <T extends C> IResult<?> receiveWithAsyncMethod(T command, AsyncMethod<T> asyncMethod) {
        // 执行处理方法
        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            Object[] responses = {null};
            asyncMethod.invoke(command, r -> {
                responses[0] = r;
                countDownLatch.countDown();
            });
            countDownLatch.await();
//            if (responses[0] == null) {
//                return ResultUtils.failure(command, command.getClass().getName() + "服务async方法返回null");
//            }
            return ResultUtils.returnData(command, responses[0]);
        } catch (Throwable e) {
            log.error("{}服务处理失败", command, e);
            return ResultUtils.failure(command, command.getClass().getName() + "服务处理失败, " + e.getMessage());
        }
    }

    /**
     * asynchronous run synchronous method with thread pool.
     */
    private <T extends C> void asyncWithBlockMethod(T command, BlockingMethod<T> blockingMethod,
        Consumer<IResult<?>> callback) {

        ExecutorFactory<T> executorFactory = getExecutorFactory(command);
        Executor executor = executorFactory == null ? this.defaultExecutor : executorFactory.create(command);

        CompletableFuture<Object> future = executor != null ?
            CompletableFuture.supplyAsync(() -> blockingMethod.invoke(command), executor)  // 指定执行器执行
            : CompletableFuture.supplyAsync(() -> blockingMethod.invoke(command));  // 公共默认执行器执行

        future.whenComplete(((response, throwable) -> doResponse(command, response, throwable, callback)));
    }

    private void doResponse(ICommand command, Object value, Throwable throwable, Consumer<IResult<?>> callback) {
        IResult<?> response = null;
        if (throwable != null) {
            StringWriter writer = null;
            PrintWriter stream = null;
            try {
                writer = new StringWriter();
                stream = new PrintWriter(writer);
                throwable.printStackTrace(stream);
                response = ResultUtils.failure(command, writer.toString());
            } finally {
                close(writer, stream);
            }
        } else {
            response = ResultUtils.returnData(command, value);
        }

        callback.accept(response);
    }

    private void close(AutoCloseable... closeables) {
        for (AutoCloseable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    log.error("closeable[{}] object close error", closeable, e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends C> AsyncMethod<T> getAsyncMethod(T command) {
        return (AsyncMethod<T>) asyncMethodMap.get(command.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T extends C> BlockingMethod<T> getBlockingMethod(T command) {
        return (BlockingMethod<T>) blockingMethodMap.get(command.getClass());
    }

    @SuppressWarnings("unchecked")
    private <T extends C> ExecutorFactory<T> getExecutorFactory(T command) {
        return (ExecutorFactory<T>) requestExecutors.get(command.getClass());
    }

}
