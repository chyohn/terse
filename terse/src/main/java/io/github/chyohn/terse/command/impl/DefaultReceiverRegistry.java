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

package io.github.chyohn.terse.command.impl;

import io.github.chyohn.terse.exception.CommandExecuteException;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.utils.ObjectUtils;
import io.github.chyohn.terse.command.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class DefaultReceiverRegistry implements IReceiverRegistry {

    private final Set<IReceiver<ICommand>> receivers = new HashSet<>();
    private final Set<Class<? extends IReceiverFactory>> receiverFactoryClasses = new HashSet<>();
    private final Set<IReceiverFactory<ICommand>> receiverFactories = new HashSet<>();

    @Override
    public void register(IReceiverFactory<ICommand> receiverFactory) {
        if (!receiverFactoryClasses.add(receiverFactory.getClass())) {
            return;
        }
        IReceiverBuilder<ICommand> builder = new DefaultReceiverBuilder<>();
        receiverFactory.buildReceiver(builder);
        IReceiver<ICommand> receiver = builder.build();
        receivers.add(receiver);
    }

    @Override
    public IReceiver<ICommand> getSyncReceiver(ICommand command) {
        for (IReceiver<ICommand> receiver : receivers) {
            if (receiver.canSync(command)) {
                return receiver;
            }
        }

        for (IReceiver<ICommand> receiver : receivers) {
            if (receiver.canAsync(command)) {
                return receiver;
            }
        }

        if (register(command)) {
            return getSyncReceiver(command);
        }

        throw new CommandExecuteException("no receiver can execute this command: " + command.getClass().getName());
    }

    @Override
    public IReceiver<ICommand> getAsyncReceiver(ICommand command) {

        for (IReceiver<ICommand> receiver : receivers) {
            if (receiver.canAsync(command)) {
                return receiver;
            }
        }

        for (IReceiver<ICommand> receiver : receivers) {
            if (receiver.canSync(command)) {
                return receiver;
            }
        }

        if (register(command)) {
            return getAsyncReceiver(command);
        }

        throw new CommandExecuteException(
                "no receiver can async execute this command: " + command.getClass().getName());
    }

    /**
     * load and registry the receiver which can receive the command.
     *
     * @param command command
     * @return true if has receiver to handle the command
     */
    private boolean register(ICommand command) {

        List<IReceiverFactoryLoader> loaders = ISpiFactory.getAll(IReceiverFactoryLoader.class);
        if (ObjectUtils.isEmpty(loaders)) {
            return false;
        }

        boolean loaded = false;
        for (IReceiverFactoryLoader loader : loaders) {
            List<IReceiverFactory<ICommand>> factories = loader.load(command);
            if (ObjectUtils.isEmpty(factories)) {
                continue;
            }
            for (IReceiverFactory<ICommand> factory : factories) {
                register(factory);
            }
            loaded = true;
        }

        return loaded;
    }
}
