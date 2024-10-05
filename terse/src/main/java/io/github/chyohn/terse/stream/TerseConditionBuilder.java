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
import io.github.chyohn.terse.flow.ITask;
import io.github.chyohn.terse.utils.ObjectUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * the builder that can build an conditional task
 *
 * @param <C> the context of flow
 * @author qiang.shao
 * @since 1.0.0
 */
public class TerseConditionBuilder<C extends IFlowContext> {

    public static <C extends IFlowContext> TerseConditionBuilder<C> of() {
        return new TerseConditionBuilder<>();
    }

    private IConditionTask<C> instance;
    private Function<C, Boolean> conditionHandler;
    private Set<TerseFlowBuilder<C>> mustRelyOns;
    private Set<TerseFlowBuilder<C>> randomRelyOns;
    private Set<TerseConditionBuilder<C>> mustConditions;
    private Set<TerseConditionBuilder<C>> randomConditions;

    public IConditionTask<C> build() {
        if (conditionHandler == null) {
            return null;
        }

        if (instance != null) {
            return instance;
        }

        return instance = new SimpleConditionTask<C>(conditionHandler)
                .setMustRelyTasks(generateTask(mustRelyOns))
                .setRandomRelyTasks(generateTask(randomRelyOns))
                .setMustConditionTasks(generateConditions(mustConditions))
                .setRandomConditionTasks(generateConditions(randomConditions));
    }

    public TerseConditionBuilder<C> condition(Function<C, Boolean> conditionHandler) {
        this.conditionHandler = conditionHandler;
        return this;
    }

    public TerseConditionBuilder<C> mustRelyOn(TerseFlowBuilder<C> flowBuilder) {
        if (mustRelyOns == null) {
            mustRelyOns = new HashSet<>();
        }
        mustRelyOns.add(flowBuilder);
        return this;
    }


    public TerseConditionBuilder<C> randomRelyOn(TerseFlowBuilder<C> flowBuilder) {
        if (randomRelyOns == null) {
            randomRelyOns = new HashSet<>();
        }
        randomRelyOns.add(flowBuilder);
        return this;
    }

    public TerseConditionBuilder<C> mustCondition(TerseConditionBuilder<C> conditionBuilder) {
        if (mustConditions == null) {
            mustConditions = new HashSet<>();
        }
        mustConditions.add(conditionBuilder);
        return this;
    }


    public TerseConditionBuilder<C> randomCondition(TerseConditionBuilder<C> conditionBuilder) {
        if (randomConditions == null) {
            randomConditions = new HashSet<>();
        }
        randomConditions.add(conditionBuilder);
        return this;
    }

    private List<ITask<C>> generateTask(Set<TerseFlowBuilder<C>> flowBuilders) {
        return flowBuilders == null ? Collections.emptyList() :
                flowBuilders.stream()
                        .flatMap(f -> {
                            Set<ITask<C>> tasks = f.buildTasks();
                            if (ObjectUtils.isEmpty(tasks)) {
                                return Stream.empty();
                            }
                            return tasks.stream();
                        }).collect(Collectors.toList());
    }

    private List<IConditionTask<C>> generateConditions(Set<TerseConditionBuilder<C>> conditionBuilders) {
        return conditionBuilders == null ? Collections.emptyList() :
                conditionBuilders.stream()
                        .flatMap(f -> {
                            IConditionTask<C> condition = f.build();
                            if (condition == null) {
                                return Stream.empty();
                            }
                            return Stream.of(condition);
                        }).collect(Collectors.toList());
    }

}
