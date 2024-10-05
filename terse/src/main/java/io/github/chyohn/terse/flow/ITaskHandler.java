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
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;

import java.util.List;

/**
 * support provide {@link ICommand}，handle the {@link IResult}
 *
 * <p>note: the method {@link #getCommand()} and {@link #handleResult(IResult, boolean)} are executed in same thread with invoker.</p>
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@External
public interface ITaskHandler {

    /**
     * provide the commands to get command result.
     *
     * @return the command to submit
     */
    List<ICommand> getCommand();

    /**
     * handle the command result
     *
     * @param result   command result, can be null.
     * @param finished true: all commands executed；false: some command are executing or wait to execute.
     */
    void handleResult(IResult<?> result, boolean finished);


}
