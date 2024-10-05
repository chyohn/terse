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

package io.github.chyohn.terse.flow;


import io.github.chyohn.terse.anotations.External;
import io.github.chyohn.terse.flow.factory.TaskHandlerFactory;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IResult;

import java.util.List;

/**
 * flow task, supply multiple commands
 *
 * @author qiang.shao
 * @since 1.0.0
 * @param <C> type Context
 * @param <R> type of command return
 */
@External
public interface ITaskMultiple<C extends IFlowContext, R> extends ITask<C> {

    /**
     * provide the commands to get command result.
     *
     * @param context flow context
     * @return commands
     */
    List<ICommandX<R>> getCommands(C context);

    /**
     * handle the command result
     *
     * @param context flow context
     * @param result   command result, can be null.
     * @param finished true: all commands executed；false: some command are executing or wait to execute.
     */
    void handleResult(C context, IResult<R> result, boolean finished);

    /**
     * provide the task handler who provide command and result handler
     *
     * @param context flow context
     * @return can return null if the task does nothing.
     */
    default ITaskHandler createTaskHandler(C context) {
        return TaskHandlerFactory.newTaskHandler(() -> getCommands(context),
                (result, finished) -> handleResult(context, result, finished));
    }

}
