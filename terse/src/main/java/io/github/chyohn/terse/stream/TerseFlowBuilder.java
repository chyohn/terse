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


import io.github.chyohn.terse.Terse;
import io.github.chyohn.terse.flow.*;
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.utils.ObjectUtils;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IResult;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * the builder that can build a flow
 *
 * @param <C> the class type of context
 * @author qiang.shao
 * @since 1.0.0
 */
public final class TerseFlowBuilder<C extends IFlowContext> {
    public static <C extends IFlowContext> TerseFlowBuilder<C> of() {
        return new TerseFlowBuilder<>();
    }

    // the handlers of current flow
    private final Set<Function<C, ITaskHandler>> handlers = new HashSet<>();
    // the tasks of current flow, and the tasks are generated with the  handlers
    private Set<ITask<C>> tasks;

    // current task execute rely on some other task flow
    private Set<TerseFlowBuilder<C>> mustRelyOns;
    private Set<TerseFlowBuilder<C>> randomRelyOns;

    private Set<TerseConditionBuilder<C>> mustConditions;
    private Set<TerseConditionBuilder<C>> randomConditions;

    // merge with other flow
    private Set<TerseFlowBuilder<C>> mergeFlowBuilders;

    /**
     * run the task by thread pool
     *
     * @param poolName the thread pool name，register pool see {@link Terse#registerExecutor(String, Executor)}.
     *                 if poolName=null or not registry, will use {@link CompletableFuture#supplyAsync(Supplier)} execute <code>runnable</code>
     * @param runnable thread task
     * @return current builder
     */
    public TerseFlowBuilder<C> runnable(String poolName, Consumer<C> runnable) {
        addHandle(context -> TaskHandlerFactory.newTaskHandler(poolName, () -> runnable.accept(context)));
        return this;
    }


    /**
     * run the task by thread pool, and return indicate type result
     *
     * @param poolName        the thread pool name，register pool see {@link Terse#registerExecutor(String, Executor)}.
     *                        if poolName=null or not registry, will use {@link CompletableFuture#supplyAsync(Supplier)} execute <code>callable</code>
     * @param callable        thread task with return
     * @param responseHandler response handler
     * @param <R>             the type of task return
     * @return current builder
     */
    public <R> TerseFlowBuilder<C> callable(String poolName, Function<C, R> callable, Callback2<C, R> responseHandler) {
        addHandle(context -> TaskHandlerFactory.newTaskHandler(poolName
                , () -> callable.apply(context)
                , result -> responseHandler.apply(context, result)));
        return this;
    }

    /**
     * handle task by indicate command
     *
     * @param requestsSupplier the supplier to get the request
     * @param responseHandler  the handler to handle the result
     * @param <R>              the request data type
     * @return current builder
     */
    public <R> TerseFlowBuilder<C> runCommand(Function<C, ICommandX<R>> requestsSupplier, Callback2<C, R> responseHandler) {
        addHandle(context -> {
            return TaskHandlerFactory.newTaskHandler(() -> {
                ICommandX<R> commandX = requestsSupplier.apply(context);
                if (commandX == null) {
                    return null;
                }
                return Collections.singletonList(commandX);
            }, (response, finished) -> responseHandler.apply(context, response.getValue()));
        });
        return this;
    }

    /**
     * handle task by indicate command
     *
     * @param requestsSupplier the supplier to get the request
     * @param responseHandler  the handler to handle the result
     * @param <R>              the request data type
     * @return current builder
     */
    public <R> TerseFlowBuilder<C> runCommands(Function<C, List<ICommandX<R>>> requestsSupplier, ResponseHandler<C, R> responseHandler) {
        addHandle(context -> {
            return TaskHandlerFactory.newTaskHandler(() -> requestsSupplier.apply(context)
                    , (response, finished) -> responseHandler.handle(context, response, finished));
        });
        return this;
    }

    /**
     * add the handle task
     *
     * @param handler the task handler
     */
    private void addHandle(Function<C, ITaskHandler> handler) {
        handlers.add(handler);
    }

    /**
     * finish current TerseFlowBuilder and goto handle next TerseFlowBuilder
     *
     * @return new TerseFlowBuilder that rely on current TerseFlowBuilder
     */
    public TerseFlowBuilder<C> then() {
        TerseFlowBuilder<C> currentFlow = TerseFlowBuilder.of();
        currentFlow.mustRelyOn(this);
        return currentFlow;
    }


    /**
     * the current task's execution rely on forward-tasks finished.
     *
     * <p><b>For Example: </b></p>
     * <p><b>Flow A: </b> task1 -&gt; task2 </p>
     * <p><b>Flow B: </b> task3 -&gt; task4 </p>
     * <p><b>Flow C: </b> task5</p>
     * <ol>
     *     <li>
     *         if <b>C.relyOn(A)</b>, the result flow is: task1 -&gt; task2 -&gt; task5
     *     </li>
     *     <li>
     *      <p>if <b>C.relyOn(A).relyOn(B)</b>, the result flow as below: </p>
     *      <pre>
     *          task1 -&gt; task2 \
     *                          |-&gt; task5
     *          task3 -&gt; task4 /
     *      </pre>
     *     </li>
     * </ol>
     *
     * @param forwardFlow another TerseFlowBuilder
     * @return current TerseFlowBuilder
     */
    public TerseFlowBuilder<C> mustRelyOn(TerseFlowBuilder<C> forwardFlow) {
        if (forwardFlow == null) {
            return this;
        }

        if (mustRelyOns == null) {
            mustRelyOns = new HashSet<>();
        }
        mustRelyOns.add(forwardFlow);
        return this;
    }


    public TerseFlowBuilder<C> randomRelyOn(TerseFlowBuilder<C> forwardFlow) {
        if (forwardFlow == null) {
            return this;
        }
        if (randomRelyOns == null) {
            randomRelyOns = new HashSet<>();
        }
        randomRelyOns.add(forwardFlow);
        return this;
    }

    public TerseFlowBuilder<C> mustCondition(TerseConditionBuilder<C> condition) {
        if (condition == null) {
            return this;
        }
        if (mustConditions == null) {
            mustConditions = new HashSet<>();
        }
        mustConditions.add(condition);
        return this;
    }

    public TerseFlowBuilder<C> randomCondition(TerseConditionBuilder<C> condition) {
        if (condition == null) {
            return this;
        }
        if (randomConditions == null) {
            randomConditions = new HashSet<>();
        }
        randomConditions.add(condition);
        return this;
    }


    public TerseFlowBuilder<C> merge(TerseFlowBuilder<C> mergedFlow) {
        if (mergedFlow == null) {
            return this;
        }
        if (mergeFlowBuilders == null) {
            mergeFlowBuilders = new HashSet<>();
        }
        mergeFlowBuilders.add(mergedFlow);
        return this;
    }

    /**
     * generate {@link TerseFlow} object.
     *
     * @return flow
     */
    public TerseFlow<C> build() {
        return build(null);
    }

    /**
     * handle the result, after all tasks finished.
     *
     * @param summary summary handler
     * @return flow object with a SummaryTask
     */
    public TerseFlow<C> build(Consumer<C> summary) {


        boolean noHandler = ObjectUtils.isEmpty(handlers);

        ISummaryTask<C> summaryTask = new SimpleSummary<>(summary)
                .setMustRelyTasks(new ArrayList<>(this.buildTasks()))
                .setRandomRelyTasks(noHandler ? genTasks(randomRelyOns) : null)
                .setMustConditionTasks(noHandler ? genConditionTasks(mustConditions) : null)
                .setRandomConditionTasks(noHandler ? genConditionTasks(randomConditions) : null);

        return new TerseFlow<>(summaryTask);
    }

    private List<IConditionTask<C>> genConditionTasks(Set<TerseConditionBuilder<C>> builders) {
        return builders == null ? new ArrayList<>()
                : builders.stream().flatMap(f -> {
                    IConditionTask<C> conditionTask = f.build();
                    return conditionTask == null ? Stream.empty() : Stream.of(conditionTask);
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private List<ITask<C>> genTasks(Set<TerseFlowBuilder<C>> builders) {
        return builders == null ? new ArrayList<>()
                : builders.stream().flatMap(f -> {
                    Set<ITask<C>> tasks = f.buildTasks();
                    return ObjectUtils.isEmpty(tasks) ? Stream.empty() : tasks.stream();
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private Set<ITask<C>> getMergedTasks() {
        return mergeFlowBuilders == null ? new HashSet<>()
                : mergeFlowBuilders.stream().flatMap(f -> {
            Set<ITask<C>> tasks = f.buildTasks();
            if (ObjectUtils.isEmpty(tasks)) {
                return Stream.empty();
            }
            return tasks.stream();
        }).collect(Collectors.toSet());
    }

    private Set<ITask<C>> concat(Set<ITask<C>> s1, Set<ITask<C>> s2) {
        if (ObjectUtils.isEmpty(s1)) {
            return s2;
        }
        if (ObjectUtils.isEmpty(s2)) {
            return s1;
        }
        Set<ITask<C>> s3 = new HashSet<>(s1);
        s3.addAll(s2);
        return s3;
    }

    Set<ITask<C>> buildTasks() {
        if (tasks != null) {
            return tasks;
        }

        List<ITask<C>> mustRelyOnTasks = genTasks(mustRelyOns);
        if (ObjectUtils.isEmpty(handlers)) {
            tasks = concat(new HashSet<>(mustRelyOnTasks), getMergedTasks());
            return tasks;
        }

        tasks = concat(getMergedTasks(), handlers.stream().map(handler -> {
            return new SimpleTask<C>(handler)
                    .setMustRelyTasks(mustRelyOnTasks)
                    .setRandomRelyTasks(genTasks(randomRelyOns))
                    .setMustConditionTasks(genConditionTasks(mustConditions))
                    .setRandomConditionTasks(genConditionTasks(randomConditions));
        }).collect(Collectors.toSet()));
        return tasks;
    }

    @FunctionalInterface
    public interface ResponseHandler<C extends IFlowContext, R> {
        void handle(C c, IResult<R> result, boolean finished);
    }
}
