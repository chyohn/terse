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

import java.util.ArrayList;

/**
 * support for the case: the task rely on other task with some condition. it's same to say, dynamic add condition for second task.
 *
 * so, this class object manage all dynamic conditions for the second task. all the conditions are ‘or’ relationship with each others.
 *
 * @param <C>
 * @author qiang.shao
 * @since 1.0.0
 */
class TaskRelyWithConditionTask<C extends IFlowContext> extends AbstractReliableTask<TaskRelyWithConditionTask<C>, C> implements IConditionTask<C> {

    TaskRelyWithConditionTask() {
        setRandomConditionTasks(new ArrayList<>());
    }

    void addRelyCondition(IConditionTask<C> condition) {
        if (!randomConditions().contains(condition)) {
            randomConditions().add(condition);
        }
    }

    @Override
    public boolean isTrue(C c) {
        return true;
    }
}