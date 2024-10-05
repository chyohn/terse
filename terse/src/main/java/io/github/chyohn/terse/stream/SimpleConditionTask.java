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

import io.github.chyohn.terse.flow.IConditionTask;
import io.github.chyohn.terse.flow.IFlowContext;

import java.util.function.Function;

/**
 * @param <C>
 * @author qiang.shao
 * @since 1.0.0
 */
class SimpleConditionTask<C extends IFlowContext> extends AbstractReliableTask<SimpleConditionTask<C>, C> implements IConditionTask<C> {

    private final Function<C, Boolean> handler;

    public SimpleConditionTask(Function<C, Boolean> handler) {
        this.handler = handler;
    }

    @Override
    public boolean isTrue(C c) {
        return handler.apply(c);
    }
}
