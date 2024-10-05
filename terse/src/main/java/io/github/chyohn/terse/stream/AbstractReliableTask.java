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
import io.github.chyohn.terse.flow.IReliableTask;
import io.github.chyohn.terse.flow.ITask;

import java.util.List;

/**
 * @param <C>
 * @author qiang.shao
 * @since 1.0.0
 */
class AbstractReliableTask<T extends AbstractReliableTask<T, C>, C extends IFlowContext> implements IReliableTask<C> {

    private List<ITask<C>> mustRelyTasks;
    private List<ITask<C>> randomRelyTasks;
    private List<IConditionTask<C>> mustConditionTasks;
    private List<IConditionTask<C>> randomConditionTasks;

    public T setMustRelyTasks(List<ITask<C>> mustRelyTasks) {
        this.mustRelyTasks = mustRelyTasks;
        return (T)this;
    }
    
    public T setRandomRelyTasks(List<ITask<C>> randomRelyTasks) {
        this.randomRelyTasks = randomRelyTasks;
        return (T)this;
    }

    public T setMustConditionTasks(List<IConditionTask<C>> mustConditionTasks) {
        this.mustConditionTasks = mustConditionTasks;
        return (T)this;
    }

    public T setRandomConditionTasks(List<IConditionTask<C>> randomConditionTasks) {
        this.randomConditionTasks = randomConditionTasks;
        return (T)this;
    }

    @Override
    public List<ITask<C>> mustRelyOnTasks() {
        return mustRelyTasks;
    }

    @Override
    public List<ITask<C>> randomRelyOnTasks() {
        return randomRelyTasks;
    }

    @Override
    public List<IConditionTask<C>> mustConditions() {
        return mustConditionTasks;
    }

    @Override
    public List<IConditionTask<C>> randomConditions() {
        return randomConditionTasks;
    }
}
