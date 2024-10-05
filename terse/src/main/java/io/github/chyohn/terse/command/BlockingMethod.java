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

import io.github.chyohn.terse.exception.CommandExecuteException;

/**
 * the method that execute blocking command
 *
 * @param <T> the command type
 * @author qiang.shao
 * @since 1.0.0
 */
@FunctionalInterface
public interface BlockingMethod<T extends ICommand> {

    /**
     * invoke the command and wrap the exception throw when run
     *
     * @param command command
     * @return execute result
     */
    default Object invoke(T command) {
        try {
            return run(command);
        } catch (Exception e) {
            throw new CommandExecuteException(e);
        }
    }

    /**
     * invoke command
     *
     * @param command command
     * @return execute result
     * @throws Exception some exception when run command
     */
    Object run(T command) throws Exception;
}
