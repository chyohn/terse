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

package io.github.chyohn.terse.command;

import io.github.chyohn.terse.anotations.Internal;

import java.util.function.Consumer;

/**
 * define the receiver of command
 *
 * @param <C> class type of command
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
public interface IReceiver<C extends ICommand> {

    /**
     * If can handle command synchronously
     *
     * @param command command
     * @param <T>     class type of command
     * @return true if has sync handler to handle command
     */
    <T extends C> boolean canSync(T command);

    /**
     * If can handle command asynchronously
     *
     * @param command command
     * @param <T>     class type of command
     * @return true if has async handler to handle command
     */
    <T extends C> boolean canAsync(T command);

    /**
     * handle command synchronously
     *
     * @param command command
     * @param <T>     class type of command
     * @return result of command executed
     */
    <T extends C> IResult<?> sync(T command);

    /**
     * handle command asynchronously
     *
     * @param command  command
     * @param callback result handler
     * @param <T>      class type of command
     */
    <T extends C> void async(T command, Consumer<IResult<?>> callback);
}
