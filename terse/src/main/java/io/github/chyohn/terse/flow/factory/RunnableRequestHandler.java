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

package io.github.chyohn.terse.flow.factory;

import io.github.chyohn.terse.flow.ITaskHandler;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IResult;
import io.github.chyohn.terse.command.threadpool.RunnableCommand;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
class RunnableRequestHandler  implements ITaskHandler {
    String poolName;
    List<Runnable> runnableList;

    RunnableRequestHandler(String poolName, List<Runnable> runnableList) {
        this.poolName = poolName;
        this.runnableList = runnableList;
    }

    @Override
    public List<ICommand> getCommand() {
        return runnableList.stream().map(runnable -> new RunnableCommand(poolName, runnable)).collect(Collectors.toList());
    }

    @Override
    public void handleResult(IResult<?> result, boolean finished) {
        // nothing to do
    }
}
