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
import io.github.chyohn.terse.function.Callback2;
import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.ICommandX;
import io.github.chyohn.terse.command.IResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author qiang.shao
 * @since 1.0.0
 */
class SimpleTaskHandler<R> implements ITaskHandler {
    Supplier<List<ICommandX<R>>> commandSupplier;
    Callback2<IResult<R>, Boolean> resultHandler;

    SimpleTaskHandler(Supplier<List<ICommandX<R>>> commandSupplier, Callback2<IResult<R>, Boolean> resultHandler) {
        this.commandSupplier = commandSupplier;
        this.resultHandler = resultHandler;
    }

    @Override
    public List<ICommand> getCommand() {
        return new ArrayList<>(commandSupplier.get());
    }

    @Override
    public void handleResult(IResult<?> result, boolean finished) {
        if (result != null && result.getValue() != null) {
            resultHandler.apply((IResult<R>) result, finished);
        }
    }
}
