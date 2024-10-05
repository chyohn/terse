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

package io.github.chyohn.terse;

import io.github.chyohn.terse.cluster.IClusterClient;
import io.github.chyohn.terse.command.*;
import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IFlowExecutor;
import io.github.chyohn.terse.flow.ISummaryTask;
import io.github.chyohn.terse.command.threadpool.ThreadPoolReceiverFactory;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.stream.TerseConditionBuilder;
import io.github.chyohn.terse.stream.TerseFlow;
import io.github.chyohn.terse.stream.TerseFlowBuilder;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Terse facade
 */
public abstract class Terse {

    private Terse() {
    }

    /**
     * init cluster and set configs before start cluster
     *
     * @param config the config of cluster
     */
    public static void initCluster(Map<String, Object> config) {
        IClusterClient clusterClient = ISpiFactory.get(IClusterClient.class, false);
        if (clusterClient == null || clusterClient.isInit()) {
            return;
        }
        clusterClient.onInit(config);
    }

    /**
     * start cluster after all service ready.
     */
    public static synchronized void readyCluster() {
        IClusterClient clusterClient = ISpiFactory.get(IClusterClient.class, false);
        if (clusterClient == null || clusterClient.isReady()) {
            return;
        }
        clusterClient.onReady();
    }

    /**
     * build a flow
     *
     * @param <C> flow context type
     * @return flow builder
     */
    public static <C extends IFlowContext> TerseFlowBuilder<C> flow() {
        return TerseFlow.newBuilder();
    }


    /**
     * build an conditional task
     *
     * @param handler the condition handler
     * @param <C>     type of context
     * @return builder of conditional task
     */
    public static <C extends IFlowContext> TerseConditionBuilder<C> condition(Function<C, Boolean> handler) {
        return TerseConditionBuilder.<C>of().condition(handler);
    }

    /**
     * register pool to execute some task
     *
     * @param poolName pool name
     * @param executor thread pool
     */
    public static void registerExecutor(String poolName, Executor executor) {
        ThreadPoolReceiverFactory.register(poolName, executor);
    }

    /**
     * register receiver who can execute indicate command type
     *
     * @param builderConsumer consume and config the command receiver handler
     */
    public static void withReceiverBuilder(Consumer<IReceiverBuilder<ICommand>> builderConsumer) {
        IReceiverRegistry receiverManager = ISpiFactory.get(IReceiverRegistry.class);
        receiverManager.register(builderConsumer::accept);
    }

    @SuppressWarnings("unchecked")
    public static void registerReceiverFactory(IReceiverFactory<?> factory) {
        IReceiverRegistry receiverManager = ISpiFactory.get(IReceiverRegistry.class);
        receiverManager.register((IReceiverFactory<ICommand>) factory);
    }

    /**
     * execute the indicate context with {@link ISummaryTask} object
     *
     * @param summaryTask summary task
     * @param context flow context
     * @param <C>     type of flow context
     */
    public static <C extends IFlowContext> void execute(ISummaryTask<C> summaryTask, C context) {
        IFlowExecutor executor = ISpiFactory.get(IFlowExecutor.class);
        executor.execute(summaryTask, context, RunningWay.PARALLEL);
    }


    /**
     * @return batch invoker that can submit the commands to receiver
     */
    public static ICommandInvoker commandInvoker() {
        return ISpiFactory.get(ICommandInvoker.class);
    }
}
