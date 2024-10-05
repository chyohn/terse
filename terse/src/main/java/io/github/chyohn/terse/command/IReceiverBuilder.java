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


import io.github.chyohn.terse.anotations.Internal;

import java.util.concurrent.Executor;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
public interface IReceiverBuilder<C extends ICommand> {


    <T extends C> IReceiverBuilder<C> onReceive(Class<T> t, BlockingMethod<T> handler);

    <T extends C>  IReceiverBuilder<C> onReceive(Class<T> t, BlockingMethod<T> handler, ExecutorFactory<T> executorFactory);

    <T extends C> IReceiverBuilder<C> onReceive(Class<T> t, AsyncMethod<T> handler);

    <T extends C> IReceiverBuilder<C> defaultExecutor(Executor executor);

    <T extends C> IReceiverBuilder<C> executor(Class<T> t, ExecutorFactory<T> executorFactory);

    IReceiver<C> build();
}
