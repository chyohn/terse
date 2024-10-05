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

import io.github.chyohn.terse.flow.IFlowContext;
import io.github.chyohn.terse.flow.ISummaryTask;

import java.util.function.Consumer;

/**
 * @param <C>
 * @author qiang.shao
 * @since 1.0.0
 */
class SimpleSummary<C extends IFlowContext> extends AbstractReliableTask<SimpleSummary<C>, C> implements ISummaryTask<C> {
    private final Consumer<C> handler;

    public SimpleSummary(Consumer<C> handler) {
        this.handler = handler;
    }

    @Override
    public void summary(C c) {
        if(handler != null) {
            handler.accept(c);
        }
    }
}
