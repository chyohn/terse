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

import java.util.List;

import io.github.chyohn.terse.anotations.External;
import io.github.chyohn.terse.spi.SPI;

/**
 * the loader that load the {@link IReceiverFactory}
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@External
@SPI(allowMultiInstance = true)
public interface IReceiverFactoryLoader {

    /**
     * load the receiver factories by command
     *
     * @param command command
     * @return the list of {@link IReceiverFactory}
     */
    List<IReceiverFactory<ICommand>> load(ICommand command);
}
