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

package io.github.chyohn.terse.command.threadpool;

import io.github.chyohn.terse.anotations.Internal;
import io.github.chyohn.terse.utils.ObjectUtils;
import io.github.chyohn.terse.command.IReceiverBuilder;
import io.github.chyohn.terse.command.IReceiverFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;


/**
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
public class ThreadPoolReceiverFactory implements IReceiverFactory<ThreadCommand> {

    private static final Map<String, Executor> executors = new HashMap<>();

    public static synchronized void register(String poolName, Executor executor) {
        if (executors.containsKey(poolName)) {
            throw new IllegalArgumentException(poolName + "has exists, not allowed the duplicate define.");
        }
        executors.put(poolName, executor);
    }

    @Override
    public void buildReceiver(IReceiverBuilder<ThreadCommand> builder) {
        builder.onReceive(RunnableCommand.class, command -> {
                    command.runnable.run();
                    return true;
                }, this::getExecutor)
                .onReceive(CallableCommand.class, (command) -> {
                    return command.callable.call();
                }, this::getExecutor);
    }

    private Executor getExecutor(ThreadCommand command) {
        if (ObjectUtils.isBlank(command.poolName)) {
            return null;
        }
        return executors.get(command.poolName);
    }

}
