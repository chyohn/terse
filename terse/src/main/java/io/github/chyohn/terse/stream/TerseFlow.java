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

package io.github.chyohn.terse.stream;

import io.github.chyohn.terse.enums.RunningWay;
import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.IFlowExecutor;
import io.github.chyohn.terse.flow.ISummaryTask;
import io.github.chyohn.terse.spi.ISpiFactory;

/**
 * define a flow
 *
 * @param <C> the context of flow
 * @author qiang.shao
 * @since 1.0.0
 */
public class TerseFlow<C extends IFlowContext> {

    private final ISummaryTask<C> summaryTask;

    public static <C extends IFlowContext> TerseFlowBuilder<C> newBuilder() {
        return TerseFlowBuilder.of();
    }

    TerseFlow(ISummaryTask<C> summaryTask) {
        this.summaryTask = summaryTask;
    }


    /**
     * execute the flow with parallel
     *
     * @param context flow context
     */
    public void execute(C context) {
        IFlowExecutor executor = ISpiFactory.get(IFlowExecutor.class);
        executor.execute(summaryTask, context, RunningWay.PARALLEL);
    }

    /**
     * execute the flow one by one
     *
     * @param context flow context
     */
    public void serialExecute(C context) {
        IFlowExecutor executor = ISpiFactory.get(IFlowExecutor.class);
        executor.execute(summaryTask, context, RunningWay.SERIAL);
    }
}
