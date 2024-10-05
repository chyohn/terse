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

import io.github.chyohn.terse.command.AsyncMethod;
import io.github.chyohn.terse.command.BlockingMethod;
import io.github.chyohn.terse.command.ExecutorFactory;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiver;
import io.github.chyohn.terse.command.IReceiverBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class DefaultReceiverBuilder<C extends ICommand> implements IReceiverBuilder<C> {

    private final Map<Class<? extends C>, BlockingMethod<? extends C>> blockingMethodMap = new HashMap<>();
    private final Map<Class<? extends C>, AsyncMethod<? extends C>> asyncMethodMap = new HashMap<>();
    private Executor defaultExecutor;
    private final Map<Class<? extends C>, ExecutorFactory<? extends C>> requestExecutors = new HashMap<>();

    @Override
    public <T extends C>  IReceiverBuilder<C> onReceive(Class<T> t, BlockingMethod<T> handler) {
        blockingMethodMap.put(t, handler);
        return this;
    }

    @Override
    public <T extends C>  IReceiverBuilder<C> onReceive(Class<T> t, BlockingMethod<T> handler,
        ExecutorFactory<T> executorFactory) {
        onReceive(t, handler);
        executor(t, executorFactory);
        return this;
    }

    @Override
    public <T extends C>  IReceiverBuilder<C> onReceive(Class<T> t, AsyncMethod<T> handler) {
        asyncMethodMap.put(t, handler);
        return this;
    }

    @Override
    public <T extends C> DefaultReceiverBuilder<C> defaultExecutor(Executor executor) {
        this.defaultExecutor = executor;
        return this;
    }

    @Override
    public <T extends C>  IReceiverBuilder<C> executor(Class<T> t, ExecutorFactory<T> executorFactory) {
        this.requestExecutors.put(t, executorFactory);
        return this;
    }


    public IReceiver<C> build() {
        return new Receiver<C>(blockingMethodMap, asyncMethodMap, defaultExecutor, requestExecutors);
    }

}
