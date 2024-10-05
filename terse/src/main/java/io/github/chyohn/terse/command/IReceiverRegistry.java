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
import io.github.chyohn.terse.spi.SPI;

/**
 * the registry of Receivers
 *
 * @author qiang.shao
 * @since 1.0.0
 */
@Internal
@SPI(allowMultiInstance = false)
public interface IReceiverRegistry {

    /**
     * registry the factory that can create the receiver
     *
     * @param receiverFactory receiver factory
     */
    void register(IReceiverFactory<ICommand> receiverFactory);

    /**
     * get the receiver which can handle command synchronously
     *
     * @param request command
     * @return receiver that can execute command synchronously
     */
    IReceiver<ICommand> getSyncReceiver(ICommand request);

    /**
     * get the receiver which can handle command asynchronously
     *
     * @param request command
     * @return receiver than can execute the special command asynchronously
     */
    IReceiver<ICommand> getAsyncReceiver(ICommand request);
}
