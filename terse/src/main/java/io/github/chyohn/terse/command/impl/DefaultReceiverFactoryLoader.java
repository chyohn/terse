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

import io.github.chyohn.terse.command.ICommand;
import io.github.chyohn.terse.command.IReceiverFactory;
import io.github.chyohn.terse.command.IReceiverFactoryLoader;
import io.github.chyohn.terse.spi.ISpiFactory;
import io.github.chyohn.terse.utils.ObjectUtils;
import java.util.ArrayList;
import java.util.List;
import io.github.chyohn.terse.utils.ResolvableType;

/**
 * @author qiang.shao
 * @since 1.0.0
 */
class DefaultReceiverFactoryLoader implements IReceiverFactoryLoader {

    @Override
    @SuppressWarnings("unchecked")
    public List<IReceiverFactory<ICommand>> load(ICommand command) {

        List<IReceiverFactory> receiverFactories = ISpiFactory.getAll(IReceiverFactory.class);
        if (ObjectUtils.isEmpty(receiverFactories)) {
            return null;
        }

        List<IReceiverFactory<ICommand>> factories = new ArrayList<>();
        for (IReceiverFactory<ICommand> factory : receiverFactories) {
            Class<?> type = factory.getClass();
            if (ResolvableType.hasGeneric(type, command.getClass())) {
                factories.add(factory);
            }
        }

        return factories;
    }

}
